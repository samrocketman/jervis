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
//the AutoReleaseTest() class automatically sees the AutoRelease class because they're in the same package
import net.gleske.jervis.exceptions.JervisException

import java.text.SimpleDateFormat
import org.junit.After
import org.junit.Before
import org.junit.Test

class AutoReleaseTest extends GroovyTestCase {
    @Test public void test_AutoRelease_fail_instantiation() {
        shouldFail(IllegalStateException) {
            new AutoRelease()
        }
    }
    @Test public void test_AutoRelease_getNextRelease_date_hotfix_prefix() {
        //Date.format is not relably future-proof https://issues.apache.org/jira/browse/GROOVY-9709
        //String date = new Date().format('YYYMMdd')
        String date = new SimpleDateFormat('YYYYMMdd').format(new Date())

        assert "v${date}-1" == AutoRelease.getNextRelease(date, ['foo'], '-', 'v')
        assert "v${date}-2" == AutoRelease.getNextRelease(date, ['foo', "v${date}-1"], '-', 'v')
        assert "v${date}-3" == AutoRelease.getNextRelease(date, ['foo', "v${date}-1", "v${date}-2"], '-', 'v')
    }
    // cover every test case from the documentation for getNextRelease
    @Test public void test_AutoRelease_getNextRelease_1_0_snapshot_defaults() {
        assert '1.3' == AutoRelease.getNextRelease('1.0-SNAPSHOT', ['1.1', '1.2'])
    }
    @Test public void test_AutoRelease_getNextRelease_1_1_5_defaults() {
        assert '1.1.5.3' == AutoRelease.getNextRelease('1.1.5', ['1.1.6', '1.1.5.1', '1.1.5.2'])
    }
    @Test public void test_AutoRelease_getNextRelease_2_1_snapshot_prefix() {
        assert 'client-2.1.3' == AutoRelease.getNextRelease('2.1-SNAPSHOT', ['client-2.1.1', 'client-2.1.2'], '.', 'client-')
    }
    @Test public void test_AutoRelease_getNextRelease_1_3_prefix() {
        assert 'v1.3.4' == AutoRelease.getNextRelease('1.3', ['v1.1.1', 'v1.2.1', 'v1.3.1', 'v1.3.2', 'v1.3.3'], '.', 'v')
    }
    @Test public void test_AutoRelease_getNextRelease_1_3_0_prefix() {
        assert 'v1.3.4' == AutoRelease.getNextRelease('1.3.0', ['v1.1.1', 'v1.2.1', 'v1.3.1', 'v1.3.2', 'v1.3.3'], '.', 'v')
    }
    @Test public void test_AutoRelease_getNextRelease_1_0_prefix() {
        assert 'v1.4' == AutoRelease.getNextRelease('1.0', ['v1.1', 'v1.2', 'v1.3', 'v1.3.2', 'v1.3.3'], '.', 'v')
    }
    @Test public void test_AutoRelease_getNextRelease_20200101_hotfix() {
        assert '20200101-3' == AutoRelease.getNextRelease('20200101', ['20200101-1', '20200101-2'], '-')
    }
    @Test public void test_AutoRelease_getNextRelease_1_0_beta_prefix_hotfix() {
        assert 'v1.0-beta-1' == AutoRelease.getNextRelease('1.0-beta', [], '-', 'v')
    }
    @Test public void test_AutoRelease_getNextRelease_1_0_rc_prefix_hotfix() {
        assert 'v1.0-rc-3' == AutoRelease.getNextRelease('1.0-rc', ['v1.0-rc-1', 'v1.0-rc-2'], '-', 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_fail_nonsemantic() {
        shouldFail(JervisException) {
            AutoRelease.getNextSemanticRelease('1.0', ['1.1', '1.2', '1.3'], '-')
        }
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_0_1_0_defaults() {
        assert '0.1.3' == AutoRelease.getNextSemanticRelease('0.1.0', ['0.1.1', '0.1.2'])
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_1_5_first_hotfix() {
        assert '1.1.5-1' == AutoRelease.getNextSemanticRelease('1.1.5', ['1.1.5', '1.1.6'])
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_1_5_multi_hotfix() {
        assert '1.1.5-3' == AutoRelease.getNextSemanticRelease('1.1.5', ['1.1.6', '1.1.5-1', '1.1.5-2'])
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_2_1_0_prefix() {
        assert 'client-2.1.3' == AutoRelease.getNextSemanticRelease('2.1.0', ['client-2.1.1', 'client-2.1.2'], 'client-')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_3_0_prefix() {
        assert 'v1.3.4' == AutoRelease.getNextSemanticRelease('1.3.0', ['v1.1.1', 'v1.2.1', 'v1.3.1', 'v1.3.2', 'v1.3.3'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_3_2_prefix_first_hotfix() {
        assert 'v1.3.2-1' == AutoRelease.getNextSemanticRelease('1.3.2', ['v1.1.1', 'v1.2.1', 'v1.3.1', 'v1.3.2', 'v1.3.3'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_3_2_prefix_multi_hotfix() {
        assert 'v1.3.2-3' == AutoRelease.getNextSemanticRelease('1.3.2', ['v1.3.2', 'v1.3.3', 'v1.3.2-1', 'v1.3.2-2'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_3_2_prefix_nested_hotfix() {
        assert 'v1.3.2-1-1' == AutoRelease.getNextSemanticRelease('1.3.2-1', ['v1.3.2', 'v1.3.3', 'v1.3.2-1', 'v1.3.2-2'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_0_0_rc_prefix() {
        assert 'v1.0.0-rc-2' == AutoRelease.getNextSemanticRelease('1.0.0-rc', ['v0.10.3', 'v1.0.0-rc-1'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_1_0_beta_prefix_first_release() {
        assert 'v1.1.0-beta-1' == AutoRelease.getNextSemanticRelease('1.1.0-beta', ['0.10.3', 'v1.0.0-rc-1'], 'v')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_1_1_0_beta_prefix_multi_release() {
        assert '1.1.0-beta-4' == AutoRelease.getNextSemanticRelease('1.1.0-beta', ['1.1.0-beta-1', '1.1.0-beta-2', '1.1.0-beta-3'])
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_non_semantic_beta_failure() {
        shouldFail(JervisException) {
            AutoRelease.getNextSemanticRelease('1.0-beta', [])
        }
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_isMatched() {
        assert false == AutoRelease.isMatched('hello', 'world')
        assert true == AutoRelease.isMatched('hello', 'hello')
        assert false == AutoRelease.isMatched('/hello', '/world')
        assert true == AutoRelease.isMatched('/hello', '/hello')
        assert false == AutoRelease.isMatched('hello/', 'world/')
        assert true == AutoRelease.isMatched('hello/', 'hello/')
        assert true == AutoRelease.isMatched('/[0-9]+/', '8675309')
        assert false == AutoRelease.isMatched('/[0-9]+/', 'jenny')
    }
    @Test public void test_AutoRelease_getNextSemanticRelease_getScriptFromTemplate() {
        assert 'goodbye friend' == AutoRelease.getScriptFromTemplate('${hello} ${world}', [hello: 'goodbye', world: 'friend'])
        assert 'animals: dog cat snake' == AutoRelease.getScriptFromTemplate('animals:<% animals.each { print " ${it}" } %>', [animals: ['dog', 'cat', 'snake']])
    }
}
