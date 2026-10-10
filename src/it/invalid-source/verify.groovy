assert !new File(basedir, 'target/dendrodocs.json').exists()
assert new File(basedir, 'build.log').text.contains('DendroDocs generation failed')
assert !new File(basedir, 'target').listFiles().any { it.name.startsWith('.dendrodocs-') }
return true
