package jervis.lang
//the lifecycleValidatorTest() class automatically sees the lifecycleValidator() class because they're in the same package
import jervis.exceptions.LifecycleBadValueInKeyException
import jervis.exceptions.LifecycleInfiniteLoopException
import jervis.exceptions.LifecycleMissingKeyException
import jervis.exceptions.LifecycleValidationException
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
        assert lifecycles.languages == ['groovy', 'ruby', 'java']
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
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_fileExistsCondition() {
        URL url = this.getClass().getResource('/bad_lifecycles_resolve_fileExistsCondition.json');
        lifecycles.load_JSON(url.getFile())
        shouldFail(LifecycleBadValueInKeyException) {
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
