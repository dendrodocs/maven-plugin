[CmdletBinding()]
param(
    [string]$ArtifactPath,
    [string]$ToolArtifact,
    [string]$OutputPath = 'target/security/analyzer-audit.json',
    [switch]$FailOnFindings
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
[xml]$pom = Get-Content (Join-Path $repoRoot 'pom.xml') -Raw
if (-not $ToolArtifact) { $ToolArtifact = [string]$pom.project.properties.'dendrodocs.tool.artifact' }
$parts = $ToolArtifact.Split(':')
if ($parts.Count -lt 3 -or $parts.Count -gt 5 -or @($parts | Where-Object { $_ -notmatch '^[A-Za-z0-9_.+\-]+$' }).Count -gt 0) {
    throw 'ToolArtifact must be groupId:artifactId[:jar[:classifier]]:version.'
}
$version = $parts[-1]
if (($parts.Count -gt 3 -and $parts[2] -cne 'jar') -or $version -in @('LATEST', 'RELEASE')) {
    throw 'ToolArtifact requires a JAR and a concrete version.'
}
if (-not $ArtifactPath) {
    $groupPath = $parts[0].Replace('.', '/')
    $artifactId = $parts[1]
    $classifier = if ($parts.Count -eq 5) { '-' + $parts[3] } else { '' }
    $ArtifactPath = Join-Path $repoRoot "target/it-repository/$groupPath/$artifactId/$version/$artifactId-$version$classifier.jar"
}
if (-not (Test-Path -LiteralPath $ArtifactPath -PathType Leaf)) {
    throw "Analyzer artifact missing: $ArtifactPath. Run Maven verify first."
}

$components = @()
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ArtifactPath).Path)
try {
    foreach ($entry in $archive.Entries | Where-Object FullName -Like 'META-INF/maven/*/pom.properties') {
        $reader = [IO.StreamReader]::new($entry.Open())
        try {
            $component = @{}
            foreach ($line in ($reader.ReadToEnd() -split "`n")) {
                if ($line -match '^(groupId|artifactId|version)=(.*)$') {
                    $component[$Matches[1]] = $Matches[2].Trim()
                }
            }
            if ($component.ContainsKey('groupId') -and $component.ContainsKey('artifactId') -and $component.ContainsKey('version')) {
                $components += [pscustomobject]$component
            }
        } finally {
            $reader.Dispose()
        }
    }
} finally {
    $archive.Dispose()
}
if ($components.Count -eq 0) { throw 'No embedded dependency metadata found; cannot assess analyzer.' }

$queries = @($components | ForEach-Object {
    @{package = @{ecosystem = 'Maven'; name = ($_.groupId + ':' + $_.artifactId)}; version = $_.version}
})
$response = Invoke-RestMethod 'https://api.osv.dev/v1/querybatch' -Method Post -ContentType 'application/json' -Body (@{queries = $queries} | ConvertTo-Json -Depth 6)
if (@($response.results).Count -ne $components.Count) { throw 'OSV response did not cover every component.' }
$findings = @()
for ($index = 0; $index -lt $components.Count; $index++) {
    $result = $response.results[$index]
    $vulnerabilityProperty = $result.PSObject.Properties['vulns']
    if ($null -ne $vulnerabilityProperty -and @($vulnerabilityProperty.Value).Count -gt 0) {
        $component = $components[$index]
        $findings += [pscustomobject]@{
            component = ($component.groupId + ':' + $component.artifactId)
            version = $component.version
            advisoryIds = @($vulnerabilityProperty.Value.id)
        }
    }
}
$absoluteOutput = if ([IO.Path]::IsPathRooted($OutputPath)) { $OutputPath } else { Join-Path $repoRoot $OutputPath }
New-Item -ItemType Directory -Force -Path (Split-Path $absoluteOutput -Parent) | Out-Null
[pscustomobject]@{
    scannedAt = [DateTime]::UtcNow.ToString('o')
    source = 'https://api.osv.dev/v1/querybatch'
    artifact = $ToolArtifact
    artifactSha256 = (Get-FileHash -LiteralPath $ArtifactPath -Algorithm SHA256).Hash.ToLowerInvariant()
    embeddedComponents = $components
    findings = $findings
    scope = 'Embedded Maven metadata only; minimized JAR reachability not assessed'
} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $absoluteOutput -Encoding utf8

Write-Output "Assessed $($components.Count) embedded components; $($findings.Count) components matched advisories."
foreach ($finding in $findings) {
    Write-Warning "$($finding.component):$($finding.version): $($finding.advisoryIds -join ', ')"
}
if ($FailOnFindings -and $findings.Count -gt 0) {
    throw "Production release blocked by analyzer advisory matches. See $absoluteOutput"
}
