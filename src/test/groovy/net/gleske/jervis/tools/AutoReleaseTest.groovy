/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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
//the AutoReleaseTest() class automatically sees the securityIO() class because they're in the same package
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
        String date = new Date().format('YYYMMdd')

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
}
