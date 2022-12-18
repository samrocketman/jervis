/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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
}
