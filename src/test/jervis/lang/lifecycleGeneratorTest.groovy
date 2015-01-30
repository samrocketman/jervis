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
    @Test public void test_lifecycleGenerator_generateBeforeInstall() {
        generator.loadYaml("language: ruby")
        assert "" == generator.generateBeforeInstall()
        generator.loadYaml("language: ruby\nbefore_install: some code")
        assert "#\n# BEFORE_INSTALL SECTION\n#\nsome code\n" == generator.generateBeforeInstall()
        generator.loadYaml("language: ruby\nbefore_install:\n - some code\n - more code")
        assert "#\n# BEFORE_INSTALL SECTION\n#\nsome code\nmore code\n" == generator.generateBeforeInstall()
    }
    @Test public void test_lifecycleGenerator_generateInstall() {
        generator.loadYaml("language: ruby")
        assert "#\n# INSTALL SECTION\n#\nbundle install --jobs=3 --retry=3\n" == generator.generateInstall()
        generator.loadYaml("language: ruby\ninstall: some code")
        assert "#\n# INSTALL SECTION\n#\nsome code\n" == generator.generateInstall()
        generator.loadYaml("language: ruby\ninstall:\n - some code\n - more code")
        assert "#\n# INSTALL SECTION\n#\nsome code\nmore code\n" == generator.generateInstall()
    }
    @Test public void test_lifecycleGenerator_generateBeforeScript() {
        generator.loadYaml("language: ruby")
        assert "" == generator.generateBeforeScript()
        generator.loadYaml("language: ruby\nbefore_script: some code")
        assert "#\n# BEFORE_SCRIPT SECTION\n#\nsome code\n" == generator.generateBeforeScript()
        generator.loadYaml("language: ruby\nbefore_script:\n - some code\n - more code")
        assert "#\n# BEFORE_SCRIPT SECTION\n#\nsome code\nmore code\n" == generator.generateBeforeScript()
    }
    @Test public void test_lifecycleGenerator_generateScript() {
        generator.loadYaml("language: ruby")
        assert "#\n# SCRIPT SECTION\n#\nbundle exec rake\n" == generator.generateScript()
        generator.loadYaml("language: ruby\nscript: some code")
        assert "#\n# SCRIPT SECTION\n#\nsome code\n" == generator.generateScript()
        generator.loadYaml("language: ruby\nscript:\n - some code\n - more code")
        assert "#\n# SCRIPT SECTION\n#\nsome code\nmore code\n" == generator.generateScript()
    }
    @Test public void test_lifecycleGenerator_generateAfterSuccess() {
        generator.loadYaml("language: ruby")
        assert "" == generator.generateAfterSuccess()
        generator.loadYaml("language: ruby\nafter_success: some code")
        assert "#\n# AFTER_SUCCESS SECTION\n#\nsome code\n" == generator.generateAfterSuccess()
        generator.loadYaml("language: ruby\nafter_success:\n - some code\n - more code")
        assert "#\n# AFTER_SUCCESS SECTION\n#\nsome code\nmore code\n" == generator.generateAfterSuccess()
    }
    @Test public void test_lifecycleGenerator_generateAfterFailure() {
        generator.loadYaml("language: ruby")
        assert "" == generator.generateAfterFailure()
        generator.loadYaml("language: ruby\nafter_failure: some code")
        assert "#\n# AFTER_FAILURE SECTION\n#\nsome code\n" == generator.generateAfterFailure()
        generator.loadYaml("language: ruby\nafter_failure:\n - some code\n - more code")
        assert "#\n# AFTER_FAILURE SECTION\n#\nsome code\nmore code\n" == generator.generateAfterFailure()
    }
    @Test public void test_lifecycleGenerator_generateAfterScript() {
        generator.loadYaml("language: ruby")
        assert "" == generator.generateAfterScript()
        generator.loadYaml("language: ruby\nafter_script: some code")
        assert "#\n# AFTER_SCRIPT SECTION\n#\nsome code\n" == generator.generateAfterScript()
        generator.loadYaml("language: ruby\nafter_script:\n - some code\n - more code")
        assert "#\n# AFTER_SCRIPT SECTION\n#\nsome code\nmore code\n" == generator.generateAfterScript()
    }
}
