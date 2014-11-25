package jervis.lang
//the lifecycleValidatorTest() class automatically sees the lifecycleValidatorTest() class because they're in the same package
import org.junit.*

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
    //test supportedLanguage()
    @Test public void test_lifecycleValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource("/good_lifecycles_simple.json");
        lifecycles.load_JSON(url)
        assert true == lifecycles.supportedLanguage("groovy")
    }
    @Test public void test_lifecycleValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource("/good_lifecycles_simple.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.supportedLanguage("derpy")
    }
    //test against invalid lifecycle files
    @Test public void test_lifecycleValidator_bad_lifecycles_missing_defaultKey() {
        URL url = this.getClass().getResource("/bad_lifecycles_missing_defaultKey.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_missing_fileExistsCondition() {
        URL url = this.getClass().getResource("/bad_lifecycles_missing_fileExistsCondition.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_defaultKey() {
        URL url = this.getClass().getResource("/bad_lifecycles_resolve_defaultKey.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_fallbackKey() {
        URL url = this.getClass().getResource("/bad_lifecycles_resolve_fallbackKey.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_fileExistsCondition() {
        URL url = this.getClass().getResource("/bad_lifecycles_resolve_fileExistsCondition.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_bad_lifecycles_resolve_infinite_loop() {
        URL url = this.getClass().getResource("/bad_lifecycles_resolve_infinite_loop.json");
        lifecycles.load_JSON(url)
        assert false == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_good_lifecycles_simple() {
        URL url = this.getClass().getResource("/good_lifecycles_simple.json");
        lifecycles.load_JSON(url)
        assert true == lifecycles.validate_asBool()
    }
    @Test public void test_lifecycleValidator_main_lifecycles_json() {
        URL url = this.getClass().getResource("/lifecycles.json");
        lifecycles.load_JSON(url)
        assert true == lifecycles.validate()
    }
}
