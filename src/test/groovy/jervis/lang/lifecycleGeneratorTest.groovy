package jervis.lang
//the lifecycleGeneratorTest() class automatically sees the lifecycleGenerator() class because they're in the same package
import jervis.exceptions.JervisException
import jervis.exceptions.UnsupportedLanguageException
import jervis.exceptions.UnsupportedToolException
import jervis.lang.lifecycleValidator
import jervis.lang.toolchainValidator
import org.junit.After
import org.junit.Before
import org.junit.Test

class lifecycleGeneratorTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new lifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_simple.json');
        generator.loadToolchains(url.getFile())
    }
    //tear down after every test
    @After protected void tearDown() {
        generator = null
        super.tearDown()
    }
    @Test public void test_lifecycleGenerator_loadLifecycles() {
        generator = null
        generator = new lifecycleGenerator()
        assert generator.lifecycle_obj == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        assert generator.lifecycle_obj != null
        assert generator.lifecycle_obj.class == lifecycleValidator
        assert generator.lifecycle_obj.lifecycles['groovy']['friendlyName'] == 'Groovy'
        generator = null
        generator = new lifecycleGenerator()
        assert generator.lifecycle_obj == null
        url = this.getClass().getResource('/bad_lifecycles_missing_defaultKey.json');
        shouldFail(JervisException) {
            generator.loadLifecycles(url.getFile())
        }
    }
    @Test public void test_lifecycleGenerator_loadLifecyclesString() {
        generator = null
        generator = new lifecycleGenerator()
        assert generator.lifecycle_obj == null
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        String contents = new File(url.getFile()).getText()
        generator.loadLifecyclesString(contents)
        assert generator.lifecycle_obj != null
        assert generator.lifecycle_obj.class == lifecycleValidator
        assert generator.lifecycle_obj.lifecycles['groovy']['friendlyName'] == 'Groovy'
        generator = null
        generator = new lifecycleGenerator()
        assert generator.lifecycle_obj == null
        url = this.getClass().getResource('/bad_lifecycles_missing_defaultKey.json');
        contents = new File(url.getFile()).getText()
        shouldFail(JervisException) {
            generator.loadLifecyclesString(contents)
        }
    }
    @Test public void test_lifecycleGenerator_loadToolchains() {
        generator = null
        generator = new lifecycleGenerator()
        assert generator.toolchain_obj == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        generator.loadToolchains(url.getFile())
        assert generator.toolchain_obj != null
        assert generator.toolchain_obj.class == toolchainValidator
        assert generator.toolchain_obj.toolchains['jdk']['default_ivalue'] == 'openjdk7'
    }
    @Test public void test_lifecycleGenerator_loadToolchainsString() {
        generator = null
        generator = new lifecycleGenerator()
        assert generator.toolchain_obj == null
        URL url = this.getClass().getResource('/good_toolchains_simple.json');
        String contents = new File(url.getFile()).getText()
        generator.loadToolchainsString(contents)
        assert generator.toolchain_obj != null
        assert generator.toolchain_obj.class == toolchainValidator
        assert generator.toolchain_obj.toolchains['jdk']['default_ivalue'] == 'openjdk7'
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_yes() {
        generator.loadYamlString('language: ruby')
        assert 'ruby' == generator.yaml_language
    }
    @Test public void test_lifecycleGenerator_loadYaml_supportedLanguage_no() {
        //not in lifecycles and not in toolchains
        shouldFail(UnsupportedLanguageException) {
            generator.loadYamlString('language: derp')
        }
        //in lifecycles but not in toolchains
        shouldFail(UnsupportedLanguageException) {
            generator.loadYamlString('language: groovy')
        }
    }
    @Test public void test_lifecycleGenerator_setfolder_listing() {
        shouldFail(JervisException) {
            generator.folder_listing = ['Gemfile.lock', 'Gemfile']
        }
        generator.loadYamlString('language: ruby')
        generator.folder_listing = ['Gemfile.lock', 'Gemfile']
        assert 'rake1' == generator.lifecycle_key
        generator.folder_listing = ['Gemfile']
        assert 'rake2' == generator.lifecycle_key
        generator.loadYamlString('language: java')
        generator.folder_listing = []
        assert 'ant' == generator.lifecycle_key
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_false() {
        generator.loadYamlString('language: ruby\nenv: foo=bar')
        assert false == generator.isMatrixBuild()
        generator.loadYamlString('language: ruby\nenv:\n  - foo=bar')
        assert false == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_true() {
        generator.loadYamlString('language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar')
        assert true == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_matrixExcludeFilter() {
        generator.loadYamlString('language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar')
    }
    @Test public void test_lifecycleGenerator_matrixGetAxisValue() {
        generator.loadYamlString('language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar')
        assert '0 1' == generator.matrixGetAxisValue('env')
        assert '' == generator.matrixGetAxisValue('rvm')
    }
    @Test public void test_lifecycleGenerator_generateToolchainSection_matrix() {
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\ncase ${env} in\n  0)\n    export world=hello\n    ;;\n  1)\n    export world=goodbye\n    ;;\nesac\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\njdk: [openjdk6, openjdk7]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\ncase ${jdk} in\n  0)\n    more commands\n    ;;\n  1)\n    some commands\n    ;;\nesac\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\njdk: [openjdk6, openjdk7, derp]')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
    }
    @Test public void test_lifecycleGenerator_generateToolchainSection_nonmatrix() {
        generator.loadYamlString('language: ruby')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv: foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  - foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\njdk: openjdk7')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\njdk: derp')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
        generator.loadYamlString('language: ruby\njdk: 2.5')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
    }
    @Test public void test_lifecycleGenerator_generateBeforeInstall() {
        generator.loadYamlString('language: ruby')
        assert '' == generator.generateBeforeInstall()
        generator.loadYamlString('language: ruby\nbefore_install: some code')
        assert '#\n# BEFORE_INSTALL SECTION\n#\nsome code\n' == generator.generateBeforeInstall()
        generator.loadYamlString('language: ruby\nbefore_install:\n - some code\n - more code')
        assert '#\n# BEFORE_INSTALL SECTION\n#\nsome code\nmore code\n' == generator.generateBeforeInstall()
    }
    @Test public void test_lifecycleGenerator_generateInstall() {
        generator.loadYamlString('language: ruby')
        assert '#\n# INSTALL SECTION\n#\nbundle install --jobs=3 --retry=3\n' == generator.generateInstall()
        generator.loadYamlString('language: ruby\ninstall: some code')
        assert '#\n# INSTALL SECTION\n#\nsome code\n' == generator.generateInstall()
        generator.loadYamlString('language: ruby\ninstall:\n - some code\n - more code')
        assert '#\n# INSTALL SECTION\n#\nsome code\nmore code\n' == generator.generateInstall()
    }
    @Test public void test_lifecycleGenerator_generateBeforeScript() {
        generator.loadYamlString('language: ruby')
        assert '' == generator.generateBeforeScript()
        generator.loadYamlString('language: ruby\nbefore_script: some code')
        assert '#\n# BEFORE_SCRIPT SECTION\n#\nsome code\n' == generator.generateBeforeScript()
        generator.loadYamlString('language: ruby\nbefore_script:\n - some code\n - more code')
        assert '#\n# BEFORE_SCRIPT SECTION\n#\nsome code\nmore code\n' == generator.generateBeforeScript()
    }
    @Test public void test_lifecycleGenerator_generateScript() {
        generator.loadYamlString('language: ruby')
        assert '#\n# SCRIPT SECTION\n#\nbundle exec rake\n' == generator.generateScript()
        generator.loadYamlString('language: ruby\nscript: some code')
        assert '#\n# SCRIPT SECTION\n#\nsome code\n' == generator.generateScript()
        generator.loadYamlString('language: ruby\nscript:\n - some code\n - more code')
        assert '#\n# SCRIPT SECTION\n#\nsome code\nmore code\n' == generator.generateScript()
    }
    @Test public void test_lifecycleGenerator_generateAfterSuccess() {
        generator.loadYamlString('language: ruby')
        assert '' == generator.generateAfterSuccess()
        generator.loadYamlString('language: ruby\nafter_success: some code')
        assert '#\n# AFTER_SUCCESS SECTION\n#\nsome code\n' == generator.generateAfterSuccess()
        generator.loadYamlString('language: ruby\nafter_success:\n - some code\n - more code')
        assert '#\n# AFTER_SUCCESS SECTION\n#\nsome code\nmore code\n' == generator.generateAfterSuccess()
    }
    @Test public void test_lifecycleGenerator_generateAfterFailure() {
        generator.loadYamlString('language: ruby')
        assert '' == generator.generateAfterFailure()
        generator.loadYamlString('language: ruby\nafter_failure: some code')
        assert '#\n# AFTER_FAILURE SECTION\n#\nsome code\n' == generator.generateAfterFailure()
        generator.loadYamlString('language: ruby\nafter_failure:\n - some code\n - more code')
        assert '#\n# AFTER_FAILURE SECTION\n#\nsome code\nmore code\n' == generator.generateAfterFailure()
    }
    @Test public void test_lifecycleGenerator_generateAfterScript() {
        generator.loadYamlString('language: ruby')
        assert '' == generator.generateAfterScript()
        generator.loadYamlString('language: ruby\nafter_script: some code')
        assert '#\n# AFTER_SCRIPT SECTION\n#\nsome code\n' == generator.generateAfterScript()
        generator.loadYamlString('language: ruby\nafter_script:\n - some code\n - more code')
        assert '#\n# AFTER_SCRIPT SECTION\n#\nsome code\nmore code\n' == generator.generateAfterScript()
    }
    @Test public void test_lifecycleGenerator_generateAll() {
        generator.loadYamlString('language: ruby')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n\n#\n# INSTALL SECTION\n#\nbundle install --jobs=3 --retry=3\n\n#\n# SCRIPT SECTION\n#\nbundle exec rake\n' == generator.generateAll()
    }
}
