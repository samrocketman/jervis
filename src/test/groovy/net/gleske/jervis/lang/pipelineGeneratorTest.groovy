/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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
//the pipelineGeneratorTest() class automatically sees the pipelineGenerator() class because they're in the same package
import net.gleske.jervis.exceptions.PipelineGeneratorException
import org.junit.After
import org.junit.Before
import org.junit.Test

class pipelineGeneratorTest extends GroovyTestCase {
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
    @Test public void test_pipelineGenerator_serialization() {
        URL url = this.getClass().getResource('/good_platforms_simple.json');
        generator.loadPlatforms(url.getFile())
        generator.loadYamlString('language: ruby')
        def pipeline = new pipelineGenerator(generator)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(pipeline)
    }
    @Test public void test_pipelineGenerator_getSecretPairsEnv() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048');
        URL file_url = this.getClass().getResource('/rsa_keys/rsa_secure_properties_map_test.yml')
        generator.loadYamlString(file_url.content.text)
        generator.setPrivateKey(url.content.text)
        generator.decryptSecrets()
        def pipeline_generator = new pipelineGenerator(generator)
        List<List> results = pipeline_generator.getSecretPairsEnv()
        assert results[0] instanceof List<Map>
        assert results[1] instanceof List<String>
        assert results[0]  == [[var: 'JERVIS_SECRETS_TEST', password: 'plaintext']]
        assert results[1] == ['JERVIS_SECRETS_TEST=plaintext']
    }
    @Test public void test_pipelineGenerator_supported_collections() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.supported_collections == ['foo', 'artifacts'].toSet()
    }
    @Test public void test_pipelineGenerator_getPublishableItems() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getPublishableItems() == ['artifacts', 'foo']
        pipeline_generator.supported_collections = ['foo']
        assert pipeline_generator.getPublishableItems() == ['foo']
    }
    @Test public void test_pipelineGenerator_getPublishable() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: "**/*.gem"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getPublishable('foo') == 'path/to/foo'
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem'
    }
    @Test public void test_pipelineGenerator_getPublishable_infinite_loop() {
        //detects an inifinite loop when using yaml anchors and keys
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    artifacts: &key\n      path: *key')
        shouldFail(PipelineGeneratorException) {
            new pipelineGenerator(generator)
        }
    }
    @Test public void test_pipelineGenerator_getPublishable_EmptyList() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: []')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == ''
    }
    @Test public void test_pipelineGenerator_getPublishable_EmptyMap() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts: {}')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == ''
    }
    @Test public void test_pipelineGenerator_getPublishable_List() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      - "**/*.gem"\n      - "**/*.rpm"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem,**/*.rpm'
    }
    @Test public void test_pipelineGenerator_getPublishable_Map() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      path: "**/*.gem"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem'
    }
    @Test public void test_pipelineGenerator_getPublishable_MapList() {
        generator.loadYamlString('language: ruby\njenkins:\n  collect:\n    foo: path/to/foo\n    artifacts:\n      path:\n        - "**/*.gem"\n        - "**/*.rpm"')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert pipeline_generator.getPublishable('artifacts') == '**/*.gem,**/*.rpm'
    }
    @Test public void test_pipelineGenerator_getBuildableMatrixAxes_matrix() {
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodby"]\njdk:\n  - openjdk6\n  - openjdk7')
        def pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[env:'env0', jdk:'jdk0'], [env:'env1', jdk:'jdk0'], [env:'env0', jdk:'jdk1'], [env:'env1', jdk:'jdk1']]
        //account for matrix include axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  include:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() == [[env:'env0', jdk:'jdk0'], [env:'env1', jdk:'jdk1']]
        //account for inverse matrix exclude axes
        generator.loadYamlString('language: java\nenv: ["world=hello", "world=goodbye"]\njdk:\n  - openjdk6\n  - openjdk7\nmatrix:\n  exclude:\n    - {env: "world=hello", jdk: openjdk6}\n    - {env: "world=goodbye", jdk: openjdk7}')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getBuildableMatrixAxes() ==  [[env:'env1', jdk:'jdk0'], [env:'env0', jdk:'jdk1']]
    }
    @Test public void test_pipelineGenerator_getBuildableMatrixAxes_nonmatrix() {
        generator.loadYamlString('language: java\nenv: "world=hello"\njdk:\n  - openjdk6')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getBuildableMatrixAxes() == []
    }
    @Test public void test_pipelineGenerator_getStashMap() {
        //empty stash
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello')
        def pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //single stash with defaults
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
        //set excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      excludes: goodbye')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'goodbye', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
        //set use_default_excludes away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      use_default_excludes: false')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:false, allow_empty:false, matrix_axis:[:]]]
        //set allow_empty away from default
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      allow_empty: true')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:true, matrix_axis:[:]]]
        //set matrix_axis away from default
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis: [jdk: openjdk6]')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
    }
    @Test public void test_pipelineGenerator_getStashMap_nonmatrix() {
        //automatically infer stashes from nonmatrix
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    foo: hello\n    artifacts: world\n    baz: goodbye')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['foo', 'artifacts']
        assert pipeline_generator.getStashMap() == [foo:[includes:'hello', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]], artifacts:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]], baz:[includes:'goodbye', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[:]]]
    }
    @Test public void test_pipelineGenerator_getStashMap_matrix() {
        //set matrix_axis away from default
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6')
        def pipeline_generator = new pipelineGenerator(generator)
        //for this matrix
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        //not for this matrix
        assert pipeline_generator.getStashMap([jdk: 'openjdk7']) == [:]
        //alternate for this matrix
        assert pipeline_generator.getStashMap([jdk: 'jdk0']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        //friendlyLabel for this matrix
        generator = new lifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_friendly.json');
        generator.loadToolchains(url.getFile())
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        assert pipeline_generator.getStashMap([jdk: 'jdk:openjdk6']) == [hello:[includes:'world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        assert pipeline_generator.getStashMap([jdk: 'jdk0']) == [:]
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis:\n        jdk: openjdk6\n  collect:\n    hello: goodbye_world')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'goodbye_world', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      matrix_axis:\n        jdk: openjdk6\n  collect:\n    hello: happy_days')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap([jdk: 'openjdk6']) == [hello:[includes:'happy_days', excludes:'', use_default_excludes:true, allow_empty:false, matrix_axis:[jdk: 'openjdk6']]]
    }
    @Test public void test_pipelineGenerator_getStashMap_test_for_failure() {
        //not a map
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - hello')
        def pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //name defined but empty
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: ""\n      includes: world')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //includes defined but empty
        generator.loadYamlString('language: java\njenkins:\n  stash:\n    - name: hello\n      includes: ""')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
        //is a matrix build but no matrix is defined
        generator.loadYamlString('language: java\njdk: [openjdk6, openjdk7]\njenkins:\n  stash:\n    - name: hello\n      includes: world\n      matrix_axis: {}')
        pipeline_generator = new pipelineGenerator(generator)
        assert pipeline_generator.getStashMap() == [:]
    }
    @Test public void test_pipelineGenerator_collect_settings_defaults_filtering() {
        generator.loadYamlString('language: java')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [
            '': ['planet': 'venus'],
            a: 'hello',
            b: ['planet': 'earth'],
            c: ['planet': 'mars'],
            d: [:]
        ]
        assert [b: [planet: 'earth'], c: [planet: 'mars']] == pipeline_generator.collect_settings_defaults
    }
    @Test public void test_pipelineGenerator_collect_settings_defaults_appending() {
        generator.loadYamlString('language: java')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.collect_settings_defaults = [a: ['planet': 'earth']]
        pipeline_generator.collect_settings_defaults = [b: ['planet': 'mars']]
        assert [a: [planet: 'earth'], b: [planet: 'mars']] == pipeline_generator.collect_settings_defaults
    }
    @Test public void test_pipelineGenerator_getPublishableItems_without_supported_collections() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts: hello')
        def pipeline_generator = new pipelineGenerator(generator)
        shouldFail(PipelineGeneratorException) {
            pipeline_generator.publishableItems
        }
    }
    @Test public void test_pipelineGenerator_getPublishableItems_should_filter_empty_collections() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts: ""')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert [] == pipeline_generator.getPublishableItems()
    }
    @Test public void test_pipelineGenerator_getPublishable_from_collect_settings_defaults() {
        generator.loadYamlString('language: java\njenkins:\n  collect:\n    artifacts:\n      - hello\n      - world')
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true, path: 'hello,world'] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_pipelineGenerator_getPublishable_from_collect_settings_defaults_customized() {
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
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        assert [allowEmptyArchive: false, caseSensitive: true, defaultExcludes: true, excludes: 'mars', onlyIfSuccessful: false, path: 'hello,world'] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_pipelineGenerator_getPublishable_from_collect_settings_defaults_filesets() {
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
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: '', onlyIfSuccessful: true]]
        pipeline_generator.collect_settings_filesets = [artifacts: ['excludes']]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'hello,mars', onlyIfSuccessful: true, path: 'hello,world'] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_pipelineGenerator_getPublishable_from_collect_settings_defaults_empty_filesets() {
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
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert 'hello,world' == pipeline_generator.getPublishable('artifacts')
        pipeline_generator.collect_settings_defaults = [artifacts: [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'mars', onlyIfSuccessful: true]]
        pipeline_generator.collect_settings_filesets = [artifacts: ['excludes']]
        assert [allowEmptyArchive: true, caseSensitive: false, defaultExcludes: false, excludes: 'mars', onlyIfSuccessful: true, path: 'hello,world'] == pipeline_generator.getPublishable('artifacts')
    }
    @Test public void test_pipelineGenerator_getPublishable_bug_undefined_collect() {
        String yaml = 'language: java'
        generator.loadYamlString(yaml)
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['artifacts']
        assert [] == pipeline_generator.publishableItems
    }
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation1() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: Only capitalization ending with a period.
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation2() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake:
            |      path: some/path
            |      validateme: 0123abcdef
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation_user_failure() {
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
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure1() {
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
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure2() {
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
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_with_collect_settings_validation_admin_failure3() {
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
        def pipeline_generator = new pipelineGenerator(generator)
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
    @Test public void test_pipelineGenerator_getPublishable_bug_invalid_default_fileset() {
        String yaml = '''
            |language: java
            |jenkins:
            |  collect:
            |    fake: some/path
        '''.stripMargin().trim()
        generator.loadYamlString(yaml)
        def pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.collect_settings_filesets = [fake: ['anotherpath']]
        pipeline_generator.collect_settings_defaults = [
            fake: [
                anotherpath: '**/*'
            ]
        ]
        assert '**/*' == pipeline_generator.getPublishable('fake')['anotherpath']
    }
}
