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
//LifecycleGenerator, LifecycleValidator, ToolchainValidator, and
//PlatformValidator don't have to be imported because they're in the same
//package.

import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.tools.YamlOperator

import org.junit.Test

/**
  Tests user supplied configurations from the resources directory in the root
  of this repository.
  */
class JervisConfigsTest extends GroovyTestCase {
    /**
      Abstracted validatePlatformsString in case an admin needs to test
      multiple files.
      */
    private void validatePlatformsString(String yaml) {
        def platform_obj = new PlatformValidator()
        platform_obj.loadYamlString(yaml)
        platform_obj.validate()
    }

    /**
      A function for linting all toolchains bash scripts in a
      LifecycleGenerator object.
     */
    private void validateLifecycleGeneratorBashSyntax(String osName, LifecycleGenerator generator) {
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
                        proc2.waitFor()
                        proc2.waitForProcessOutput(stdout, stderr)
                        if(proc2.exitValue()) {
                            //syntax check failed so alert which section of the toolchains.yaml file failed.
                            throw new JervisException("${osName}: Toolchains bash syntax error when testing: ${language} > ${toolchain} > ${toolchain_value}\n\nYAML sample:\n${sample_yaml}\n\nBash error:\n" + stderr.toString())
                        }
                    }
                }
            }
        }
    }

    /**
      Test and validate production platforms.yaml.
     */
    @Test public void test_JervisConfigsTest_validate_platforms_config() {
        URL url = this.getClass().getResource('/platforms.yaml');
        validatePlatformsString(url.content.text)
    }

    /**
      Runs through every toolchain configuration to ensure there's no bash
      syntax errors.
     */
    @Test public void test_JervisConfigsTest_toolchains_ubuntu1604_stable_bash_syntax_check() {
        URL url = this.getClass().getResource('/platforms.yaml');
        YamlOperator.loadYamlFrom(url.content.text).supported_platforms.each { platform, oses ->
            oses.collect { os, languages ->
                def generator = new LifecycleGenerator()
                url = this.getClass().getResource("/lifecycles-${os}-stable.yaml");
                generator.loadLifecyclesString(url.content.text)
                url = this.getClass().getResource("/toolchains-${os}-stable.yaml");
                generator.loadToolchainsString(url.content.text)
                validateLifecycleGeneratorBashSyntax(os, generator)
            }
        }
    }
}
