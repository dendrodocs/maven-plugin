param(
    [string]$ReportRoot = 'target',
    [string]$SummaryPath = $env:GITHUB_STEP_SUMMARY
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($SummaryPath)) { throw 'Supply -SummaryPath or GITHUB_STEP_SUMMARY.' }

function Read-ReportXml([string]$Path) {
    $settings = [Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $reader = [Xml.XmlReader]::Create($Path, $settings)
    try {
        $document = [Xml.XmlDocument]::new()
        $document.XmlResolver = $null
        $document.Load($reader)
        return ,$document
    } finally { $reader.Dispose() }
}

$lines = [Collections.Generic.List[string]]::new()
$lines.Add('## Verification evidence')
$lines.Add('')
$lines.Add('| Suite | Executed | Failures | Errors | Skipped |')
$lines.Add('| --- | ---: | ---: | ---: | ---: |')

$unitReports = @(Get-ChildItem (Join-Path $ReportRoot 'surefire-reports/TEST-*.xml') -ErrorAction SilentlyContinue)
if ($unitReports.Count -gt 0) {
    $tests = 0; $failures = 0; $errors = 0; $skipped = 0
    foreach ($file in $unitReports) {
        $suite = (Read-ReportXml $file.FullName).DocumentElement
        $tests += [int]$suite.GetAttribute('tests')
        $failures += [int]$suite.GetAttribute('failures')
        $errors += [int]$suite.GetAttribute('errors')
        $skipped += [int]$suite.GetAttribute('skipped')
    }
    $lines.Add("| Unit tests | $($tests - $skipped) | $failures | $errors | $skipped |")
} else { $lines.Add('| Unit tests | Report unavailable | - | - | - |') }

$invokerReports = @(Get-ChildItem (Join-Path $ReportRoot 'invoker-reports/BUILD-*.xml') -ErrorAction SilentlyContinue)
if ($invokerReports.Count -gt 0) {
    $passed = 0; $failed = 0; $skipped = 0
    foreach ($file in $invokerReports) {
        $result = (Read-ReportXml $file.FullName).DocumentElement.GetAttribute('result')
        switch ($result) {
            'success' { $passed++ }
            'skipped' { $skipped++ }
            default { $failed++ }
        }
    }
    $lines.Add("| Maven Invoker fixtures | $($passed + $failed) | $failed | - | $skipped |")
    $lines.Add('')
    $lines.Add('Invoker success includes fixtures whose expected result is a failed consumer build.')
} else { $lines.Add('| Maven Invoker fixtures | Report unavailable | - | - | - |') }

$lines.Add('')
$coveragePath = Join-Path $ReportRoot 'site/jacoco/jacoco.xml'
if (Test-Path -LiteralPath $coveragePath) {
    $report = (Read-ReportXml $coveragePath).DocumentElement
    $lines.Add('| Unit-test coverage | Covered | Missed | Percent |')
    $lines.Add('| --- | ---: | ---: | ---: |')
    foreach ($kind in @('INSTRUCTION', 'BRANCH', 'LINE', 'METHOD', 'CLASS')) {
        $counter = $report.SelectSingleNode("counter[@type='$kind']")
        if ($null -ne $counter) {
            $covered = [long]$counter.GetAttribute('covered')
            $missed = [long]$counter.GetAttribute('missed')
            $percent = if ($covered + $missed -eq 0) { 'n/a' } else {
                (($covered * 100.0 / ($covered + $missed)).ToString('F1', [Globalization.CultureInfo]::InvariantCulture)) + '%'
            }
            $lines.Add("| $kind | $covered | $missed | $percent |")
        }
    }
    $lines.Add('')
    $lines.Add('Coverage includes the adapter unit-test JVM; generated help code, Invoker builds, and child analyzers are excluded.')
} else { $lines.Add('Unit-test coverage report unavailable.') }

$lines.Add('')
$auditPath = Join-Path $ReportRoot 'security/analyzer-dependency-audit.json'
if (Test-Path -LiteralPath $auditPath) {
    $audit = Get-Content -LiteralPath $auditPath -Raw | ConvertFrom-Json
    $lines.Add("Analyzer audit: $(@($audit.embeddedComponents).Count) embedded components assessed; $(@($audit.findings).Count) components matched advisories.")
    $lines.Add('Embedded Maven metadata only; minimized-JAR reachability is not assessed. Advisory matches block Central staging.')
} else { $lines.Add('Analyzer advisory report unavailable; no security conclusion is inferred.') }
$lines.Add('')
$lines.Add('Download the verification artifact for XML reports, coverage HTML, audit details, and integration build logs.')
$lines.Add('A summary reports available evidence; the build and audit steps determine job success.')
[IO.File]::AppendAllText($SummaryPath, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
