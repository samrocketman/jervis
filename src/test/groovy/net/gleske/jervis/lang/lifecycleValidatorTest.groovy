/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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
//the lifecycleValidatorTest() class automatically sees the lifecycleValidator() class because they're in the same package
import net.gleske.jervis.exceptions.LifecycleBadValueInKeyException
import net.gleske.jervis.exceptions.LifecycleInfiniteLoopException
import net.gleske.jervis.exceptions.LifecycleMissingKeyException
import net.gleske.jervis.exceptions.LifecycleValidationException
import org.junit.After
import org.junit.Before
import org.junit.Test

class lifecycleValidatorTest extends GroovyTestCase {
    def lifecycles
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        lifecycles = new lifecycleValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        lifecycles = null
        super.tearDown()
    }
    @Test public void test_lifecycleValidator_load_JSON() {
        assert lifecycles.lifecycles == null
        assert lifecycles.languages == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.load_JSON(url.getFile())
        assert lifecycles.lifecycles instanceof Map
        assert lifecycles.languages instanceof String[]
        assert lifecycles.lifecycles['groovy']['friendlyName'] == 'Groovy'
        assert 'groovy' in lifecycles.languages
        assert 'ruby' in lifecycles.languages
        assert 'java' in lifecycles.languages
    }
    @Test public void test_lifecycleValidator_load_JSONString() {
        assert lifecycles.lifecycles == null
        assert lifecycles.languages == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        String contents = new File(url.getFile()).getText()
        //use a string this time
        lifecycles.load_JSONString(contents)
        assert lifecycles.lifecycles instanceof Map
        assert lifecycles.languages instanceof String[]
        assert lifecycles.lifecycles['groovy']['friendlyName'] == 'Groovy'
        assert 'groovy' in lifecycles.languages
        assert 'ruby' in lifecycles.languages
        assert 'java' in lifecycles.languages
    }
    //test supportedLanguage()
    @Test public void test_lifecycleValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.load_JSON(url.getFile())
        assert true == lifecycles.supportedLanguage('groovy')
    }
    @Test public void test_lifecycleValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.load_JSON(url.getFile())
        assert false == lifecycles.supportedLanguage('derpy')
    }
    //test against invalid lifecycle files
    @Test public void test_lifecycleValidator_bad_lifecycles_missing_defaultKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_defaultKey.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_missing_fileExistsCondition() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_fileExistsCondition.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_missing_friendlyName() {
        URL url = this.getClass().getResource('/bad_lifecycles_missing_friendlyName.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_defaultKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_defaultKey.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_fallbackKey() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_fallbackKey.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleMissingKeyException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_infinite_loop() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_infinite_loop.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleInfiniteLoopException) {
            lifecycles.validate()
        }
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_good_lifecycles_simple() {
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        lifecycles.load_JSON(url.getFile())
        assert true == lifecycles.validate()
        assert true == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_main_lifecycles_json() {
        URL url = this.getClass().getResource('/lifecycles.json');
        lifecycles.load_JSON(url.getFile())
        assert true == lifecycles.validate()
    }
}
