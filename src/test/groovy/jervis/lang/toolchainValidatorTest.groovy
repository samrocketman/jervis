package jervis.lang
//the toolchainValidatorTest() class automatically sees the lifecycleValidator() class because they're in the same package
import jervis.exceptions.ToolchainMissingKeyException
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
        URL url = this.getClass().getResource('/toolchains.json');
        toolchains.load_JSON(url.getFile())
        assert true == toolchains.validate()
        assert true == toolchains.validate_asBool()
    }
}
