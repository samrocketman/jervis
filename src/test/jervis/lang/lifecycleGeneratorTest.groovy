package jervis.lang
//the lifecycleGeneratorTest() class automatically sees the lifecycleGenerator() class because they're in the same package
import org.junit.*
import jervis.exceptions.UnsupportedLanguageException

class lifecycleGeneratorTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new lifecycleGenerator()
    }
    //tear down after every test
    @After protected void tearDown() {
        generator = null
        super.tearDown()
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_yes() {
        generator.loadYaml("language: groovy")
        assert "groovy" == generator.language
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_no() {
        shouldFail(UnsupportedLanguageException) {
            generator.loadYaml("language: derp")
        }
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_false() {
        generator.loadYaml("language: groovy\nenv: foo=bar")
        assert false == generator.isMatrixBuild()
        generator.loadYaml("language: groovy\nenv:\n  - foo=bar")
        assert false == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_true() {
        generator.loadYaml("language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar")
        assert true == generator.isMatrixBuild()
    }
}
