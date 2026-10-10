for (mode in ['xml', 'property']) {
    def module = new File(basedir, 'consumer-' + mode)
    def output = new File(module, 'target/dendrodocs.json')
    assert output.isFile()
    assert new groovy.json.JsonSlurper().parse(output)*.FullName == ['sample.KotlinExample']
    assert !new File(module, 'target').listFiles().any { it.name.startsWith('.dendrodocs-') }
}
def log = new File(basedir, 'build.log').text
assert log.count('Using DendroDocs tool test:fixture-analyzer:jar:1; Java preflight: false') == 2
return true
