package jervis.lang
//the lifecycleGeneratorTest() class automatically sees the lifecycleGenerator() class because they're in the same package
import jervis.exceptions.JervisException
import jervis.exceptions.UnsupportedLanguageException
import jervis.exceptions.UnsupportedToolException
import org.junit.*

class lifecycleGeneratorTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new lifecycleGenerator()
        URL url = this.getClass().getResource("/good_lifecycles_simple.json");
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource("/good_toolchains_simple.json");
        generator.loadToolchains(url.getFile())
    }
    //tear down after every test
    @After protected void tearDown() {
        generator = null
        super.tearDown()
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_yes() {
        generator.loadYaml("language: ruby")
        assert "ruby" == generator.yaml_language
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_no() {
        //not in lifecycles and not in toolchains
        shouldFail(UnsupportedLanguageException) {
            generator.loadYaml("language: derp")
        }
        //in lifecycles but not in toolchains
        shouldFail(UnsupportedLanguageException) {
            generator.loadYaml("language: groovy")
        }
    }
    @Test public void test_lifecycleGenerator_setfolder_listing() {
        shouldFail(JervisException) {
            generator.folder_listing = ["Gemfile.lock", "Gemfile"]
        }
        generator.loadYaml("language: ruby")
        generator.folder_listing = ["Gemfile.lock", "Gemfile"]
        assert 'rake1' == generator.lifecycle_key
        generator.folder_listing = ["Gemfile"]
        assert 'rake2' == generator.lifecycle_key
        generator.loadYaml("language: java")
        generator.folder_listing = []
        assert 'ant' == generator.lifecycle_key
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_false() {
        generator.loadYaml("language: ruby\nenv: foo=bar")
        assert false == generator.isMatrixBuild()
        generator.loadYaml("language: ruby\nenv:\n  - foo=bar")
        assert false == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_true() {
        generator.loadYaml("language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar")
        assert true == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_generateToolchainSection_nonmatrix() {
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
