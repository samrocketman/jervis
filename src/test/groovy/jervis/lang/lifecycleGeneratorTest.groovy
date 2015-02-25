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
        generator = null
        generator = new lifecycleGenerator()
        assert generator.toolchain_obj == null
        url = this.getClass().getResource('/bad_toolchains_missing_default_ivalue.json');
        shouldFail(JervisException) {
            generator.loadToolchains(url.getFile())
        }
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
        generator = null
        generator = new lifecycleGenerator()
        assert generator.toolchain_obj == null
        url = this.getClass().getResource('/bad_toolchains_missing_default_ivalue.json');
        contents = new File(url.getFile()).getText()
        shouldFail(JervisException) {
            generator.loadToolchainsString(contents)
        }
    }
    @Test public void test_lifecycleGenerator_loadYaml_exceptions() {
        generator = new lifecycleGenerator()
        shouldFail(JervisException) {
            generator.loadYamlString('language: ruby')
        }
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        shouldFail(JervisException) {
            generator.loadYamlString('language: ruby')
        }
        url = this.getClass().getResource('/good_toolchains_simple.json');
        generator.loadToolchains(url.getFile())
        generator.loadYamlString('language: ruby')
        assert 'ruby' == generator.yaml_language
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
        generator.loadYamlString('language: ruby\nenv:\n  matrix:\n    - foo=bar')
        assert false == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_isMatrixBuild_true() {
        generator.loadYamlString('language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar')
        assert true == generator.isMatrixBuild()
        generator.loadYamlString('language: ruby\nenv:\n  matrix:\n    - foobar=foo\n    - foobar=bar')
        assert true == generator.isMatrixBuild()
    }
    @Test public void test_lifecycleGenerator_matrixExcludeFilter() {
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  exclude:\n    - env: world=goodbye\n      rvm: "2.1"')
        assert '!(env == \'env1\' && rvm == \'rvm2\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - env: world=goodbye\n      rvm: "2.1"')
        assert '(env == \'env1\' && rvm == \'rvm2\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\njdk: openjdk6\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - env: world=goodbye\n      rvm: "2.1"\n    - jdk: openjdk6')
        assert '(env == \'env1\' && rvm == \'rvm2\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  exclude:\n    - env: world=goodbye\n      rvm: "2.1"\n    - env: world=hello\n      rvm: 1.9.3')
        assert '!(env == \'env1\' && rvm == \'rvm2\') && !(env == \'env0\' && rvm == \'rvm0\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - env: world=goodbye\n      rvm: "2.1"\n    - env: world=hello\n      rvm: 1.9.3')
        assert '((env == \'env1\' && rvm == \'rvm2\') || (env == \'env0\' && rvm == \'rvm0\'))' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  exclude:\n    - env: world=hello\n      rvm: 1.9.3\n  include:\n    - rvm: "1.9.3"\n    - rvm: "2.1"')
        assert '!(env == \'env0\' && rvm == \'rvm0\') && ((rvm == \'rvm0\') || (rvm == \'rvm2\'))' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - env: world=hello\n      rvm: 1.9.3\n  exclude:\n    - rvm: "1.9.3"\n    - rvm: "2.1"')
        assert '!(rvm == \'rvm0\') && !(rvm == \'rvm2\') && (env == \'env0\' && rvm == \'rvm0\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv:\n  matrix: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  exclude:\n    - env: world=hello\n      rvm: 1.9.3\n  include:\n    - rvm: "1.9.3"\n    - rvm: "2.1"')
        assert '!(env == \'env0\' && rvm == \'rvm0\') && ((rvm == \'rvm0\') || (rvm == \'rvm2\'))' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv:\n  matrix: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - env: world=hello\n      rvm: 1.9.3\n  exclude:\n    - rvm: "1.9.3"\n    - rvm: "2.1"')
        assert '!(rvm == \'rvm0\') && !(rvm == \'rvm2\') && (env == \'env0\' && rvm == \'rvm0\')' == generator.matrixExcludeFilter()
        generator.loadYamlString('language: ruby\nenv:\n  matrix: [world=hello, world=goodbye]\nrvm: ["1.9.3", "2.0.0", "2.1"]\nmatrix:\n  include:\n    - rvm: 1.9.3\n      env: world=hello\n  exclude:\n    - rvm: "1.9.3"\n    - rvm: "2.1"')
        assert '!(rvm == \'rvm0\') && !(rvm == \'rvm2\') && (rvm == \'rvm0\' && env == \'env0\')' == generator.matrixExcludeFilter()
    }
    @Test public void test_lifecycleGenerator_matrixGetAxisValue() {
        generator.loadYamlString('language: ruby\nenv:\n  - foobar=foo\n  - foobar=bar')
        assert 'env0 env1' == generator.matrixGetAxisValue('env')
        assert '' == generator.matrixGetAxisValue('rvm')
        generator.loadYamlString('language: ruby\nenv:\n  matrix:\n    - foobar=foo\n    - foobar=bar')
        assert 'env0 env1' == generator.matrixGetAxisValue('env')
    }
    @Test public void test_lifecycleGenerator_generateToolchainSection_matrix() {
        //basic env matrix
        generator.loadYamlString('language: ruby\nenv: [world=hello, world=goodbye]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\ncase ${env} in\n  env0)\n    export world=hello\n    ;;\n  env1)\n    export world=goodbye\n    ;;\nesac\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //using jdk as matrix
        generator.loadYamlString('language: ruby\njdk: [openjdk6, openjdk7]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\ncase ${jdk} in\n  jdk0)\n    more commands\n    ;;\n  jdk1)\n    some commands\n    ;;\nesac\n' == generator.generateToolchainSection()
        //advanced env section which has a matrix section
        generator.loadYamlString('language: ruby\nenv:\n  matrix: [world=hello, world=goodbye]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\ncase ${env} in\n  env0)\n    export world=hello\n    ;;\n  env1)\n    export world=goodbye\n    ;;\nesac\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  matrix: {hello: three}')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
        ///advanced env section which has a matrix and global section
        generator.loadYamlString('language: ruby\nenv:\n  global: foobar=foo\n  matrix: [world=hello, world=goodbye]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foobar=foo\ncase ${env} in\n  env0)\n    export world=hello\n    ;;\n  env1)\n    export world=goodbye\n    ;;\nesac\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  global: [foobar=foo, foobar=bar]\n  matrix: [world=hello, world=goodbye]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foobar=foo\nexport foobar=bar\ncase ${env} in\n  env0)\n    export world=hello\n    ;;\n  env1)\n    export world=goodbye\n    ;;\nesac\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  global: {hello: three}')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
        //check for bad value in jdk matrix
        generator.loadYamlString('language: ruby\njdk: [openjdk6, openjdk7, derp]')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
    }
    @Test public void test_lifecycleGenerator_generateToolchainSection_nonmatrix() {
        //basic language test
        generator.loadYamlString('language: ruby')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //configuring basic env toolchain
        generator.loadYamlString('language: ruby\nenv: foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //test for env toolchain as an ArrayList
        generator.loadYamlString('language: ruby\nenv:\n  - foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //configuring basic jdk toolchain
        generator.loadYamlString('language: ruby\njdk: openjdk7')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //testing env global value for non-matrix configuration
        generator.loadYamlString('language: ruby\nenv:\n  global: foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  global: [foo=bar]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //testing for env global throwing an exception for non-matrix configuration
        generator.loadYamlString('language: ruby\nenv:\n  global: {hello: three}')
        shouldFail(UnsupportedToolException) {
            generator.generateToolchainSection()
        }
        //testing env matrix value for non-matrix configuration
        generator.loadYamlString('language: ruby\nenv:\n  matrix: foo=bar')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        generator.loadYamlString('language: ruby\nenv:\n  matrix: [foo=bar]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\nexport foo=bar\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n' == generator.generateToolchainSection()
        //testing for exceptions
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
    @Test public void test_lifecycleGenerator_boolean_bug() {
        generator.loadYamlString('language: ruby\ninstall: true')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n\n#\n# INSTALL SECTION\n#\ntrue\n\n#\n# SCRIPT SECTION\n#\nbundle exec rake\n' == generator.generateAll()
        generator.loadYamlString('language: ruby\ninstall: [true,true]')
        assert '#\n# TOOLCHAINS SECTION\n#\n#gemfile toolchain section\nexport BUNDLE_GEMFILE="${PWD}/Gemfile"\n#env toolchain section\n#rvm toolchain section\nsome commands\n#jdk toolchain section\nsome commands\n\n#\n# INSTALL SECTION\n#\ntrue\ntrue\n\n#\n# SCRIPT SECTION\n#\nbundle exec rake\n' == generator.generateAll()
    }
    @Test public void test_lifecycleGenerator_isGenerateBranch() {

        generator.loadYamlString('language: ruby\nbranches:\n  only:\n    - master')
        assert true == generator.isGenerateBranch('master')
    }
}
