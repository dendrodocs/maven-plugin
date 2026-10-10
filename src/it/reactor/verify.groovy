assert !new File(basedir, 'target/dendrodocs.json').exists()
def library = new groovy.json.JsonSlurper().parse(new File(basedir, 'library/target/dendrodocs.json'))
def consumer = new groovy.json.JsonSlurper().parse(new File(basedir, 'consumer/target/dendrodocs.json'))
assert library*.FullName == ['library.Greeting']
assert consumer*.FullName == ['consumer.Consumer']
assert consumer[0].Fields[0].Type == 'library.Greeting'
return true
