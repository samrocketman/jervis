package jervis.lang
//the toolchainValidatorTest() class automatically sees the lifecycleValidator() class because they're in the same package
import org.junit.*
import jervis.exceptions.ToolchainMissingKeyException

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
    //test supportedLanguage()
    @Test public void test_toolchainValidator_supportedLanguage_yes() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedLanguage("ruby")
    }
    @Test public void test_toolchainValidator_supportedLanguage_no() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedLanguage("derpy")
    }
    //test supportedToolchain()
    @Test public void test_toolchainValidator_supportedToolchain_yes() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedToolchain("jdk")
    }
    @Test public void test_toolchainValidator_supportedToolchain_no() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedToolchain("derpy")
    }
    //test supportedMatrix()
    @Test public void test_toolchainValidator_supportedMatrix_yes() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.supportedMatrix("ruby", "rvm")
    }
    @Test public void test_toolchainValidator_supportedMatrix_no() {
        URL url = this.getClass().getResource("/good_toolchains_simple.json");
        toolchains.load_JSON(url.getFile())
        assert false == toolchains.supportedMatrix("ruby","derpy")
    }
    //test against invalid toolchains files
    @Test public void test_toolchainValidator_bad_toolchains_missing_toolchain() {
        URL url = this.getClass().getResource("/bad_toolchains_missing_toolchain.json");
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_missing_toolchains() {
        URL url = this.getClass().getResource("/bad_toolchains_missing_toolchains.json");
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_bad_toolchains_missing_default_ivalue() {
        URL url = this.getClass().getResource("/bad_toolchains_missing_default_ivalue.json");
        toolchains.load_JSON(url.getFile())
        shouldFail(ToolchainMissingKeyException) {
            toolchains.validate()
        }
        assert false == toolchains.validate_asBool()
    }
    @Test public void test_toolchainValidator_main_toolchains_json() {
        URL url = this.getClass().getResource("/toolchains.json");
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.validate()
        assert true == toolchains.validate_asBool()
    }
}
