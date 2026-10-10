def output = new File(basedir, 'custom output/result.json')
assert output.isFile()
assert !output.text.contains('\n')
def types = new groovy.json.JsonSlurper().parse(output)
assert types*.FullName.sort() == ['sample.Extra', 'sample.Primary']
assert types.find { it.FullName == 'sample.Extra' }.Fields[0].Type == 'sample.Primary'
assert !new File(basedir, 'custom output').listFiles().any { it.name.startsWith('.dendrodocs-') }
return true
