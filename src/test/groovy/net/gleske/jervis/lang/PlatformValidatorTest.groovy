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
//the PlatformValidatorTest() class automatically sees the PlatformValidator() class because they're in the same package
import net.gleske.jervis.exceptions.PlatformBadValueInKeyException
import net.gleske.jervis.exceptions.PlatformMissingKeyException
import net.gleske.jervis.exceptions.PlatformValidationException
import net.gleske.jervis.tools.YamlOperator

import org.junit.After
import org.junit.Before
import org.junit.Test

class PlatformValidatorTest extends GroovyTestCase {
    def platforms
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        platforms = new PlatformValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        platforms = null
        super.tearDown()
    }
    @Test public void test_PlatformValidator_loadYamlFile() {
        assert platforms.platforms == null
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        platforms.loadYamlFile(url.getFile())
        assert platforms.platforms instanceof Map
        assert platforms.platforms['defaults']['platform'] == 'docker'
    }
    @Test public void test_PlatformValidator_loadYamlString() {
        assert platforms.platforms == null
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        String contents = new File(url.getFile()).getText()
        //use a string this time
        platforms.loadYamlString(contents)
        assert platforms.platforms instanceof Map
        assert platforms.platforms['defaults']['platform'] == 'docker'
    }
    @Test public void test_PlatformValidator_bad_platforms_missing_root_defaults() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_defaults.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_platforms_missing_root_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_supported_platforms.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_platforms_missing_root_restrictions() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_restrictions.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_platforms_rootkey_defaults() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_defaults.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_platforms_rootkey_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_supported_platforms.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_platforms_rootkey_restrictions() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_restrictions.json')
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_good_platforms_simple() {
        URL url = this.getClass().getResource('/good_platforms_simple.json');
        platforms.loadYamlFile(url.getFile())
        assert true == platforms.validate()
        assert true == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_bad_missing_defaults() {
        ['platform', 'os', 'stability', 'sudo'].each {
            URL url = this.getClass().getResource("/bad_platforms_missing_defaults_${it}.json");
            platforms.loadYamlFile(url.getFile())
            shouldFail(PlatformMissingKeyException) {
                platforms.validate()
            }
            url = this.getClass().getResource("/bad_platforms_type_defaults_${it}.json");
            platforms.loadYamlFile(url.getFile())
            shouldFail(PlatformBadValueInKeyException) {
                platforms.validate()
            }
        }
    }
    @Test public void test_PlatformValidator_bad_supported_platforms_empty() {
        URL url = this.getClass().getResource('/bad_platforms_supported_platforms_empty.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_value_supported_platforms.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_defaults_platform() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_defaults_os() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_os.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_sudo() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_sudo.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_stability() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_stability.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_supported_platforms_defaults_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_defaults_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_missing_supported_platforms_platform_os_language() {
        URL url = this.getClass().getResource('/bad_platforms_missing_supported_platforms_platform_os_language.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_missing_supported_platforms_platform_os_toolchain() {
        URL url = this.getClass().getResource('/bad_platforms_missing_supported_platforms_platform_os_toolchain.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_supported_platforms_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_supported_platforms_platform_os() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_supported_platforms_platform_os_language() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os_language.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_supported_platforms_platform_os_toolchain() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os_toolchain.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_value_supported_platforms_platform() {
        URL url = this.getClass().getResource('/bad_platforms_value_supported_platforms_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_restrictions_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_restrictions_platform_only_organizations() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform_only_organizations.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_type_restrictions_platform_only_projects() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform_only_projects.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_bad_missing_restrictions_platform() {
        URL url = this.getClass().getResource('/bad_platforms_missing_restrictions_platform.json');
        platforms.loadYamlFile(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_PlatformValidator_good_platforms_optional() {
        URL url = this.getClass().getResource('/good_platforms_optional.json')
        platforms.loadYamlFile(url.getFile())
        assert true == platforms.validate()
        assert true == platforms.validate_asBool()
    }
    @Test public void test_PlatformValidator_partial_unstable() {
        URL url = this.getClass().getResource('/good_platforms_partial.yaml')
        platforms.loadYamlFile(url.getFile())
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.friendlyName', '') == 'Ubuntu 22.04'
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.language', []) == ['python']
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.toolchain', []) == ['env', 'python']
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.friendlyName', '') == 'Ubuntu 22.04'
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.language', []) == ['python']
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.toolchain', []) == ['env', 'python']
        url = this.getClass().getResource('/good_platforms_partial_unstable.yaml')
        platforms.loadYamlFile(url.getFile(), true)
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.friendlyName', '') == 'Ubuntu 22.04'
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.language', []) == ['python']
        assert YamlOperator.getObjectValue(platforms.platforms, 'supported_platforms.default.ubuntu2204.toolchain', []) == ['env', 'python']
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.friendlyName', '') == 'Ubuntu 22.04'
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.language', []) == ['java', 'python']
        assert YamlOperator.getObjectValue(platforms.getPlatforms(true), 'supported_platforms.default.ubuntu2204.toolchain', []) == ['env', 'jdk', 'python']
    }
    @Test public void test_PlatformValidator_partial_platform_os() {
        URL url = this.getClass().getResource('/good_platforms_partial.yaml')
        platforms.loadYamlFile(url.getFile())
        assert platforms.platforms.defaults.sudo == 'sudo'
        assert platforms.platforms.supported_platforms.keySet().toList().sort() == ['default']
        assert platforms.platforms.supported_platforms.default.keySet().toList().sort() == ['ubuntu2204']
        url = this.getClass().getResource('/good_platforms_partial_add_platform_os.yaml')
        platforms.loadYamlFile(url.getFile(), true)
        assert platforms.platforms.defaults.sudo == 'sudo'
        assert platforms.platforms.supported_platforms.keySet().toList().sort() == ['default']
        assert platforms.platforms.supported_platforms.default.keySet().toList().sort() == ['ubuntu2204']
        assert platforms.getPlatforms(true).defaults.sudo == 'nosudo'
        assert platforms.getPlatforms(true).supported_platforms.keySet().toList().sort() == ['arm64', 'default']
        assert platforms.getPlatforms(true).supported_platforms.default.keySet().toList().sort() == ['alpine', 'ubuntu2204']
    }
    @Test public void test_PlatformValidator_serialization() {
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        platforms.loadYamlFile(url.getFile())
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(platforms)
    }
}
