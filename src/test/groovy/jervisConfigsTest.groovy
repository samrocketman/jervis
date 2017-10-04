/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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
//lifecycleGenerator, lifecycleValidator, toolchainValidator, and
//platformValidator don't have to be imported because they're in the same
//package.

import net.gleske.jervis.exceptions.JervisException
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
  Tests user supplied configurations from the resources directory in the root
  of this repository.
  */
class jervisConfigsTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new lifecycleGenerator()
    }
    //tear down after every test
    @After protected void tearDown() {
        generator = null
        super.tearDown()
    }
    @Test public void test_lifecycleGenerator_main_toolchains_bash_syntax_check() {
        URL url = this.getClass().getResource('/lifecycles-ubuntu1604-stable.json');
        generator.loadLifecyclesString(url.content.text)
        url = this.getClass().getResource('/toolchains-ubuntu1604-stable.json');
        generator.loadToolchainsString(url.content.text)
        List skip_keys = ['default_ivalue', 'secureSupport', 'friendlyLabel', 'comment', 'matrix']
        //cycle through all permutations of the toolchains file and check bash syntax
        generator.toolchain_obj.languages.each {
            String language = it
            generator.toolchain_obj.toolchains['toolchains'][language].each {
                String toolchain = it
                (generator.toolchain_obj.toolchains[toolchain].keySet() as String[]).each {
                    String toolchain_value = it
                    if(!(toolchain_value in skip_keys)) {
                        //load the yaml permutations
                        String sample_yaml
                        if('*' == it) {
                            sample_yaml = "language: ${language}\n${toolchain}:\n  - hello"
                        }
                        else {
                            sample_yaml = "language: ${language}\n${toolchain}:\n  - \"${toolchain_value}\""
                        }
                        generator.loadYamlString(sample_yaml)
                        //do the syntax checking
                        def stdout = new StringBuilder()
                        def stderr = new StringBuilder()
                        def proc1 = ['echo', generator.generateToolchainSection()].execute()
                        def proc2 = ['bash', '-n'].execute()
                        proc1 | proc2
                        proc2.waitForProcessOutput(stdout, stderr)
                        if(proc2.exitValue()) {
                            //syntax check failed so alert which section of the toolchains.json file failed.
                            throw new JervisException("Toolchains bash syntax error when testing: ${language} > ${toolchain} > ${toolchain_value}\n\nYAML sample:\n${sample_yaml}\n\nBash error:\n" + stderr.toString())
                        }
                    }
                }
            }
        }
    }
}
