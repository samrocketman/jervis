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
//the LifecycleValidatorTest() class automatically sees the LifecycleValidator() class because they're in the same package
import net.gleske.jervis.exceptions.LifecycleBadValueInKeyException
import net.gleske.jervis.exceptions.LifecycleInfiniteLoopException
import net.gleske.jervis.exceptions.LifecycleMissingKeyException
import net.gleske.jervis.exceptions.LifecycleValidationException

import org.junit.After
import org.junit.Before
import org.junit.Test

class LifecycleValidatorTest extends GroovyTestCase {
    def lifecycles
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        lifecycles = new LifecycleValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        lifecycles = null
        super.tearDown()
    }
    @Test public void test_LifecycleValidator_loadYamlFile() {
        assert lifecycles.lifecycles == null
        assert lifecycles.languages == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        assert lifecycles.lifecycles['groovy']['friendlyName'] == 'Groovy'
        assert 'groovy' in lifecycles.languages
        assert 'ruby' in lifecycles.languages
        assert 'java' in lifecycles.languages
    }
    @Test public void test_LifecycleValidator_loadYamlString() {
        assert lifecycles.lifecycles == null
        assert lifecycles.languages == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        String contents = new File(url.getFile()).getText()
        //use a string this time
        lifecycles.loadYamlString(contents)
        assert lifecycles.lifecycles['groovy']['friendlyName'] == 'Groovy'
        assert 'groovy' in lifecycles.languages
        assert 'ruby' in lifecycles.languages
        assert 'java' in lifecycles.languages
    }
    //test supportedLanguage()
    @Test public void test_LifecycleValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        assert true == lifecycles.supportedLanguage('groovy')
    }
    @Test public void test_LifecycleValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        assert false == lifecycles.supportedLanguage('derpy')
    }
    //test against invalid lifecycle files
    @Test public void test_LifecycleValidator_bad_lifecycles_missing_defaultKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_defaultKey.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_bad_lifecycles_missing_fileExistsCondition() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_fileExistsCondition.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_bad_lifecycles_missing_friendlyName() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_friendlyName.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_bad_lifecycles_resolve_defaultKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_defaultKey.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_bad_lifecycles_resolve_fallbackKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_fallbackKey.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_bad_lifecycles_resolve_infinite_loop() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_infinite_loop.json');
        lifecycles.loadYamlFile(url.getFile())
        shouldFail(LifecycleInfiniteLoopException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_good_lifecycles_simple() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        assert true == lifecycles.validate()
        assert true == lifecycles.validate_asBool()
    }
    @Test public void test_LifecycleValidator_serialization() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(lifecycles)
    }
    @Test public void test_LifecycleValidator_loadYamlString_invalid_language_lifecycle() {
        lifecycles.loadYamlString('hello: world')
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    //test supportedLanguage()
    @Test public void test_LifecycleValidator_supportedLanguage_partial_unstable() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.loadYamlFile(url.getFile())
        lifecycles.validate()
        assert true == lifecycles.supportedLanguage('groovy')
        assert false == lifecycles.supportedLanguage('python')
        url = this.getClass().getResource('/good_lifecycles_python_number.json');
        // load unstable
        lifecycles.loadYamlFile(url.getFile(), true)
        lifecycles.validate()
        assert true == lifecycles.supportedLanguage('groovy')
        assert false == lifecycles.supportedLanguage('python')
        // supported when unstable enabled
        assert true == lifecycles.supportedLanguage('python', true)
    }
}
