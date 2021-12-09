package net.gleske.jervis.lang
import org.junit.Test
class varsLinterTest extends GroovyTestCase {
    private static final GROOVY_FILES = '**/*.groovy'
    private static final RULESET_FILES = [
        'rulesets/basic.xml',
        'rulesets/braces.xml',
        'rulesets/groovyism.xml',
        'rulesets/imports.xml'
    ].join(',')
    @Test
    void testRunCodeNarc() {
        def ant = new AntBuilder()
        ant.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')
        ant.codenarc(ruleSetFiles: RULESET_FILES, maxPriority1Violations: 0, maxPriority2Violations: 0, maxPriority3Violations: 0) {
           fileset(dir: 'vars') {
               include(name:GROOVY_FILES)
           }
           report(type: 'html') {
               option(name: 'outputFile', value: 'build/reports/codenarc-vars.html')
               option(name: 'title', value: 'Pipeline vars directory CodeNarc report')
           }
        }
    }
}
