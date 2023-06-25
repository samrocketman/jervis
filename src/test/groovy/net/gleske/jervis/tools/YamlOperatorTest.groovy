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
package net.gleske.jervis.tools
//the YamlOperatorTest() class automatically sees the YamlOperator class because they're in the same package

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.yaml.snakeyaml.error.MarkedYAMLException

class YamlOperatorTest extends GroovyAssert {
    @Rule public TemporaryFolder folder = new TemporaryFolder()
    @Test public void test_YamlOperator_fail_instantiation() {
        shouldFail(IllegalStateException) {
            new YamlOperator()
        }
    }
    @Test public void test_YamlOperator_writeObjToYaml() {
        String result = YamlOperator.writeObjToYaml(hello: 'world', me: ['a', 'b'])
        assert result == 'hello: world\nme:\n  - a\n  - b\n'
    }
    @Test public void test_YamlOperator_writeObjToYaml_unmodifiableMap() {
        Map response_headers = Collections.unmodifiableMap([(null): Collections.unmodifiableList(['HTTP/1.1 200 OK'])])
        String result = YamlOperator.writeObjToYaml(response_headers)
        assert result == 'null:\n  - HTTP/1.1 200 OK\n'
    }
    @Test public void test_YamlOperator_writeObjToYaml_loadYamlFrom_file() {
        File file = folder.newFile("sample.yaml")
        Map obj = [foo: 'bar', baz: ['hello', 'world']]
        YamlOperator.writeObjToYaml(file, obj)
        assert file.exists()
        assert file.text == 'foo: bar\nbaz:\n  - hello\n  - world\n'
        Map fromFile = YamlOperator.loadYamlFrom(file)
        assert fromFile == obj
        // Check not the same instance
        fromFile.foo = 'sun'
        assert fromFile.foo != obj.foo
    }
    @Test public void test_YamlOperator_loadYamlFrom_string() {
        String yaml = 'foo: bar\nbaz:\n  - hello\n  - world\n'
        Map obj = [foo: 'bar', baz: ['hello', 'world']]
        assert YamlOperator.loadYamlFrom(yaml) == obj
    }
    @Test public void test_YamlOperator_loadYamlFrom_stringwriter() {
        String yaml = 'foo: bar\nbaz:\n  - hello\n  - world\n'
        StringWriter sw = new StringWriter()
        Map obj = [foo: 'bar', baz: ['hello', 'world']]
        assert YamlOperator.loadYamlFrom(sw) == ''
        sw << yaml
        assert YamlOperator.loadYamlFrom(sw) == obj
    }
    @Test public void test_YamlOperator_loadYamlFrom_bytes() {
        String yaml = 'foo: bar\nbaz:\n  - hello\n  - world\n'
        Map obj = [foo: 'bar', baz: ['hello', 'world']]
        assert YamlOperator.loadYamlFrom(yaml.bytes) == obj
    }
    @Test public void test_YamlOperator_loadYamlFrom_stream() {
        String yaml = 'foo: bar\nbaz:\n  - hello\n  - world\n'
        Map obj = [foo: 'bar', baz: ['hello', 'world']]
        assert YamlOperator.loadYamlFrom(new ByteArrayInputStream(yaml.bytes)) == obj
    }
    @Test public void test_YamlOperator_loadYamlFrom_malicious1() {
        String yaml = '!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://localhost"]]]]'
        shouldFail(MarkedYAMLException) {
            YamlOperator.loadYamlFrom(yaml)
        }
    }
    @Test public void test_YamlOperator_loadYamlFrom_malicious2() {
        String yaml = 'somekey: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://localhost"]]]]'
        shouldFail(MarkedYAMLException) {
            YamlOperator.loadYamlFrom(yaml)
        }
    }
    @Test public void test_YamlOperator_loadYamlFrom_malicious3() {
        String yaml = 'somekey:\n  - !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://localhost"]]]]'
        shouldFail(MarkedYAMLException) {
            YamlOperator.loadYamlFrom(yaml)
        }
    }
    @Test public void test_YamlOperator_getObjectValue() {
        Map example = [key1: [subkey1: 'string']]
        assert 'string'.equals(generator.getObjectValue(example, 'key1.subkey1', 'default'))
        assert 'default'.equals(generator.getObjectValue(example, 'key2.subkey1', 'default'))
        assert 2.equals(generator.getObjectValue(example, 'key1.subkey1', 2))
    }
    @Test public void test_YamlOperator_getObjectValue_false_bug() {
        Map example = [key1: [subkey1: 'false']]
        assert false == generator.getObjectValue(example, 'key1.subkey1', false)
        assert false == generator.getObjectValue(example, 'key1.subkey1', true)
    }
    @Test public void test_YamlOperator_getObjectValue_type_bug() {
        Map example = [key1: [subkey1: 'string'],key2: ["a", "b"]]
        assert 'default'.equals(generator.getObjectValue(example, 'key1', 'default'))
        assert 'default'.equals(generator.getObjectValue(example, 'key2', 'default'))
    }
    @Test public void test_YamlOperator_getObjectValue_allow_conflicting_and_custom_lookup_paths() {
        Map hexKeys = [ 'hello.io': 'world', 'hello': ['jervis.io{': 'friend']]

        assert generator.getObjectValue(hexKeys, 'hello', 'hi') == 'hi'
        assert generator.getObjectValue(hexKeys, 'hello\\.io', '') == 'world'
        assert generator.getObjectValue(hexKeys, 'hello\\.io', []) == []
        assert generator.getObjectValue(hexKeys, 'hello.jervis.io{', '') == ''
        assert generator.getObjectValue(hexKeys, 'hello.jervis\\.io\\{', '') == 'friend'
    }
    @Test public void test_YamlOperator_getObjectValue_fallbacks_and_multiple_defaults() {
        Map json = [
            python: ['2.7': ['hello', 'world']],
            type: [hello: 'world'],
            friend: 'true',
            bye: 'false'
        ]

        assert generator.getObjectValue(json, 'type // type.hello // python.2\\.7', ['', []]) == 'world'
        assert generator.getObjectValue(json, 'type // type.hello // python.2\\.7', [[], '']) == 'world'
        assert generator.getObjectValue(json, 'type.hello // python.2\\.7', [[], '']) == 'world'
        assert generator.getObjectValue(json, 'type.hello // python.2\\.7', ['', []]) == 'world'
        assert generator.getObjectValue(json, 'python.2\\.7 // type.hello', [[], '']) == ['hello', 'world']
        assert generator.getObjectValue(json, 'python.2\\.7 // type.hello', ['', []]) == ['hello', 'world']

        // no fallback but multiple defaults
        assert generator.getObjectValue(json, 'python.2\\.7', ['', []]) == ['hello', 'world']
        assert generator.getObjectValue(json, 'python.2\\.7', [[], '']) == ['hello', 'world']

        // fallbacks with single default
        assert generator.getObjectValue(json, 'type // type.hello // python.2\\.7', '') == 'world'
        assert generator.getObjectValue(json, 'type // type.hello // python.2\\.7', []) == ['hello', 'world']
        assert generator.getObjectValue(json, 'type.hello // python.2\\.7', true) == true
        assert generator.getObjectValue(json, 'type.hello // python.2\\.7', false) == true
        assert generator.getObjectValue(json, 'type.hello // python.2\\.7', [:]) == [:]
        assert generator.getObjectValue(json, 'friend', true) == true
        assert generator.getObjectValue(json, 'bye', true) == false
        assert generator.getObjectValue(json, 'friend', false) == true
        assert generator.getObjectValue(json, 'bye', false) == false
        assert generator.getObjectValue(json, 'bye', [[], [:]]) == []
        assert generator.getObjectValue(json, 'bye', [[:], []]) == [:]
        assert generator.getObjectValue(json, 'friend // bye', [[:], []]) == [:]
        assert generator.getObjectValue(json, 'bye // friend', [[:], []]) == [:]
        assert generator.getObjectValue(json, 'friend // bye', [[], [:]]) == []
        assert generator.getObjectValue(json, 'bye // friend', [[], [:]]) == []
    }
    @Test public void test_YamlOperator_getObjectValue_quoted_periods() {
        Map json = [
            python: ['2.7': ['hello', 'world']],
            type: [hello: 'world']
        ]

        def benchmark = generator.getObjectValue(json, 'python.2\\.7', [])
        assert benchmark == ['hello', 'world']
        assert benchmark == generator.getObjectValue(json, 'python."2\\.7"', [])
        assert benchmark == generator.getObjectValue(json, 'python."2.7"', [])
    }
    @Test public void test_YamlOperator_getObjectValue_groovydoc_examples() {
        Map hexKeys = [
            'hello.io': 'world',
            hello: ['jervis.io{': 'friend'],
            friend: [name: 'dog']
        ]

        assert generator.getObjectValue(hexKeys, 'friend.name', '') == 'dog'
        assert generator.getObjectValue(hexKeys, 'friend.name', ['item']) == 'dog'
        assert generator.getObjectValue(hexKeys, 'friend.name', [['item']]) == ['item']
        assert generator.getObjectValue(hexKeys, 'hello\\.io', '') == 'world'
        assert generator.getObjectValue(hexKeys, 'hello%{2e}io', '') == 'world'
        assert generator.getObjectValue(hexKeys, 'hello.jervis\\.io\\{', '') == 'friend'
        assert generator.getObjectValue(hexKeys, '"hello.io"', '') == 'world'
        assert generator.getObjectValue(hexKeys, 'hello."jervis.io"{', '') == 'friend'
        assert generator.getObjectValue(hexKeys, 'hello."jervis.io"{ // friend', [:]) == [name: 'dog']
        assert generator.getObjectValue(hexKeys, 'hello', [ '', [] ]) == ''
        assert generator.getObjectValue(hexKeys, 'hello // friend.name', [ '', [] ]) == 'dog'
    }
}
