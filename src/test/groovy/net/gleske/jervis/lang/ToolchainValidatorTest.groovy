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
//the ToolchainValidatorTest() class automatically sees the LifecycleValidator() class because they're in the same package
import net.gleske.jervis.exceptions.ToolchainBadValueInKeyException
import net.gleske.jervis.exceptions.ToolchainMissingKeyException

import org.junit.After
import org.junit.Before
import org.junit.Test

class ToolchainValidatorTest extends GroovyTestCase {
    def toolchains
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        toolchains = new ToolchainValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        toolchains = null
        super.tearDown()
    }
    @Test public void test_ToolchainValidator_loadYamlFile_partial_unstable() {
        URL url = this.getClass().getResource('/good_toolchains_matrix_added_toolchain.json');
        toolchains.loadYamlFile(url.getFile())
        assert toolchains.validate(true) == true
        assert toolchains.validate(false) == true
        assert toolchains.validate() == true
        assert toolchains.validate_asBool() == true
        assert toolchains.getToolchain_list(false) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.getToolchain_list(true) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.getLanguages(false) == ['python']
        assert toolchains.getLanguages(true) == ['python']
        assert toolchains.getMatrix_toolchain_list(true) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.getMatrix_toolchain_list(false) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.isFriendlyLabel('python', false) == false
        assert toolchains.isFriendlyLabel('python', true) == false
        assert toolchains.supportedTool('python', '3.6', false) == true
        assert toolchains.supportedTool('python', '3.6', true) == true
        assert toolchains.supportedTool('python', '3.10', false) == false
        assert toolchains.supportedTool('python', '3.10', true) == false
        assert toolchains.supportedMatrix('java', 'jdk', false) == false
        assert toolchains.supportedMatrix('java', 'jdk', true) == false
        assert toolchains.supportedMatrix('python', 'go', false) == false
        assert toolchains.supportedMatrix('python', 'go', true) == false
        assert toolchains.toolchainType('env', false) == 'advanced'
        assert toolchains.toolchainType('env', true) == 'advanced'
        assert toolchains.supportedLanguage('java', false) == false
        assert toolchains.supportedLanguage('java', true) == false
        assert toolchains.supportedToolchain('go', false) == false
        assert toolchains.supportedToolchain('go', true) == false
    }
    @Test public void test_ToolchainValidator_loadYamlFile_partial_unstable_load() {
        URL url = this.getClass().getResource('/good_toolchains_matrix_added_toolchain.json');
        toolchains.loadYamlFile(url.getFile())
        url = this.getClass().getResource('/good_toolchains_partial_unstable.yaml');
        // load unstable
        toolchains.loadYamlFile(url.getFile(), true)
        assert toolchains.validate(true) == true
        assert toolchains.validate(false) == true
        assert toolchains.validate() == true
        assert toolchains.validate_asBool() == true
        assert toolchains.getToolchain_list(false) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.getToolchain_list(true) == ['toolchains', 'env', 'jdk', 'python', 'go']
        assert toolchains.getLanguages(false) == ['python']
        assert toolchains.getLanguages(true) == ['python', 'java']
        assert toolchains.getMatrix_toolchain_list(false) == ['toolchains', 'env', 'jdk', 'python']
        assert toolchains.getMatrix_toolchain_list(true) == ['toolchains', 'jdk', 'python', 'go']
        assert toolchains.isFriendlyLabel('python', false) == false
        assert toolchains.isFriendlyLabel('python', true) == true
        assert toolchains.supportedTool('python', '3.6', false) == true
        assert toolchains.supportedTool('python', '3.6', true) == true
        assert toolchains.supportedTool('python', '3.10', false) == false
        assert toolchains.supportedTool('python', '3.10', true) == true
        assert toolchains.supportedMatrix('java', 'jdk', false) == false
        assert toolchains.supportedMatrix('java', 'jdk', true) == true
        assert toolchains.toolchainType('env', false) == 'advanced'
        assert toolchains.toolchainType('env', true) == 'disabled'
        assert toolchains.supportedLanguage('java', false) == false
        assert toolchains.supportedLanguage('java', true) == true
        assert toolchains.supportedToolchain('go', false) == false
        assert toolchains.supportedToolchain('go', true) == true
    }
    @Test public void test_ToolchainValidator_loadYamlFile() {
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert toolchains.toolchains['jdk']['default_ivalue'] == 'openjdk7'
        assert 'toolchains' in toolchains.toolchain_list
        assert 'gemfile' in toolchains.toolchain_list
        assert 'jdk' in toolchains.toolchain_list
        assert 'env' in toolchains.toolchain_list
        assert 'rvm' in toolchains.toolchain_list
        assert 'ruby' in toolchains.languages
        assert 'java' in toolchains.languages
        toolchains == null
        toolchains = new ToolchainValidator()
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        toolchains.loadYamlFile(url.getFile())
        assert toolchains.languages == null
    }
    @Test public void test_ToolchainValidator_loadYamlString() {
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        String contents = new File(url.getFile()).getText()
        //pass the string contents
        toolchains.loadYamlString(contents)
        assert toolchains.toolchains['jdk']['default_ivalue'] == 'openjdk7'
        assert 'toolchains' in toolchains.toolchain_list
        assert 'gemfile' in toolchains.toolchain_list
        assert 'jdk' in toolchains.toolchain_list
        assert 'env' in toolchains.toolchain_list
        assert 'rvm' in toolchains.toolchain_list
        assert 'ruby' in toolchains.languages
        assert 'java' in toolchains.languages
        toolchains == null
        toolchains = new ToolchainValidator()
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        contents = new File(url.getFile()).getText()
        toolchains.loadYamlString(contents)
        assert toolchains.languages == null
    }
    //test supportedLanguage()
    @Test public void test_ToolchainValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedLanguage('ruby')
    }
    @Test public void test_ToolchainValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedLanguage('derpy')
    }
    //test supportedToolchain()
    @Test public void test_ToolchainValidator_supportedToolchain_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedToolchain('jdk')
    }
    @Test public void test_ToolchainValidator_supportedToolchain_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedToolchain('derpy')
    }
    //test supportedTool()
    @Test public void test_ToolchainValidator_supportedTool_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedTool('jdk','openjdk7')
        assert true == toolchains.supportedTool('rvm','derpy')
    }
    @Test public void test_ToolchainValidator_supportedTool_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedTool('jdk','derpy')
    }
    //test supportedMatrix()
    @Test public void test_ToolchainValidator_supportedMatrix_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedMatrix('ruby', 'rvm')
    }
    @Test public void test_ToolchainValidator_supportedMatrix_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedMatrix('ruby','derpy')
    }
    //test against invalid toolchains files
    @Test public void test_ToolchainValidator_bad_toolchains_missing_toolchain() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_toolchain.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_bad_toolchains_missing_toolchains() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_bad_toolchains_missing_default_ivalue() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_default_ivalue.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_isFriendlyLabel() {
        URL url = this.getClass().getResource('/good_toolchains_friendly.json');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.isFriendlyLabel('env')
        assert true == toolchains.isFriendlyLabel('rvm')
    }
    @Test public void test_ToolchainValidator_isFriendlyLabel_bad_type() {
        URL url = this.getClass().getResource('/bad_toolchains_friendly.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_formerly_good_toolchains_simple() {
        //this test is for migrations from jervis-0.9 to 0.10 because advanced matrices were introduced
        URL url = this.getClass().getResource('/bad_toolchains_formerly_good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_toolchainType() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        assert 'advanced' == toolchains.toolchainType('env')
        assert 'simple' == toolchains.toolchainType('jdk')
        assert 'simple' == toolchains.toolchainType('foo')
    }
    @Test public void test_ToolchainValidator_bad_toolchains_matrix_value() {
        URL url = this.getClass().getResource('/bad_toolchains_matrix_value.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_bad_toolchains_cleanup_list() {
        URL url = this.getClass().getResource('/bad_toolchains_cleanup_list.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_bad_toolchains_cleanup_string() {
        URL url = this.getClass().getResource('/bad_toolchains_cleanup_string.json');
        toolchains.loadYamlFile(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_good_toolchains_cleanup() {
        URL url = this.getClass().getResource('/good_toolchains_cleanup.json');
        toolchains.loadYamlFile(url.getFile())
        toolchains.validate()
        assert true == toolchains.validate_asBool()
    }
    @Test public void test_ToolchainValidator_serialization() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.loadYamlFile(url.getFile())
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(toolchains)
    }
    //test YAML supportedLanguage()
    @Test public void test_ToolchainValidator_supportedLanguage_yes_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedLanguage('ruby')
    }
    @Test public void test_ToolchainValidator_supportedLanguage_no_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedLanguage('derpy')
    }
    //test supportedToolchain()
    @Test public void test_ToolchainValidator_supportedToolchain_yes_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedToolchain('jdk')
    }
    @Test public void test_ToolchainValidator_supportedToolchain_no_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedToolchain('derpy')
    }
    //test supportedTool()
    @Test public void test_ToolchainValidator_supportedTool_yes_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedTool('jdk','openjdk7')
        assert true == toolchains.supportedTool('rvm','derpy')
    }
    @Test public void test_ToolchainValidator_supportedTool_no_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedTool('jdk','derpy')
    }
    //test supportedMatrix()
    @Test public void test_ToolchainValidator_supportedMatrix_yes_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert true == toolchains.supportedMatrix('ruby', 'rvm')
    }
    @Test public void test_ToolchainValidator_supportedMatrix_no_yaml() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert false == toolchains.supportedMatrix('ruby','derpy')
    }
    @Test public void test_ToolchainValidator_toolValues() {
        URL url = this.getClass().getResource('/good_toolchains_simple.yaml');
        toolchains.loadYamlFile(url.getFile())
        assert toolchains.toolValues('jdk') == ['openjdk6', 'openjdk7']
        assert toolchains.toolValues('python') == ['2.6', '2.7']
        assert toolchains.toolValues('rvm') == []
    }
}
