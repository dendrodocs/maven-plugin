def output = new File(basedir, 'target/dendrodocs.json')
assert output.isFile()
def types = new groovy.json.JsonSlurper().parse(output)
assert types*.FullName.sort() == ['sample.Greeting', 'sample.GreetingRecord']
assert types.find { it.FullName == 'sample.Greeting' }.Fields[0].Type == 'java.lang.String'
assert types.find { it.FullName == 'sample.GreetingRecord' }.Type == 2
assert !new File(basedir, 'target').listFiles().any { it.name.startsWith('.dendrodocs-') }
return true
