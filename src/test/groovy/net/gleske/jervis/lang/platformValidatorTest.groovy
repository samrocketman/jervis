/*
   Copyright 2014-2016 Sam Gleske - https://github.com/samrocketman/jervis

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
//the platformValidatorTest() class automatically sees the platformValidator() class because they're in the same package
import net.gleske.jervis.exceptions.PlatformBadValueInKeyException
import net.gleske.jervis.exceptions.PlatformMissingKeyException
import net.gleske.jervis.exceptions.PlatformValidationException
import org.junit.After
import org.junit.Before
import org.junit.Test

class platformValidatorTest extends GroovyTestCase {
    def platforms
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        platforms = new platformValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        platforms = null
        super.tearDown()
    }
    @Test public void test_platformValidator_load_JSON() {
        assert platforms.platforms == null
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        platforms.load_JSON(url.getFile())
        assert platforms.platforms instanceof Map
        assert platforms.platforms['defaults']['platform'] == 'docker'
    }
    @Test public void test_platformValidator_load_JSONString() {
        assert platforms.platforms == null
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        String contents = new File(url.getFile()).getText()
        //use a string this time
        platforms.load_JSONString(contents)
        assert platforms.platforms instanceof Map
        assert platforms.platforms['defaults']['platform'] == 'docker'
    }
    @Test public void test_platformValidator_bad_platforms_missing_root_defaults() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_defaults.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_bad_platforms_missing_root_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_supported_platforms.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_bad_platforms_missing_root_restrictions() {
        URL url = this.getClass().getResource('/bad_platforms_missing_root_restrictions.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_bad_platforms_rootkey_defaults() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_defaults.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_bad_platforms_rootkey_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_supported_platforms.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_bad_platforms_rootkey_restrictions() {
        URL url = this.getClass().getResource('/bad_platforms_rootkey_restrictions.json')
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
        assert false == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_good_platforms_simple() {
        URL url = this.getClass().getResource('/good_platforms_simple.json');
        platforms.load_JSON(url.getFile())
        assert true == platforms.validate()
        assert true == platforms.validate_asBool()
    }
    @Test public void test_platformValidator_main_platforms_json() {
        URL url = this.getClass().getResource('/platforms.json');
        platforms.load_JSON(url.getFile())
        assert true == platforms.validate()
    }
    @Test public void test_platformValidator_bad_missing_defaults() {
        ['platform', 'os', 'stability', 'sudo'].each {
            URL url = this.getClass().getResource("/bad_platforms_missing_defaults_${it}.json");
            platforms.load_JSON(url.getFile())
            shouldFail(PlatformMissingKeyException) {
                platforms.validate()
            }
            url = this.getClass().getResource("/bad_platforms_type_defaults_${it}.json");
            platforms.load_JSON(url.getFile())
            shouldFail(PlatformBadValueInKeyException) {
                platforms.validate()
            }
        }
    }
    @Test public void test_platformValidator_bad_supported_platforms_empty() {
        URL url = this.getClass().getResource('/bad_platforms_supported_platforms_empty.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_supported_platforms() {
        URL url = this.getClass().getResource('/bad_platforms_value_supported_platforms.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_defaults_platform() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_defaults_os() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_os.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_sudo() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_sudo.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_stability() {
        URL url = this.getClass().getResource('/bad_platforms_value_defaults_stability.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_supported_platforms_defaults_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_defaults_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_missing_supported_platforms_platform_os_language() {
        URL url = this.getClass().getResource('/bad_platforms_missing_supported_platforms_platform_os_language.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_missing_supported_platforms_platform_os_toolchain() {
        URL url = this.getClass().getResource('/bad_platforms_missing_supported_platforms_platform_os_toolchain.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_supported_platforms_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_supported_platforms_platform_os() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_supported_platforms_platform_os_language() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os_language.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_supported_platforms_platform_os_toolchain() {
        URL url = this.getClass().getResource('/bad_platforms_type_supported_platforms_platform_os_toolchain.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_value_supported_platforms_platform() {
        URL url = this.getClass().getResource('/bad_platforms_value_supported_platforms_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_restrictions_platform() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_restrictions_platform_only_organizations() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform_only_organizations.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_type_restrictions_platform_only_projects() {
        URL url = this.getClass().getResource('/bad_platforms_type_restrictions_platform_only_projects.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformBadValueInKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_bad_missing_restrictions_platform() {
        URL url = this.getClass().getResource('/bad_platforms_missing_restrictions_platform.json');
        platforms.load_JSON(url.getFile())
        shouldFail(PlatformMissingKeyException) {
            platforms.validate()
        }
    }
    @Test public void test_platformValidator_good_platforms_optional() {
        URL url = this.getClass().getResource('/good_platforms_optional.json');
        platforms.load_JSON(url.getFile())
        assert true == platforms.validate()
        assert true == platforms.validate_asBool()
    }
}
