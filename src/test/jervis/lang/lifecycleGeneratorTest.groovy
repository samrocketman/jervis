package jervis.lang
//the lifecycleGeneratorTest() class automatically sees the lifecycleGenerator() class because they're in the same package
import jervis.exceptions.UnsupportedLanguageException
import jervis.exceptions.UnsupportedToolException
import org.junit.*

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
        assert "groovy" == generator.yaml_language
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
    @Test public void test_lifecycleGenerator_generateToolchainSection_nonmatrix() {
        URL url = this.getClass().getResource("/good_lifecycles_simple.json");
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource("/good_toolchains_simple.json");
        generator.loadToolchains(url.getFile())
        generator.loadYaml("language: ruby")
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYaml("language: ruby\nenv: foo=bar")
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYaml("language: ruby\nenv:\n  - foo=bar")
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYaml("language: ruby\njdk: openjdk7")
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYaml("language: ruby\njdk: derp")
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
        generator.loadYaml("language: ruby\njdk: 2.5")
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
    }
}
