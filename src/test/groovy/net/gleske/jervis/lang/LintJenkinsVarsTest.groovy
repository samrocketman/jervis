/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */
package net.gleske.jervis.lang

// classes necessary for Groovy parse syntax check
import groovy.io.GroovyPrintWriter
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.Janitor
import org.codehaus.groovy.control.SourceUnit
import org.junit.Test

class LintJenkinsVarsTest extends GroovyTestCase {
    private static final GROOVY_FILES = '**/*.groovy'
    private static final RULESET_FILES = [
        'rulesets/basic.xml',
        'rulesets/braces.xml',
        'rulesets/groovyism.xml',
        'rulesets/imports.xml'
    ].join(',')
    @Test
    void test_vars_RunCodeNarc() {
        def ant = new AntBuilder()
        ant.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')
        ant.codenarc(ruleSetFiles: RULESET_FILES, maxPriority1Violations: 0, maxPriority2Violations: 1, maxPriority3Violations: 0) {
           fileset(dir: 'vars') {
               include(name:GROOVY_FILES)
           }
           report(type: 'html') {
               option(name: 'outputFile', value: 'build/reports/codenarc-vars.html')
               option(name: 'title', value: 'Pipeline vars directory CodeNarc report')
           }
        }
    }

    @Test
    void test_vars_SyntaxCheck() {
        List errors = []
        new File('vars').listFiles().findAll { File f ->
            f.name.endsWith('.groovy')
        }.each { File f ->
            f.withReader('UTF-8') { Reader r ->
                ErrorCollector errorCollector = new ErrorCollector()
                CompilerConfiguration config = new CompilerConfiguration()
                SourceUnit source = new SourceUnit(f, config, new GroovyClassLoader(), errorCollector)
                try {
                    source.parse()
                } catch(Exception e) {
                    StringWriter out    = new StringWriter()
                    PrintWriter writer = new GroovyPrintWriter(out, true)
                    errorCollector.errors.each {
                        it.write(writer, new Janitor())
                    }
                    errors << out.toString()
                }
            }
        }
        String syntax_errors = errors.join('\n')
        assert syntax_errors == ''
    }
}
