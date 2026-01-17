/*
   Copyright 2014-2026 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.lang
//the MultiPlatformGeneratorTest() class automatically sees the MultiPlatformGenerator() class because they're in the same package
import net.gleske.jervis.exceptions.MultiPlatformJervisYamlException

import org.junit.After
import org.junit.Before
import org.junit.Test

class MultiPlatformGeneratorTest extends GroovyTestCase {
    def platforms
    //helper method to load simple test resources for isolated legacy tests
    private LifecycleGenerator loadSimpleResources() {
        def generator = new LifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_simple.json')
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_simple.json')
        generator.loadToolchains(url.getFile())
        return generator
    }
    //helper method to load simple test resources with platforms for isolated legacy tests
    private LifecycleGenerator loadSimpleResourcesWithPlatforms() {
        def generator = loadSimpleResources()
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        generator.loadPlatformsFile(url.getFile())
        return generator
    }
    //set up before every test - load multi-platform resources as the default
    @Before protected void setUp() {
        super.setUp()
        platforms = new MultiPlatformValidator()
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mptlifecycles-alpine3-stable.yaml')
        platforms.loadLifecyclesString('alpine3', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-alpine3-stable.yaml')
        platforms.loadToolchainsString('alpine3', url.content.text)
    }
    //tear down after every test
    @After protected void tearDown() {
        platforms = null
        super.tearDown()
    }
    //tests using multi-platform resources (default setUp)
    @Test public void test_MultiPlatformGenerator_multiplatform_basic() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.defaultPlatform == 'x86_64'
        assert mpg.defaultOS == 'ubuntu2204'
        assert mpg.platforms == ['x86_64', 'amd64', 'arm64']
        assert mpg.operating_systems == ['alpine3', 'ubuntu2204']
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_isMatrixBuild() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getBuildableMatrixAxes() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 2 operating systems = 4 combinations
        assert axes.size() == 4
        assert axes.any { it.platform == 'x86_64' && it.os == 'ubuntu2204' }
        assert axes.any { it.platform == 'x86_64' && it.os == 'alpine3' }
        assert axes.any { it.platform == 'arm64' && it.os == 'ubuntu2204' }
        assert axes.any { it.platform == 'arm64' && it.os == 'alpine3' }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getDefaultToolchainsEnvironment() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        Map env = mpg.getDefaultToolchainsEnvironment()
        // Default should be the first of each axis
        assert env.platform == 'x86_64'
        assert env.os == 'ubuntu2204'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_generateToolchainSection() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        String script = mpg.generateToolchainSection()
        // Should contain if/elif conditions for platform and OS
        assert script.contains('if [')
        assert script.contains('elif [')
        assert script.contains('${platform}')
        assert script.contains('${os}')
        assert script.contains("'x86_64'")
        assert script.contains("'arm64'")
        assert script.contains("'ubuntu2204'")
        assert script.contains("'alpine3'")
        assert script.contains('fi')
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_platform_generators() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // Should have generators for each platform/OS combination
        assert mpg.platform_generators.containsKey('x86_64')
        assert mpg.platform_generators.containsKey('arm64')
        assert mpg.platform_generators['x86_64'].containsKey('ubuntu2204')
        assert mpg.platform_generators['x86_64'].containsKey('alpine3')
        assert mpg.platform_generators['arm64'].containsKey('ubuntu2204')
        assert mpg.platform_generators['arm64'].containsKey('alpine3')

        // Each should be a LifecycleGenerator
        assert mpg.platform_generators['x86_64']['ubuntu2204'] instanceof LifecycleGenerator
        assert mpg.platform_generators['arm64']['alpine3'] instanceof LifecycleGenerator
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_platform_jervis_yaml() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // Each platform/OS should have its own YAML
        assert mpg.platform_jervis_yaml['x86_64']['ubuntu2204'].language == 'shell'
        assert mpg.platform_jervis_yaml['x86_64']['alpine3'].language == 'shell'
        assert mpg.platform_jervis_yaml['arm64']['ubuntu2204'].language == 'shell'
        assert mpg.platform_jervis_yaml['arm64']['alpine3'].language == 'shell'

        // Each platform YAML should have the specific platform and OS set
        assert mpg.platform_jervis_yaml['x86_64']['ubuntu2204'].jenkins.platform == 'x86_64'
        assert mpg.platform_jervis_yaml['x86_64']['ubuntu2204'].jenkins.os == 'ubuntu2204'
        assert mpg.platform_jervis_yaml['arm64']['alpine3'].jenkins.platform == 'arm64'
        assert mpg.platform_jervis_yaml['arm64']['alpine3'].jenkins.os == 'alpine3'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_rawJervisYaml() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // rawJervisYaml should preserve the original multi-platform YAML
        assert mpg.rawJervisYaml.language == 'shell'
        assert mpg.rawJervisYaml.script == '/bin/true'
        assert mpg.rawJervisYaml.jenkins.platform == ['x86_64', 'arm64']
        assert mpg.rawJervisYaml.jenkins.os == ['ubuntu2204', 'alpine3']
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getJervisYaml() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // getJervisYaml() returns the YAML for the default platform/OS
        Map jervisYaml = mpg.getJervisYaml()
        assert jervisYaml.language == 'shell'
        assert jervisYaml.jenkins.platform == 'x86_64'
        assert jervisYaml.jenkins.os == 'ubuntu2204'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getGenerator() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // getGenerator() returns the LifecycleGenerator for default platform/OS
        LifecycleGenerator gen = mpg.getGenerator()
        assert gen instanceof LifecycleGenerator
        assert gen.yaml_language == 'shell'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_single_platform_multiple_os() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 1 platform (default) x 2 OS = 2 combinations
        assert axes.size() == 2
        assert axes.any { it.os == 'ubuntu2204' }
        assert axes.any { it.os == 'alpine3' }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_multiple_platform_single_os() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 1 OS (default) = 2 combinations
        assert axes.size() == 2
        assert axes.any { it.platform == 'x86_64' }
        assert axes.any { it.platform == 'arm64' }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_with_toolchain_matrix() {
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk11
            |  - openjdk17
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 2 OS x 2 JDKs = 8 combinations
        assert axes.size() == 8
        // Check that JDK axes are included (using friendly labels jdk:openjdk11, jdk:openjdk17)
        assert axes.every { it.containsKey('jdk') }
        assert axes.any { it.jdk == 'jdk:openjdk11' && it.platform == 'x86_64' && it.os == 'ubuntu2204' }
        assert axes.any { it.jdk == 'jdk:openjdk17' && it.platform == 'arm64' && it.os == 'alpine3' }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_nonmatrix() {
        String yaml = '''
            |language: shell
            |script: /bin/true
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        // Single platform and OS - not a matrix build
        assert !mpg.isMatrixBuild()
        assert mpg.getBuildableMatrixAxes() == []
        assert mpg.defaultPlatform == 'x86_64'
        assert mpg.defaultOS == 'ubuntu2204'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_validate_invalid_platform() {
        String yaml = '''
            |language: shell
            |jenkins:
            |  platform:
            |    - invalid_platform
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        shouldFail(MultiPlatformJervisYamlException) {
            mpg.loadMultiPlatformYaml(yaml: yaml)
        }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_validate_invalid_os() {
        String yaml = '''
            |language: shell
            |jenkins:
            |  os:
            |    - invalid_os
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        shouldFail(MultiPlatformJervisYamlException) {
            mpg.loadMultiPlatformYaml(yaml: yaml)
        }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_validate_invalid_language() {
        String yaml = '''
            |language: invalid_language
            |jenkins:
            |  platform:
            |    - x86_64
            |  os:
            |    - ubuntu2204
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        shouldFail(MultiPlatformJervisYamlException) {
            mpg.loadMultiPlatformYaml(yaml: yaml)
        }
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getStashes() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
            |  stash:
            |    - name: myartifacts
            |      includes: 'build/**/*'
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        List stashes = mpg.getStashes()
        assert stashes.size() == 1
        assert stashes[0].name == 'myartifacts'
        assert stashes[0].includes == 'build/**/*'
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_serialization() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(mpg)
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_getJervisYamlString() {
        String yaml = '''
            |language: shell
            |script: /bin/true
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        String yamlString = mpg.getJervisYamlString()
        assert yamlString.contains('language: shell')
        assert yamlString.contains('script: /bin/true')
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_java_language() {
        String yaml = '''
            |language: java
            |jdk: openjdk11
            |jenkins:
            |  platform:
            |    - x86_64
            |  os:
            |    - ubuntu2204
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.getGenerator().yaml_language == 'java'
        assert !mpg.isMatrixBuild()
    }
    @Test public void test_MultiPlatformGenerator_multiplatform_python_language() {
        String yaml = '''
            |language: python
            |python: '3.10'
            |jenkins:
            |  platform:
            |    - x86_64
            |  os:
            |    - ubuntu2204
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.getGenerator().yaml_language == 'python'
        assert !mpg.isMatrixBuild()
    }
    //advanced env tests
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_isMatrixBuild() {
        // Advanced env with global and matrix - should be a matrix build
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_getBuildableMatrixAxes() {
        // Advanced env with global and matrix - should have 2 matrix axes
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        List axes = mpg.getBuildableMatrixAxes()
        assert axes.size() == 2
        // Should have env0 and env1 for the matrix values
        assert axes.any { it.env == 'env0' }
        assert axes.any { it.env == 'env1' }
        // All axes should have default platform and OS
        assert axes.every { it.platform == 'x86_64' && it.os == 'ubuntu2204' }
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_getDefaultToolchainsEnvironment() {
        // Advanced env with global and matrix - default environment should be first matrix axis
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        Map defaultEnv = mpg.getDefaultToolchainsEnvironment()
        assert defaultEnv.env == 'env0'
        assert defaultEnv.platform == 'x86_64'
        assert defaultEnv.os == 'ubuntu2204'
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_generateToolchainSection() {
        // Advanced env with global and matrix - should generate proper toolchain script
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        String script = mpg.generateToolchainSection()
        // Should contain the global export
        assert script.contains('export foo=bar')
        // Should contain the case statement for matrix
        assert script.contains('case ${env} in')
        assert script.contains('env0)')
        assert script.contains('export bax=bay')
        assert script.contains('env1)')
        assert script.contains('export bax=baz')
        assert script.contains('esac')
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_with_platform_matrix() {
        // Advanced env with global/matrix combined with platform matrix
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 2 env matrix = 4 combinations
        assert axes.size() == 4
        assert axes.any { it.platform == 'x86_64' && it.env == 'env0' }
        assert axes.any { it.platform == 'x86_64' && it.env == 'env1' }
        assert axes.any { it.platform == 'arm64' && it.env == 'env0' }
        assert axes.any { it.platform == 'arm64' && it.env == 'env1' }
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_with_platform_os_matrix() {
        // Advanced env with global/matrix combined with platform and OS matrix
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 2 OS x 2 env matrix = 8 combinations
        assert axes.size() == 8

        // Verify all combinations exist
        ['x86_64', 'arm64'].each { platform ->
            ['ubuntu2204', 'alpine3'].each { os ->
                ['env0', 'env1'].each { env ->
                    assert axes.any { it.platform == platform && it.os == os && it.env == env } :
                        "Missing combination: platform=${platform}, os=${os}, env=${env}"
                }
            }
        }
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_only_global_not_matrix() {
        // Only global env (no matrix key) - should NOT be a matrix build
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |    - hello=world
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert !mpg.isMatrixBuild()
        assert mpg.getBuildableMatrixAxes() == []

        String script = mpg.generateToolchainSection()
        assert script.contains('export foo=bar')
        assert script.contains('export hello=world')
        // Should NOT contain case statement since no matrix
        assert !script.contains('case ${env} in')
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_only_matrix_list() {
        // Only matrix key (no global) - should be a matrix build
        String yaml = '''
            |language: shell
            |env:
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        assert axes.size() == 2
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_matrix_single_value_not_matrix() {
        // Single matrix value - should NOT be a matrix build
        String yaml = '''
            |language: shell
            |env:
            |  matrix:
            |    - bax=bay
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert !mpg.isMatrixBuild()
        assert mpg.getBuildableMatrixAxes() == []
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_list_matrix_list() {
        // Multiple globals with multiple matrix - all globals should be exported
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |    - hello=world
            |    - test=value
            |  matrix:
            |    - bax=bay
            |    - bax=baz
            |    - bax=boo
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        assert axes.size() == 3
        assert axes.any { it.env == 'env0' }
        assert axes.any { it.env == 'env1' }
        assert axes.any { it.env == 'env2' }

        String script = mpg.generateToolchainSection()
        // All globals should be exported
        assert script.contains('export foo=bar')
        assert script.contains('export hello=world')
        assert script.contains('export test=value')
        // Matrix case statement
        assert script.contains('case ${env} in')
        assert script.contains('env0)')
        assert script.contains('export bax=bay')
        assert script.contains('env1)')
        assert script.contains('export bax=baz')
        assert script.contains('env2)')
        assert script.contains('export bax=boo')
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_global_matrix_verify_all_axes() {
        // Comprehensive test to verify all matrix axes are properly constructed
        String yaml = '''
            |language: shell
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        List axes = mpg.getBuildableMatrixAxes()

        // Test iterating across all matrix axes (similar to LifecycleGeneratorTest pattern)
        axes.each { Map axis ->
            // Each axis should have platform, os, and env
            assert axis.containsKey('platform')
            assert axis.containsKey('os')
            assert axis.containsKey('env')

            // Platform should be one of the specified values
            assert axis.platform in ['x86_64', 'arm64']
            // OS should be one of the specified values
            assert axis.os in ['ubuntu2204', 'alpine3']
            // Env should be env0 or env1
            assert axis.env in ['env0', 'env1']
        }

        // Verify the first axis is the default
        Map firstAxis = axes[0]
        Map defaultEnv = mpg.getDefaultToolchainsEnvironment()
        assert firstAxis == defaultEnv
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_with_jdk_matrix() {
        // Advanced env combined with JDK matrix
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk11
            |  - openjdk17
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 JDKs x 2 env matrix = 4 combinations
        assert axes.size() == 4

        // Verify all combinations
        assert axes.any { it.jdk == 'jdk:openjdk11' && it.env == 'env0' }
        assert axes.any { it.jdk == 'jdk:openjdk11' && it.env == 'env1' }
        assert axes.any { it.jdk == 'jdk:openjdk17' && it.env == 'env0' }
        assert axes.any { it.jdk == 'jdk:openjdk17' && it.env == 'env1' }
    }
    @Test public void test_MultiPlatformGenerator_advanced_env_full_matrix_all_axes() {
        // Full matrix: platform x OS x JDK x env matrix
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk11
            |  - openjdk17
            |env:
            |  global:
            |    - foo=bar
            |  matrix:
            |    - bax=bay
            |    - bax=baz
            |jenkins:
            |  platform:
            |    - x86_64
            |    - arm64
            |  os:
            |    - ubuntu2204
            |    - alpine3
        '''.stripMargin().trim()
        def mpg = new MultiPlatformGenerator(platforms)
        mpg.loadMultiPlatformYaml(yaml: yaml)

        assert mpg.isMatrixBuild()
        List axes = mpg.getBuildableMatrixAxes()
        // 2 platforms x 2 OS x 2 JDKs x 2 env = 16 combinations
        assert axes.size() == 16

        // Verify comprehensive iteration across all axes
        int count = 0
        ['x86_64', 'arm64'].each { platform ->
            ['ubuntu2204', 'alpine3'].each { os ->
                ['jdk:openjdk11', 'jdk:openjdk17'].each { jdk ->
                    ['env0', 'env1'].each { env ->
                        assert axes.any {
                            it.platform == platform && it.os == os && it.jdk == jdk && it.env == env
                        } : "Missing combination: platform=${platform}, os=${os}, jdk=${jdk}, env=${env}"
                        count++
                    }
                }
            }
        }
        assert count == 16
    }
    //isolated tests using simple resources (legacy tests adapted)
    @Test public void test_MultiPlatformGenerator_serialization() {
        def generator = loadSimpleResourcesWithPlatforms()
        generator.loadYamlString('language: ruby')
        def pipeline = new MultiPlatformGenerator(generator)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(pipeline)
    }
    @Test public void test_MultiPlatformGenerator_getSecretPairsEnv() {
        def generator = loadSimpleResources()
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        URL file_url = this.getClass().getResource('/rsa_keys/rsa_secure_properties_map_test.yml')
        generator.loadYamlString(file_url.content.text)
        generator.setPrivateKey(url.content.text)
        generator.decryptSecrets()
        def pipeline_generator = new MultiPlatformGenerator(generator)
        // Access secret pairs through the underlying generator
        def results = [[], []]
        pipeline_generator.getGenerator().plainmap.each { k, v ->
            results[0] << [var: k, password: v]
            results[1] << "${k}=${v}"
        }
        assert results[0] == [[var: 'JERVIS_SECRETS_TEST', password: 'plaintext']]
        assert results[1] == ['JERVIS_SECRETS_TEST=plaintext']
    }
    @Test public void test_MultiPlatformGenerator_getBuildableMatrixAxes_matrix() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodby"]\njdk:\n  - openjdk6\n  - openjdk7')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[platform: 'none', os: 'none', env:'env0', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env0', jdk:'jdk1'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk1']]
        //account for matrix include axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  include:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[platform: 'none', os: 'none', env:'env0', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk1']]
        //account for inverse matrix exclude axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  exclude:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() ==  [[platform: 'none', os: 'none', env:'env1', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env0', jdk:'jdk1']]
    }
    @Test public void test_MultiPlatformGenerator_getBuildableMatrixAxes_nonmatrix() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java\nenv: "world=hello"\njdk:\n  - openjdk6')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == []
    }
    @Test public void test_MultiPlatformGenerator_getStashes() {
        def generator = loadSimpleResources()
        //empty stash
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello']]
        //single stash with defaults
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello', includes: 'world']]
        //set excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      excludes: goodbye')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello', includes: 'world', excludes: 'goodbye']]
        //set use_default_excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      use_default_excludes: false')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello', includes: 'world', use_default_excludes: false]]
        //set allow_empty away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      allow_empty: true')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello', includes: 'world', allow_empty: true]]
        //set matrix_axis away from default (note: YAML [jdk: openjdk6] parses as a list containing a map)
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6')
        pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getStashes() == [[name: 'hello', includes: 'world', matrix_axis: [jdk: 'openjdk6']]]
    }
    @Test public void test_MultiPlatformGenerator_getDefaultToolchainsScript_nonmatrix() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        String script = '''
            |#
            |# TOOLCHAINS SECTION
            |#
            |set +x
            |echo '# TOOLCHAINS SECTION'
            |set -x
            |#env toolchain section
            |#jdk toolchain section
            |some commands
            '''.stripMargin().trim() + '\n'
        assert pipeline_generator.generateToolchainSection() == script
    }
    @Test public void test_MultiPlatformGenerator_getDefaultToolchainsScript_matrix() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk6
            |  - openjdk7
            |env:
            |  - foo=hello
            |  - foo=world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        String script = '''
            |#
            |# TOOLCHAINS SECTION
            |#
            |set +x
            |echo '# TOOLCHAINS SECTION'
            |set -x
            |#env toolchain section
            |case ${env} in
            |  env0)
            |    export foo=hello
            |    ;;
            |  env1)
            |    export foo=world
            |    ;;
            |esac
            |#jdk toolchain section
            |case ${jdk} in
            |  jdk0)
            |    more commands
            |    ;;
            |  jdk1)
            |    some commands
            |    ;;
            |esac
            '''.stripMargin().trim() + '\n'
        assert pipeline_generator.generateToolchainSection() == script
    }
    @Test public void test_MultiPlatformGenerator_getDefaultToolchainsEnvironment_nonmatrix() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getDefaultToolchainsEnvironment() == [:]
    }
    @Test public void test_MultiPlatformGenerator_getDefaultToolchainsEnvironment_matrix() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk6
            |  - openjdk7
            |env:
            |  - foo=hello
            |  - foo=world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getDefaultToolchainsEnvironment() == [platform: 'none', os: 'none', env: 'env0', jdk: 'jdk0']
    }
    @Test public void test_MultiPlatformGenerator_matrix_additional_toolchain() {
        def generator = new LifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_matrix_added_toolchain.json')
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_matrix_added_toolchain.json')
        generator.loadToolchains(url.getFile())
        String yaml = """
            |language: python
            |python:
            |  - 2.7
            |  - 3.6
            |jdk:
            |  - openjdk8
            |  - openjdk11
            """.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        List result = [[platform: 'none', os: 'none', 'python':'python0', 'jdk':'jdk0'], [platform: 'none', os: 'none', 'python':'python1', 'jdk':'jdk0'], [platform: 'none', os: 'none', 'python':'python0', 'jdk':'jdk1'], [platform: 'none', os: 'none', 'python':'python1', 'jdk':'jdk1']]
        assert pipeline_generator.getBuildableMatrixAxes() == result
    }
    @Test public void test_MultiPlatformGenerator_getJervisYaml() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk6
            |  - openjdk7
            |env:
            |  - foo=hello
            |  - foo=world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getJervisYaml().language == 'java'
        assert pipeline_generator.getJervisYaml().jdk == ['openjdk6', 'openjdk7']
        assert pipeline_generator.getJervisYaml().env == ['foo=hello', 'foo=world']
    }
    @Test public void test_MultiPlatformGenerator_rawJervisYaml() {
        def generator = loadSimpleResources()
        String yaml = '''
            |language: java
            |jdk:
            |  - openjdk6
            |  - openjdk7
            |env:
            |  - foo=hello
            |  - foo=world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.rawJervisYaml.language == 'java'
        assert pipeline_generator.rawJervisYaml.jdk == ['openjdk6', 'openjdk7']
        assert pipeline_generator.rawJervisYaml.env == ['foo=hello', 'foo=world']
    }
    @Test public void test_MultiPlatformGenerator_isMatrixBuild_nonmatrix() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert !pipeline_generator.isMatrixBuild()
    }
    @Test public void test_MultiPlatformGenerator_isMatrixBuild_matrix() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java\njdk:\n  - openjdk6\n  - openjdk7')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.isMatrixBuild()
    }
    @Test public void test_MultiPlatformGenerator_getGenerator() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.getGenerator() instanceof LifecycleGenerator
        assert pipeline_generator.getGenerator().yaml_language == 'java'
    }
    @Test public void test_MultiPlatformGenerator_defaultPlatform_and_defaultOS() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.defaultPlatform == 'none'
        assert pipeline_generator.defaultOS == 'none'
    }
    @Test public void test_MultiPlatformGenerator_platforms_and_operating_systems() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.platforms == ['none']
        assert pipeline_generator.operating_systems == ['none']
    }
    @Test public void test_MultiPlatformGenerator_platform_generators() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.platform_generators.containsKey('none')
        assert pipeline_generator.platform_generators['none'].containsKey('none')
        assert pipeline_generator.platform_generators['none']['none'] instanceof LifecycleGenerator
    }
    @Test public void test_MultiPlatformGenerator_platform_jervis_yaml() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        assert pipeline_generator.platform_jervis_yaml.containsKey('none')
        assert pipeline_generator.platform_jervis_yaml['none'].containsKey('none')
        assert pipeline_generator.platform_jervis_yaml['none']['none'].language == 'java'
    }
    @Test public void test_MultiPlatformGenerator_getJervisYamlString() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        String yamlString = pipeline_generator.getJervisYamlString()
        assert yamlString.contains('language: java')
    }
    @Test public void test_MultiPlatformGenerator_cannot_instantiate_without_arguments() {
        // Use reflection to test private constructor
        def constructor = MultiPlatformGenerator.class.getDeclaredConstructor()
        constructor.setAccessible(true)
        try {
            constructor.newInstance()
            fail('Expected IllegalStateException to be thrown')
        }
        catch(java.lang.reflect.InvocationTargetException e) {
            // InvocationTargetException wraps the actual exception thrown by the constructor
            assert e.cause instanceof IllegalStateException
            assert e.cause.message.contains('must be instantiated with a MultiPlatformValidator')
        }
    }
    @Test public void test_MultiPlatformGenerator_validate_errors_for_invalid_yaml() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        // Test validation with invalid language
        List errors = pipeline_generator.validate(
            platform: 'none',
            os: 'none',
            yaml: 'language: invalid_language'
        )
        assert errors.size() > 0
        assert errors.any { it.contains('Unsupported language') }
    }
    @Test public void test_MultiPlatformGenerator_validate_no_errors_for_valid_yaml() {
        def generator = loadSimpleResources()
        generator.loadYamlString('language: java')
        def pipeline_generator = new MultiPlatformGenerator(generator)
        // Test validation with valid language
        List errors = pipeline_generator.validate(
            platform: 'none',
            os: 'none',
            yaml: 'language: java'
        )
        assert errors.size() == 0
    }
}
