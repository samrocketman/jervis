/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

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
//the PipelineGeneratorTest() class automatically sees the PipelineGenerator() class because they're in the same package
import net.gleske.jervis.exceptions.PipelineGeneratorException

import org.junit.After
import org.junit.Before
import org.junit.Test

class PipelineGeneratorTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new LifecycleGenerator()
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
    @Test public void test_PipelineGenerator_serialization() {
        URL url = this.getClass().getResource('/good_platforms_simple.json');
        generator.loadPlatformsFile(url.getFile())
        generator.loadYamlString('language: ruby')
        def pipeline = new PipelineGenerator(generator)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(pipeline)
    }
    @Test public void test_PipelineGenerator_getSecretPairsEnv() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048');
        URL file_url = this.getClass().getResource('/rsa_keys/rsa_secure_properties_map_test.yml')
        generator.loadYamlString(file_url.content.text)
        generator.setPrivateKey(url.content.text)
        generator.decryptSecrets()
        def pipeline_generator = new PipelineGenerator(generator)
        List<List> results = pipeline_generator.getSecretPairsEnv()
        assert results[0] instanceof List<Map>
        assert results[1] instanceof List<String>
        assert results[0]  == [[var: 'JERVIS_SECRETS_TEST', password: 'plaintext']]
        assert results[1] == ['JERVIS_SECRETS_TEST=plaintext']
    }
    @Test public void test_PipelineGenerator_supported_collections() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.supported_collections == ['foo', 'artifacts'].toSet()
    }
    @Test public void test_PipelineGenerator_getPublishableItems() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getPublishableItems() == ['artifacts', 'foo']
        pipeline_generator.supported_collections = ['foo']
        assert pipeline_generator.getPublishableItems() == ['foo']
    }
    @Test public void test_PipelineGenerator_getPublishable() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getPublishable('foo') == 'path/to/foo'
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem'
    }
    @Test public void test_PipelineGenerator_getPublishable_infinite_loop() {
        //detects an inifinite loop when using yaml anchors and keys
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    artifacts: &key\n      path: *key')
        shouldFail(PipelineGeneratorException) {
            new PipelineGenerator(generator)
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_EmptyList() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: []')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == ''
    }
    @Test public void test_PipelineGenerator_getPublishable_EmptyMap() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: {}')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == ''
    }
    @Test public void test_PipelineGenerator_getPublishable_List() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      - "**/*.gem"\n      - "**/*.rpm"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem,**/*.rpm'
    }
    @Test public void test_PipelineGenerator_getPublishable_Map() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      path: "**/*.gem"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem'
    }
    @Test public void test_PipelineGenerator_getPublishable_MapList() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      path:\n        - "**/*.gem"\n        - "**/*.rpm"')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem,**/*.rpm'
    }
    @Test public void test_PipelineGenerator_getBuildableMatrixAxes_matrix() {
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodby"]\njdk:\n  - openjdk6\n  - openjdk7')
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[platform: 'none', os: 'none', env:'env0', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env0', jdk:'jdk1'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk1']]
        //account for matrix include axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  include:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[platform: 'none', os: 'none', env:'env0', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env1', jdk:'jdk1']]
        //account for inverse matrix exclude axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  exclude:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() ==  [[platform: 'none', os: 'none', env:'env1', jdk:'jdk0'], [platform: 'none', os: 'none', env:'env0', jdk:'jdk1']]
    }
    @Test public void test_PipelineGenerator_getBuildableMatrixAxes_nonmatrix() {
        generator.loadYamlString('language: java\nenv: "world=hello"\njdk:\n  - openjdk6')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getBuildableMatrixAxes() == []
    }
    @Test public void test_PipelineGenerator_getStashMap() {
        //empty stash
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello')
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //single stash with defaults
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
        //set excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      excludes: goodbye')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'goodbye', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
        //set use_default_excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      use_default_excludes: false')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:false, allow_empty:false, matrix_axis:[:]]]
        //set allow_empty away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      allow_empty: true')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:true, matrix_axis:[:]]]
        //set matrix_axis away from default
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis: [jdk: openjdk6]')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
    }
    @Test public void test_PipelineGenerator_getStashMap_nonmatrix() {
        //automatically infer stashes from nonmatrix
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    foo: hello\n    artifacts: world\n    baz: goodbye')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getStashMap() == [foo:[includes:'hello', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]], artifacts:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]], baz:[includes:'goodbye', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
    }
    @Test public void test_PipelineGenerator_getStashMap_matrix() {
        //set matrix_axis away from default
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6')
        def pipeline_generator = new PipelineGenerator(generator)
        //for this matrix
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        //not for this matrix
        assert pipeline_generator.getStashMap([jdk: 'openjdk7']) == [:]
        //alternate for this matrix
        assert pipeline_generator.getStashMap([jdk: 'jdk0']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        //friendlyLabel for this matrix
        generator = new LifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_friendly.json');
        generator.loadToolchains(url.getFile())
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        assert pipeline_generator.getStashMap([jdk: 'jdk:openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        assert pipeline_generator.getStashMap([jdk: 'jdk0']) == [:]
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6\n  collect:\n    hello: goodbye_world')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'goodbye_world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      matrix_axis:\n        jdk: openjdk6\n  collect:\n    hello: happy_days')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'happy_days', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
    }
    @Test public void test_PipelineGenerator_getStashMap_test_for_failure() {
        //not a map
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - hello')
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //name defined but empty
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: ""\n      includes: world')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //includes defined but empty
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: ""')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //is a matrix build but no matrix is defined
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis: {}')
        pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
    }
    @Test public void test_PipelineGenerator_collect_settings_defaults_filtering() {
        generator.loadYamlString('language: java')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            '': ['planet': 'venus'],
            a: 'hello',
            b: ['planet': 'earth'],
            c: ['planet': 'mars'],
            d: [:]
        ]
        assert [b: [planet: 'earth', 'skip_on_pr': false, 'skip_on_tag': false], c: [planet: 'mars', 'skip_on_pr': false, 'skip_on_tag': false]] == pipeline_generator.collect_settings_defaults
    }
    @Test public void test_PipelineGenerator_collect_settings_defaults_appending() {
        generator.loadYamlString('language: java')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [a: ['planet': 'earth']]
        pipeline_generator.collect_settings_defaults = [b: ['planet': 'mars']]
        assert [a: [planet: 'earth', 'skip_on_pr': false, 'skip_on_tag': false], b: [planet: 'mars', 'skip_on_pr': false, 'skip_on_tag': false]] == pipeline_generator.collect_settings_defaults
    }
    @Test public void test_PipelineGenerator_getPublishableItems_without_supported_collections() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts: hello')
        def pipeline_generator = new PipelineGenerator(generator)
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.publishableItems
        }
    }
    @Test public void test_PipelineGenerator_getPublishableItems_should_filter_empty_collections() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts: ""')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert [] == pipeline_generator.getPublishableItems()
    }
    @Test public void test_PipelineGenerator_getPublishable_from_collect_settings_defaults() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts:\n      - hello\n      - world')
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true, path: 'hello,world', 'skip_on_pr': false, 'skip_on_tag': false] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_PipelineGenerator_getPublishable_from_collect_settings_defaults_customized() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    artifacts:
            |      path:
            |        - hello
            |        - world
            |      allowEmptyArchive: false
            |      caseSensitive: true
            |      defaultExcludes: true
            |      excludes: 'mars'
            |      onlyIfSuccessful: false
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        assert [allowEmptyArchive: false, caseSensitive: true, defaultExcludes: true, excludes: 'mars', onlyIfSuccessful: false, path: 'hello,world', 'skip_on_pr': false, 'skip_on_tag': false] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_PipelineGenerator_getPublishable_from_collect_settings_defaults_filesets() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    artifacts:
            |      path:
            |        - hello
            |        - world
            |      excludes:
            |        - hello
            |        - mars
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        pipeline_generator.collect_settings_filesets = [artifacts: ['excludes']]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'hello,mars', onlyIfSuccessful: true, path: 'hello,world', 'skip_on_pr': false, 'skip_on_tag': false] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_PipelineGenerator_getPublishable_from_collect_settings_defaults_empty_filesets() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    artifacts:
            |      path:
            |        - hello
            |        - world
            |      excludes: []
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'mars', onlyIfSuccessful: true]]
        pipeline_generator.collect_settings_filesets = [artifacts: ['excludes']]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'mars', onlyIfSuccessful: true, path: 'hello,world', 'skip_on_pr': false, 'skip_on_tag': false] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_PipelineGenerator_getPublishable_bug_undefined_collect() {
        String yaml = 'language: java'
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_bug_undefined_collect_matrix() {
        String yaml = '''
            |language: java
            |env:
            | - foo=hello
            | - foo=world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation1() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: Only capitalization ending with a period.
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: '^[A-Z][ a-z]+\\.$'
            ]
        ]
        assert 'Only capitalization ending with a period.' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation1_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: Only capitalization ending with a period.
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: '^[A-Z][ a-z]+\\.$'
            ]
        ]
        assert 'Only capitalization ending with a period.' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation2() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: ['^[A-Z][ a-z]+\\.$', '^[a-f0-9]+$']
            ]
        ]
        //validate a hex value as an option in addition to capitalized sentences
        assert '0123abcdef' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation2_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: ['^[A-Z][ a-z]+\\.$', '^[a-f0-9]+$']
            ]
        ]
        //validate a hex value as an option in addition to capitalized sentences
        assert '0123abcdef' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_user_failure() {
        //should return a default if a user fails validation
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: '^[A-Z][ a-z]+\\.$'
            ]
        ]
        assert 'Some bad input was given.' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_user_failure_matrix() {
        //should return a default if a user fails validation
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //define the validator to validate jenkins.collect.fake.validateme setting
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: '^[A-Z][ a-z]+\\.$'
            ]
        ]
        assert 'Some bad input was given.' == pipeline_generator.getPublishable('fake')['validateme']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure1() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //admin defined invalid type
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: 3
            ]
        ]
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure1_matrix() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //admin defined invalid type
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: 3
            ]
        ]
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure2() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //admin defined invalid type
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: ['hello', 3]
            ]
        ]
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure2_matrix() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 'Some bad input was given.'
            ]
        ]
        //admin defined invalid type
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: ['hello', 3]
            ]
        ]
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure3() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 36
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 45
            ]
        ]
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: 'oops, cannot validate an int with a string'
            ]
        ]
        //atempted to run string validation on an int (an admin error)
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure3_matrix() {
        //throw a hard exception because admin failing is irrecoverable
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 36
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                validateme: 45
            ]
        ]
        pipeline_generator.collect_settings_validation = [
            fake: [
                validateme: 'oops, cannot validate an int with a string'
            ]
        ]
        //atempted to run string validation on an int (an admin error)
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getPublishable('fake')
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_bug_invalid_default_fileset() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_filesets = [fake: ['anotherpath']]
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        assert '**/*' == pipeline_generator.getPublishable('fake')['anotherpath']
    }
    @Test public void test_PipelineGenerator_getPublishable_bug_invalid_default_fileset_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_filesets = [fake: ['anotherpath']]
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        assert '**/*' == pipeline_generator.getPublishable('fake')['anotherpath']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_default_collect_settings_on_tag_pr_default() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.getPublishable('foo') == ['title': 'foo title', 'skip_on_pr': false, 'skip_on_tag': false, 'path': 'goodbye']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_default_collect_settings_on_tag_pr_override() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_pr: true
            |      skip_on_tag: true
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.getPublishable('foo') == ['title': 'foo title', 'skip_on_pr': true, 'skip_on_tag': true, 'path': 'goodbye']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_default_collect_settings_on_tag() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_tag: true
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.getPublishable('foo') == ['title': 'foo title', 'skip_on_pr': false, 'skip_on_tag': true, 'path': 'goodbye']
    }
    @Test public void test_PipelineGenerator_getPublishable_with_default_collect_settings_on_pr() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_pr: true
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.getPublishable('foo') == ['title': 'foo title', 'skip_on_pr': true, 'skip_on_tag': false, 'path':'goodbye']
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_badargs() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.stashmap_preprocessor = [
            baz: { ->
                'delet dis'
            },
            fake: { Map settings ->
                'keep dis'
            },
            bar: { Map settings, String extra ->
                'delet dis'
            },
            boz: { String wrong ->
                'delet dis'
            },
            boo: { Map another ->
                'also keep dis'
            }
        ]
        assert !('baz' in pipeline_generator.stashmap_preprocessor)
        assert !('bar' in pipeline_generator.stashmap_preprocessor)
        assert !('boz' in pipeline_generator.stashmap_preprocessor)
        assert ('fake' in pipeline_generator.stashmap_preprocessor)
        assert ('boo' in pipeline_generator.stashmap_preprocessor)
        assert 'some/path' == pipeline_generator.getPublishable('fake')
        assert 'some/path' == pipeline_generator.stashMap['fake']['includes']
        assert '' == pipeline_generator.getPublishable('boo')
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_badargs_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: fake
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.stashmap_preprocessor = [
            baz: { ->
                'delet dis'
            },
            fake: { Map settings ->
                'keep dis'
            },
            bar: { Map settings, String extra ->
                'delet dis'
            },
            boz: { String wrong ->
                'delet dis'
            },
            boo: { Map another ->
                'also keep dis'
            }
        ]
        assert 'some/path' == pipeline_generator.getPublishable('fake')
        assert 'some/path' == pipeline_generator.getStashMap([env: 'foo=world'])['fake']['includes']
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_badargs2() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                'keep dis'
            }
        ]
        assert 'some/path' == pipeline_generator.getPublishable('fake')['path']
        assert 'keep dis' == pipeline_generator.stashMap['fake']['includes']
        assert '**/*' == pipeline_generator.getPublishable('fake')['anotherpath']
        assert '' == pipeline_generator.getPublishable('boo')
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_badargs2_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: fake
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                'keep dis'
            }
        ]
        assert 'some/path' == pipeline_generator.getPublishable('fake')['path']
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        assert 'keep dis' == pipeline_generator.getStashMap([env: 'foo=world'])['fake']['includes']
        assert '**/*' == pipeline_generator.getPublishable('fake')['anotherpath']
        assert '' == pipeline_generator.getPublishable('boo')
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_invalid_return() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        //return invalid
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                true
            }
        ]
        //when preprocessor returns a non-String result,
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.stashMap
        }
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_invalid_return_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: fake
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        //return invalid
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                true
            }
        ]
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        //when preprocessor returns a non-String result,
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getStashMap([env: 'foo=world'])
        }
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_exception() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        //return invalid
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                throw new Exception('oops, admin wrote buggy code')
            }
        ]
        //when preprocessor throws an exception,
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.stashMap
        }
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_exception_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: fake
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        //return invalid
        pipeline_generator.stashmap_preprocessor = [
            fake: { Map settings ->
                throw new Exception('oops, admin wrote buggy code')
            }
        ]
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        //when preprocessor throws an exception,
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.getStashMap([env: 'foo=world'])
        }
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_basic_path_success() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    html: 'foo/bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert 'foo/bar' == pipeline_generator.getPublishable('html')
        assert ['html'] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_basic_path_success_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: html
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    html: 'foo/bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert 'foo/bar' == pipeline_generator.getPublishable('html')
        assert ['html'] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_basic_path_fail() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    html:
            |      - 'foo'
            |      - 'bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert '' == pipeline_generator.getPublishable('html')
        assert [:] == pipeline_generator.stashMap
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_basic_path_fail_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: html
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    html:
            |      - 'foo'
            |      - 'bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert '' == pipeline_generator.getPublishable('html')
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        assert [:] == pipeline_generator.getStashMap([env: 'foo=world'])
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_complex_path_success() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    html: 'foo/bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            html: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert 'foo/bar' == pipeline_generator.getPublishable('html')['path']
        assert ['html'] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_complex_path_success_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: html
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    html: 'foo/bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            html: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        assert 'foo/bar' == pipeline_generator.getPublishable('html')['path']
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        assert 'foo/bar' == pipeline_generator.getStashMap([env: 'foo=world'])['html']['includes']
        assert ['html'] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_complex_path_fail() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    html:
            |      - 'foo'
            |      - 'bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            html: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        pipeline_generator.supported_collections = ['html']
        assert [:] == pipeline_generator.getPublishable('html')
        assert [:] == pipeline_generator.stashMap
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_getPublishable_validate_complex_path_fail_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: html
            |      matrix_axis:
            |        env: foo=world
            |  collect:
            |    html:
            |      - 'foo'
            |      - 'bar'
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            html: [
                anotherpath: '**/*'
            ]
        ]
        pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
        pipeline_generator.supported_collections = ['html']
        assert [:] == pipeline_generator.getPublishable('html')
        assert [:] == pipeline_generator.getStashMap([env: 'foo=hello'])
        assert [:] == pipeline_generator.getStashMap([env: 'foo=world'])
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_success_nonmatrix() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    html: build/docs/groovydoc
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_filesets = [html: ['includes']]
        pipeline_generator.collect_settings_defaults = [
            html: [
                allowMissing: false,
                alwaysLinkToLastBuild: false,
                includes: '**/*',
                keepAll: false,
                reportFiles: 'index.html',
                reportName: 'HTML Report',
                reportTitles: ''
            ]
        ]
        pipeline_generator.stashmap_preprocessor = [
            html: { Map settings ->
                settings['includes']?.tokenize(',').collect {
                    "${settings['path']  -~ '/$' -~ '^/'}/${it}"
                }.join(',').toString()
            }
        ]
        pipeline_generator.collect_settings_validation = [
            html: [
                path: '''^[^,\\:*?"'<>|]+$'''
            ]
        ]
        assert pipeline_generator.stashMap == ['html':['includes':'build/docs/groovydoc/**/*', 'excludes':'', 'use_default_excludes':true, 'allow_empty':false, 'matrix_axis':[:]]]
        assert pipeline_generator.getPublishable('html') == ['allowMissing':false, 'alwaysLinkToLastBuild':false, 'includes':'**/*', 'keepAll':false, 'reportFiles':'index.html', 'reportName':'HTML Report', 'reportTitles':'', 'path':'build/docs/groovydoc', 'skip_on_pr': false, 'skip_on_tag': false]
        assert pipeline_generator.stashes == [['name':'html', 'includes':'build/docs/groovydoc']]
    }
    @Test public void test_PipelineGenerator_stashmap_preprocessor_success_matrix() {
        String yaml = '''
            |language: java
            |env:
            |  - foo=hello
            |  - foo=world
            |jenkins:
            |  stash:
            |    - name: html
            |      matrix_axis:
            |        env: 'foo=hello'
            |  collect:
            |    html: build/docs/groovydoc
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['html']
        pipeline_generator.collect_settings_filesets = [html: ['includes']]
        pipeline_generator.collect_settings_defaults = [
            html: [
                allowMissing: false,
                alwaysLinkToLastBuild: false,
                includes: '**/*',
                keepAll: false,
                reportFiles: 'index.html',
                reportName: 'HTML Report',
                reportTitles: ''
            ]
        ]
        pipeline_generator.stashmap_preprocessor = [
            html: { Map settings ->
                settings['includes']?.tokenize(',').collect {
                    "${settings['path']  -~ '/$' -~ '^/'}/${it}"
                }.join(',').toString()
            }
        ]
        pipeline_generator.collect_settings_validation = [
            html: [
                path: '''^[^,\\:*?"'<>|]+$'''
            ]
        ]
        assert pipeline_generator.stashMap == [:]
        assert pipeline_generator.getStashMap([env: 'foo=world']) == [:]
        assert pipeline_generator.getStashMap(['env': 'foo=hello']) == ['html':['includes':'build/docs/groovydoc/**/*', 'excludes':'', 'use_default_excludes':true, 'allow_empty':false, 'matrix_axis':['env':'foo=hello']]]
        assert pipeline_generator.getPublishable('html') == ['allowMissing':false, 'alwaysLinkToLastBuild':false, 'includes':'**/*', 'keepAll':false, 'reportFiles':'index.html', 'reportName':'HTML Report', 'reportTitles':'', 'path':'build/docs/groovydoc', 'skip_on_pr': false, 'skip_on_tag': false]
        assert pipeline_generator.stashes == [['name':'html', 'matrix_axis':['env':'foo=hello'], 'includes':'build/docs/groovydoc']]
    }
    @Test public void test_PipelineGenerator_publish_skip_on_pr() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_pr: true
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.publishableItems == ['bar', 'foo']
        pipeline_generator.generator.is_pr = true
        assert pipeline_generator.publishableItems == ['bar']
    }
    @Test public void test_PipelineGenerator_publish_skip_on_pr_by_admin_default() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [
                title: 'foo title',
                skip_on_pr: true
            ]
        ]

        pipeline_generator.generator.is_pr = true
        assert pipeline_generator.publishableItems == ['bar']
    }
    @Test public void test_PipelineGenerator_publish_skip_on_pr_by_admin_default_user_override() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_pr: false
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [
                title: 'foo title',
                skip_on_pr: true
            ]
        ]

        pipeline_generator.generator.is_pr = true
        assert pipeline_generator.publishableItems == ['bar', 'foo']
    }
    @Test public void test_PipelineGenerator_publish_skip_on_tag() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_tag: true
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [title: 'foo title']
        ]

        assert pipeline_generator.publishableItems == ['bar', 'foo']
        pipeline_generator.generator.is_tag = true
        assert pipeline_generator.publishableItems == ['bar']
    }
    @Test public void test_PipelineGenerator_publish_skip_on_tag_by_admin_default() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [
                title: 'foo title',
                skip_on_tag: true
            ]
        ]

        pipeline_generator.generator.is_tag = true
        assert pipeline_generator.publishableItems == ['bar']
    }
    @Test public void test_PipelineGenerator_publish_skip_on_tag_by_admin_default_user_override() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    foo:
            |      path: 'goodbye'
            |      skip_on_tag: false
            |    bar: hello world
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'bar']
        pipeline_generator.collect_settings_defaults = [
            foo: [
                title: 'foo title',
                skip_on_tag: true
            ]
        ]

        pipeline_generator.generator.is_tag = true
        assert pipeline_generator.publishableItems == ['bar', 'foo']
    }
    @Test public void test_PipelineGenerator_getDefaultToolchainsScript_nonmatrix() {
        String yaml = '''
            |language: java
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
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
        assert pipeline_generator.getDefaultToolchainsScript() == script
    }
    @Test public void test_PipelineGenerator_getDefaultToolchainsScript_matrix() {
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
        def pipeline_generator = new PipelineGenerator(generator)
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
        assert pipeline_generator.getDefaultToolchainsScript() == script
    }
    @Test public void test_PipelineGenerator_getDefaultToolchainsEnvironment_nonmatrix() {
        String yaml = '''
            |language: java
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getDefaultToolchainsEnvironment() == [:]
    }
    @Test public void test_PipelineGenerator_getDefaultToolchainsEnvironment_matrix() {
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
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getDefaultToolchainsEnvironment() == [platform: 'none', os: 'none', env: 'env0', jdk: 'jdk0']
    }
    @Test public void test_PipelineGenerator_matrix_additional_toolchain() {
        generator = new LifecycleGenerator()
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
        def pipeline_generator = new PipelineGenerator(generator)
        List result = [[platform: 'none', os: 'none', 'python':'python0', 'jdk':'jdk0'], [platform: 'none', os: 'none', 'python':'python1', 'jdk':'jdk0'], [platform: 'none', os: 'none', 'python':'python0', 'jdk':'jdk1'], [platform: 'none', os: 'none', 'python':'python1', 'jdk':'jdk1']]
        assert pipeline_generator.getBuildableMatrixAxes() == result
    }
    @Test public void test_PipelineGenerator_getYaml() {
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
        def pipeline_generator = new PipelineGenerator(generator)
        assert pipeline_generator.getYaml().language == 'java'
        assert pipeline_generator.getYaml().jdk == ['openjdk6', 'openjdk7']
        assert pipeline_generator.getYaml().env == ['foo=hello', 'foo=world']
    }
}
