/**
  * I got some hints for how to do this at the following website.
  * https://dzone.com/articles/groovy-goodness-customizing-the-groovy-compiler
  */
import org.codehaus.groovy.control.customizers.ImportCustomizer

if(Integer.parseInt(GroovySystem.version.tokenize('.').first()) >= 4) {
    // Groovy 4.0 and higher add missing imports for test compilation.
    def imports = new ImportCustomizer()
    imports.addImport('AntBuilder', 'groovy.ant.AntBuilder')
    imports.addImport('GroovyAssert', 'groovy.test.GroovyAssert')
    imports.addImport('GroovyTestCase', 'groovy.test.GroovyTestCase')
    configuration.addCompilationCustomizers(imports)
}
