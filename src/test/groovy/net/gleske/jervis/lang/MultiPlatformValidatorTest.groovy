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
//the MultiPlatformValidatorTest() class automatically sees the MultiPlatformValidator() class because they're in the same package
import net.gleske.jervis.exceptions.MultiPlatformValidatorException

import org.junit.After
import org.junit.Before
import org.junit.Test

class MultiPlatformValidatorTest extends GroovyTestCase {
    def platforms
    //helper method to fully load multi-platform validator with test resources
    private void loadFullMultiPlatformResources() {
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
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        platforms = new MultiPlatformValidator()
    }
    //tear down after every test
    @After protected void tearDown() {
        platforms = null
        super.tearDown()
    }
    //platform loading tests (similar to PlatformValidatorTest)
    @Test public void test_MultiPlatformValidator_loadPlatformsString() {
        assert platforms.platform_obj == null
        assert platforms.known_platforms == []
        assert platforms.known_operating_systems == []
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        assert platforms.platform_obj != null
        assert platforms.platform_obj instanceof PlatformValidator
        assert platforms.known_platforms.size() > 0
        assert platforms.known_operating_systems.size() > 0
        assert 'x86_64' in platforms.known_platforms
        assert 'arm64' in platforms.known_platforms
        assert 'ubuntu2204' in platforms.known_operating_systems
        assert 'alpine3' in platforms.known_operating_systems
    }
    @Test public void test_MultiPlatformValidator_loadPlatformsString_defaults() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        assert platforms.platform_obj.platforms['defaults']['platform'] == 'x86_64'
        assert platforms.platform_obj.platforms['defaults']['os'] == 'ubuntu2204'
        assert platforms.platform_obj.platforms['defaults']['stability'] == 'stable'
        assert platforms.platform_obj.platforms['defaults']['sudo'] == 'sudo'
    }
    @Test public void test_MultiPlatformValidator_loadPlatformsString_simple() {
        URL url = this.getClass().getResource('/good_platforms_simple.json')
        platforms.loadPlatformsString(url.content.text)
        assert platforms.platform_obj.platforms instanceof Map
        assert platforms.platform_obj.platforms['defaults']['platform'] == 'docker'
    }
    //lifecycle loading tests (similar to LifecycleValidatorTest)
    @Test public void test_MultiPlatformValidator_loadLifecyclesString() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        assert platforms.lifecycles == [:]
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        assert 'ubuntu2204' in platforms.lifecycles
        assert platforms.lifecycles['ubuntu2204'] instanceof LifecycleValidator
        assert platforms.lifecycles['ubuntu2204'].lifecycles['java']['friendlyName'] == 'Java'
        assert 'java' in platforms.lifecycles['ubuntu2204'].languages
        assert 'shell' in platforms.lifecycles['ubuntu2204'].languages
    }
    @Test public void test_MultiPlatformValidator_loadLifecyclesString_multiple_os() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mptlifecycles-alpine3-stable.yaml')
        platforms.loadLifecyclesString('alpine3', url.content.text)
        assert 'ubuntu2204' in platforms.lifecycles
        assert 'alpine3' in platforms.lifecycles
        assert platforms.lifecycles['ubuntu2204'] instanceof LifecycleValidator
        assert platforms.lifecycles['alpine3'] instanceof LifecycleValidator
    }
    @Test public void test_MultiPlatformValidator_supportedLanguage_lifecycle() {
        loadFullMultiPlatformResources()
        assert true == platforms.lifecycles['ubuntu2204'].supportedLanguage('java')
        assert true == platforms.lifecycles['ubuntu2204'].supportedLanguage('shell')
        assert true == platforms.lifecycles['alpine3'].supportedLanguage('java')
        assert false == platforms.lifecycles['ubuntu2204'].supportedLanguage('derpy')
    }
    //toolchain loading tests (similar to ToolchainValidatorTest)
    @Test public void test_MultiPlatformValidator_loadToolchainsString() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        assert platforms.toolchains == [:]
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        assert 'ubuntu2204' in platforms.toolchains
        assert platforms.toolchains['ubuntu2204'] instanceof ToolchainValidator
        assert 'jdk' in platforms.toolchains['ubuntu2204'].toolchain_list
        assert 'env' in platforms.toolchains['ubuntu2204'].toolchain_list
    }
    @Test public void test_MultiPlatformValidator_loadToolchainsString_multiple_os() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-alpine3-stable.yaml')
        platforms.loadToolchainsString('alpine3', url.content.text)
        assert 'ubuntu2204' in platforms.toolchains
        assert 'alpine3' in platforms.toolchains
        assert platforms.toolchains['ubuntu2204'] instanceof ToolchainValidator
        assert platforms.toolchains['alpine3'] instanceof ToolchainValidator
    }
    @Test public void test_MultiPlatformValidator_supportedLanguage_toolchain() {
        loadFullMultiPlatformResources()
        assert true == platforms.toolchains['ubuntu2204'].supportedLanguage('java')
        assert true == platforms.toolchains['ubuntu2204'].supportedLanguage('shell')
        assert false == platforms.toolchains['ubuntu2204'].supportedLanguage('derpy')
    }
    @Test public void test_MultiPlatformValidator_supportedToolchain() {
        loadFullMultiPlatformResources()
        assert true == platforms.toolchains['ubuntu2204'].supportedToolchain('jdk')
        assert true == platforms.toolchains['ubuntu2204'].supportedToolchain('env')
        assert false == platforms.toolchains['ubuntu2204'].supportedToolchain('derpy')
    }
    @Test public void test_MultiPlatformValidator_supportedTool() {
        loadFullMultiPlatformResources()
        assert true == platforms.toolchains['ubuntu2204'].supportedTool('jdk', 'openjdk11')
        assert true == platforms.toolchains['ubuntu2204'].supportedTool('jdk', 'openjdk17')
        assert false == platforms.toolchains['ubuntu2204'].supportedTool('jdk', 'derpy')
    }
    @Test public void test_MultiPlatformValidator_isFriendlyLabel() {
        loadFullMultiPlatformResources()
        assert true == platforms.toolchains['ubuntu2204'].isFriendlyLabel('jdk')
        assert true == platforms.toolchains['ubuntu2204'].isFriendlyLabel('python')
        assert false == platforms.toolchains['ubuntu2204'].isFriendlyLabel('env')
    }
    @Test public void test_MultiPlatformValidator_toolchainType() {
        loadFullMultiPlatformResources()
        assert 'advanced' == platforms.toolchains['ubuntu2204'].toolchainType('env')
    }
    @Test public void test_MultiPlatformValidator_known_toolchains() {
        loadFullMultiPlatformResources()
        assert platforms.known_toolchains.size() > 0
        assert 'jdk' in platforms.known_toolchains
        assert 'env' in platforms.known_toolchains
        assert 'python' in platforms.known_toolchains
    }
    //file name generation tests
    @Test public void test_MultiPlatformValidator_getLifecycleFiles() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        List lifecycleFiles = platforms.getLifecycleFiles()
        assert lifecycleFiles != null
        assert 'lifecycles-ubuntu2204-stable' in lifecycleFiles
        assert 'lifecycles-ubuntu2204-unstable' in lifecycleFiles
        assert 'lifecycles-alpine3-stable' in lifecycleFiles
        assert 'lifecycles-alpine3-unstable' in lifecycleFiles
    }
    @Test public void test_MultiPlatformValidator_getToolchainFiles() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        List toolchainFiles = platforms.getToolchainFiles()
        assert toolchainFiles != null
        assert 'toolchains-ubuntu2204-stable' in toolchainFiles
        assert 'toolchains-ubuntu2204-unstable' in toolchainFiles
        assert 'toolchains-alpine3-stable' in toolchainFiles
        assert 'toolchains-alpine3-unstable' in toolchainFiles
    }
    @Test public void test_MultiPlatformValidator_getLifecycleFiles_null_without_platforms() {
        // Without loading platforms, getLifecycleFiles should return null
        assert platforms.getLifecycleFiles() == null
    }
    @Test public void test_MultiPlatformValidator_getToolchainFiles_null_without_platforms() {
        // Without loading platforms, getToolchainFiles should return null
        assert platforms.getToolchainFiles() == null
    }
    //validation tests
    @Test public void test_MultiPlatformValidator_validate_success() {
        loadFullMultiPlatformResources()
        // Should not throw exception
        platforms.validate()
    }
    @Test public void test_MultiPlatformValidator_validate_no_platform_obj() {
        // Without loading platforms, validate should throw an exception
        // (either MultiPlatformValidatorException or NullPointerException depending on implementation)
        shouldFail {
            platforms.validate()
        }
    }
    @Test public void test_MultiPlatformValidator_validate_no_lifecycles() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-alpine3-stable.yaml')
        platforms.loadToolchainsString('alpine3', url.content.text)
        shouldFail(MultiPlatformValidatorException) {
            platforms.validate()
        }
    }
    @Test public void test_MultiPlatformValidator_validate_no_toolchains() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mptlifecycles-alpine3-stable.yaml')
        platforms.loadLifecyclesString('alpine3', url.content.text)
        shouldFail(MultiPlatformValidatorException) {
            platforms.validate()
        }
    }
    @Test public void test_MultiPlatformValidator_validate_missing_os_lifecycle() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        // Only load ubuntu2204 lifecycle, not alpine3
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-alpine3-stable.yaml')
        platforms.loadToolchainsString('alpine3', url.content.text)
        shouldFail(MultiPlatformValidatorException) {
            platforms.validate()
        }
    }
    @Test public void test_MultiPlatformValidator_validate_missing_os_toolchain() {
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('ubuntu2204', url.content.text)
        url = this.getClass().getResource('/mptlifecycles-alpine3-stable.yaml')
        platforms.loadLifecyclesString('alpine3', url.content.text)
        // Only load ubuntu2204 toolchain, not alpine3
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('ubuntu2204', url.content.text)
        shouldFail(MultiPlatformValidatorException) {
            platforms.validate()
        }
    }
    //generator creation tests
    @Test public void test_MultiPlatformValidator_getGeneratorFromJervis() {
        loadFullMultiPlatformResources()
        LifecycleGenerator generator = platforms.getGeneratorFromJervis(yaml: 'language: shell')
        assert generator != null
        assert generator instanceof LifecycleGenerator
        assert generator.yaml_language == 'shell'
        assert generator.multiPlatform == true
    }
    @Test public void test_MultiPlatformValidator_getGeneratorFromJervis_java() {
        loadFullMultiPlatformResources()
        LifecycleGenerator generator = platforms.getGeneratorFromJervis(yaml: 'language: java\njdk: openjdk11')
        assert generator != null
        assert generator.yaml_language == 'java'
    }
    @Test public void test_MultiPlatformValidator_getGeneratorFromJervis_python() {
        loadFullMultiPlatformResources()
        LifecycleGenerator generator = platforms.getGeneratorFromJervis(yaml: 'language: python')
        assert generator != null
        assert generator.yaml_language == 'python'
    }
    @Test public void test_MultiPlatformValidator_getGeneratorFromJervis_with_folder_listing() {
        loadFullMultiPlatformResources()
        LifecycleGenerator generator = platforms.getGeneratorFromJervis(
            yaml: 'language: java',
            folder_listing: ['build.gradle', 'src/']
        )
        assert generator != null
        assert generator.folder_listing == ['build.gradle', 'src/']
    }
    //YAML validation tests
    @Test public void test_MultiPlatformValidator_validateJervisYaml_valid() {
        loadFullMultiPlatformResources()
        Map jervisYaml = [
            language: 'shell',
            jenkins: [
                platform: ['x86_64', 'arm64'],
                os: ['ubuntu2204', 'alpine3']
            ]
        ]
        // Should not throw exception
        platforms.validateJervisYaml(jervisYaml)
    }
    @Test public void test_MultiPlatformValidator_validateJervisYaml_invalid_top_level_platform() {
        loadFullMultiPlatformResources()
        Map jervisYaml = [
            language: 'shell',
            platform: 'x86_64'  // platform as top-level key is not allowed
        ]
        shouldFail(Exception) {
            platforms.validateJervisYaml(jervisYaml)
        }
    }
    @Test public void test_MultiPlatformValidator_validateJervisYaml_invalid_top_level_os() {
        loadFullMultiPlatformResources()
        Map jervisYaml = [
            language: 'shell',
            os: 'ubuntu2204'  // os as top-level key is not allowed
        ]
        shouldFail(Exception) {
            platforms.validateJervisYaml(jervisYaml)
        }
    }
    @Test public void test_MultiPlatformValidator_validateJervisYaml_invalid_platform_value() {
        loadFullMultiPlatformResources()
        Map jervisYaml = [
            language: 'groovy',
            jenkins: [
                platform: ['x86_64', 'foo']  // 'foo' is not a valid platform
            ]
        ]
        shouldFail(Exception) {
            platforms.validateJervisYaml(jervisYaml)
        }
    }
    @Test public void test_MultiPlatformValidator_validateJervisYaml_invalid_os_value() {
        loadFullMultiPlatformResources()
        Map jervisYaml = [
            language: 'groovy',
            jenkins: [
                os: ['alpine3', 'foo']  // 'foo' is not a valid os
            ]
        ]
        shouldFail(Exception) {
            platforms.validateJervisYaml(jervisYaml)
        }
    }
    //serialization tests
    @Test public void test_MultiPlatformValidator_serialization() {
        loadFullMultiPlatformResources()
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(platforms)
    }
    @Test public void test_MultiPlatformValidator_serialization_empty() {
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(platforms)
    }
    //integration tests
    @Test public void test_MultiPlatformValidator_full_workflow() {
        // Load platforms
        URL url = this.getClass().getResource('/mptplatforms.yaml')
        platforms.loadPlatformsString(url.content.text)

        // Get file names to load
        List lifecycleFiles = platforms.getLifecycleFiles()
        List toolchainFiles = platforms.getToolchainFiles()
        assert lifecycleFiles.size() > 0
        assert toolchainFiles.size() > 0

        // Load lifecycles and toolchains for available OSes
        url = this.getClass().getResource('/mptlifecycles-ubuntu2204-stable.yaml')
        platforms.loadLifecyclesString('lifecycles-ubuntu2204-stable', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-ubuntu2204-stable.yaml')
        platforms.loadToolchainsString('toolchains-ubuntu2204-stable', url.content.text)
        url = this.getClass().getResource('/mptlifecycles-alpine3-stable.yaml')
        platforms.loadLifecyclesString('lifecycles-alpine3-stable', url.content.text)
        url = this.getClass().getResource('/mpttoolchains-alpine3-stable.yaml')
        platforms.loadToolchainsString('toolchains-alpine3-stable', url.content.text)

        // Validate
        platforms.validate()

        // Create generator
        LifecycleGenerator generator = platforms.getGeneratorFromJervis(yaml: 'language: shell')
        assert generator != null
        assert generator.yaml_language == 'shell'
    }
    @Test public void test_MultiPlatformValidator_properties_after_load() {
        loadFullMultiPlatformResources()

        // Check known_platforms
        assert 'x86_64' in platforms.known_platforms
        assert 'arm64' in platforms.known_platforms
        assert 'amd64' in platforms.known_platforms

        // Check known_operating_systems
        assert 'ubuntu2204' in platforms.known_operating_systems
        assert 'alpine3' in platforms.known_operating_systems

        // Check known_toolchains
        assert 'jdk' in platforms.known_toolchains
        assert 'env' in platforms.known_toolchains
        assert 'python' in platforms.known_toolchains

        // Check lifecycles map
        assert platforms.lifecycles.keySet().containsAll(['ubuntu2204', 'alpine3'])

        // Check toolchains map
        assert platforms.toolchains.keySet().containsAll(['ubuntu2204', 'alpine3'])
    }
    @Test public void test_MultiPlatformValidator_lifecycle_validator_properties() {
        loadFullMultiPlatformResources()

        // Check LifecycleValidator properties for ubuntu2204
        LifecycleValidator lv = platforms.lifecycles['ubuntu2204']
        assert lv.lifecycles != null
        assert lv.languages != null
        assert 'java' in lv.languages
        assert 'shell' in lv.languages
        assert 'python' in lv.languages
    }
    @Test public void test_MultiPlatformValidator_toolchain_validator_properties() {
        loadFullMultiPlatformResources()

        // Check ToolchainValidator properties for ubuntu2204
        ToolchainValidator tv = platforms.toolchains['ubuntu2204']
        assert tv.toolchains != null
        assert tv.toolchain_list != null
        assert tv.languages != null
        assert 'java' in tv.languages
        assert 'shell' in tv.languages
        assert 'jdk' in tv.toolchain_list
        assert 'env' in tv.toolchain_list
    }
    @Test public void test_MultiPlatformValidator_toolValues() {
        loadFullMultiPlatformResources()
        // ubuntu2204 has openjdk8, openjdk11, openjdk17, openjdk21, openjdk25
        List jdkValues = platforms.toolchains['ubuntu2204'].toolValues('jdk')
        assert 'openjdk8' in jdkValues
        assert 'openjdk11' in jdkValues
        assert 'openjdk17' in jdkValues
    }
    @Test public void test_MultiPlatformValidator_different_toolchains_per_os() {
        loadFullMultiPlatformResources()

        // ubuntu2204 has more JDK versions
        List ubuntu_jdk = platforms.toolchains['ubuntu2204'].toolValues('jdk')
        // alpine3 has fewer JDK versions
        List alpine_jdk = platforms.toolchains['alpine3'].toolValues('jdk')

        assert ubuntu_jdk.size() >= alpine_jdk.size()
        assert 'openjdk8' in ubuntu_jdk
        assert 'openjdk11' in alpine_jdk
    }
}
