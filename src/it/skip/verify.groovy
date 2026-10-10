assert !new File(basedir, 'target/dendrodocs.json').exists()
assert new File(basedir, 'build.log').text.contains('Skipping DendroDocs')
return true
