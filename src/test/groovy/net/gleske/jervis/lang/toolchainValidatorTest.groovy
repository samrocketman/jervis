/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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
//the toolchainValidatorTest() class automatically sees the lifecycleValidator() class because they're in the same package
import net.gleske.jervis.exceptions.ToolchainBadValueInKeyException
import net.gleske.jervis.exceptions.ToolchainMissingKeyException
import org.junit.After
import org.junit.Before
import org.junit.Test

class toolchainValidatorTest extends GroovyTestCase {
    def toolchains
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        toolchains = new toolchainValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        toolchains = null
        super.tearDown()
    }
    @Test public void test_toolchainValidator_load_JSON() {
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert toolchains.toolchains instanceof Map
        assert toolchains.toolchain_list instanceof String[]
        assert toolchains.languages instanceof String[]
        assert toolchains.toolchains['jdk']['default_ivalue'] == 'openjdk7'
        assert 'toolchains' in toolchains.toolchain_list
        assert 'gemfile' in toolchains.toolchain_list
        assert 'jdk' in toolchains.toolchain_list
        assert 'env' in toolchains.toolchain_list
        assert 'rvm' in toolchains.toolchain_list
        assert 'ruby' in toolchains.languages
        assert 'java' in toolchains.languages
        toolchains == null
        toolchains = new toolchainValidator()
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        toolchains.load_JSON(url.getFile())
        assert toolchains.toolchains instanceof Map
        assert toolchains.toolchain_list instanceof String[]
        assert toolchains.languages == null
    }
    @Test public void test_toolchainValidator_load_JSONString() {
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        String contents = new File(url.getFile()).getText()
        //pass the string contents
        toolchains.load_JSONString(contents)
        assert toolchains.toolchains instanceof Map
        assert toolchains.toolchain_list instanceof String[]
        assert toolchains.languages instanceof String[]
        assert toolchains.toolchains['jdk']['default_ivalue'] == 'openjdk7'
        assert 'toolchains' in toolchains.toolchain_list
        assert 'gemfile' in toolchains.toolchain_list
        assert 'jdk' in toolchains.toolchain_list
        assert 'env' in toolchains.toolchain_list
        assert 'rvm' in toolchains.toolchain_list
        assert 'ruby' in toolchains.languages
        assert 'java' in toolchains.languages
        toolchains == null
        toolchains = new toolchainValidator()
        assert toolchains.toolchains == null
        assert toolchains.toolchain_list == null
        assert toolchains.languages == null
        url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        contents = new File(url.getFile()).getText()
        toolchains.load_JSONString(contents)
        assert toolchains.toolchains instanceof Map
        assert toolchains.toolchain_list instanceof String[]
        assert toolchains.languages == null
    }
    //test supportedLanguage()
    @Test public void test_toolchainValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedLanguage('ruby')
    }
    @Test public void test_toolchainValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedLanguage('derpy')
    }
    //test supportedToolchain()
    @Test public void test_toolchainValidator_supportedToolchain_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedToolchain('jdk')
    }
    @Test public void test_toolchainValidator_supportedToolchain_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedToolchain('derpy')
    }
    //test supportedTool()
    @Test public void test_toolchainValidator_supportedTool_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedTool('jdk','openjdk7')
        assert true == toolchains.supportedTool('rvm','derpy')
    }
    @Test public void test_toolchainValidator_supportedTool_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedTool('jdk','derpy')
    }
    //test supportedMatrix()
    @Test public void test_toolchainValidator_supportedMatrix_yes() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedMatrix('ruby', 'rvm')
    }
    @Test public void test_toolchainValidator_supportedMatrix_no() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedMatrix('ruby','derpy')
    }
    //test against invalid toolchains files
    @Test public void test_toolchainValidator_bad_toolchains_missing_toolchain() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_toolchain.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_missing_toolchains() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_toolchains.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_missing_default_ivalue() {
        URL url = this.getClass().getResource('/bad_toolchains_missing_default_ivalue.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_main_toolchains_json() {
        URL url = this.getClass().getResource('/toolchains-ubuntu1604-stable.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.validate()
        assert true == toolchains.validate_asBool()
        (toolchains.toolchains.keySet() as List).each {
            assert it instanceof String
        }
    }
    @Test public void test_toolchainValidator_isFriendlyLabel() {
        URL url = this.getClass().getResource('/good_toolchains_friendly.json');
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.isFriendlyLabel('env')
        assert true == toolchains.isFriendlyLabel('rvm')
    }
    @Test public void test_toolchainValidator_formerly_good_toolchains_simple() {
        //this test is for migrations from jervis-0.9 to 0.10 because advanced matrices were introduced
        URL url = this.getClass().getResource('/bad_toolchains_formerly_good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_toolchainType() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        assert 'advanced' == toolchains.toolchainType('env')
        assert 'simple' == toolchains.toolchainType('jdk')
        assert 'simple' == toolchains.toolchainType('foo')
    }
    @Test public void test_toolchainValidator_bad_toolchains_matrix_value() {
        URL url = this.getClass().getResource('/bad_toolchains_matrix_value.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_cleanup_list() {
        URL url = this.getClass().getResource('/bad_toolchains_cleanup_list.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_cleanup_string() {
        URL url = this.getClass().getResource('/bad_toolchains_cleanup_string.json');
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainBadValueInKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_good_toolchains_cleanup() {
        URL url = this.getClass().getResource('/good_toolchains_cleanup.json');
        toolchains.load_JSON(url.getFile())
        toolchains.validate()
        assert true == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_serialization() {
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        toolchains.load_JSON(url.getFile())
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(toolchains)
    }
}
