/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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
//the scmGitTest() class automatically sees the scmGit() class because they're in the same package
import net.gleske.jervis.exceptions.JervisException
import org.junit.After
import org.junit.Before
import org.junit.Test

class scmGitTest extends GroovyTestCase {
    def git
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        git = new scmGit('echo')
    }
    //tear down after every test
    @After protected void tearDown() {
        git = null
        super.tearDown()
    }
    @Test public void test_scmGit_setRoot() {
        assert 'echo' == git.mygit
    }
    //test getRoot()
    @Test public void test_scmGit_getRoot1() {
        git = null
        shouldFail(JervisException) {
            git = new scmGit('false')
        }
    }
    @Test public void test_scmGit_getRoot2() {
        git.git_root = 'some git root'
        assert 'some git root' == git.getRoot()
    }
    @Test public void test_scmGit_get_mygit() {
        assert 'echo' == git.mygit
    }
}
