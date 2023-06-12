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
package net.gleske.jervis.remotes.creds
//the GitHubAppRsaCredentialImplTest() class automatically sees the GitHubAppRsaCredentialImpl() class because they're in the same package

import net.gleske.jervis.remotes.creds.GitHubAppCredential

import org.junit.After
import org.junit.Before
import org.junit.Test

class GitHubAppRsaCredentialImplTest extends GroovyTestCase {


    GitHubAppRsaCredentialImpl cred

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        // setup
        this.cred = new GitHubAppRsaCredentialImpl('some app', 'fake private key')
    }
    //tear down after every test
    @After protected void tearDown() {
        // tear down
        super.tearDown()
    }
    @Test public void test_GitHubAppRsaCredentialImpl_basic_checks() {
        assert cred.apiUri == GitHubAppCredential.DEFAULT_GITHUB_API
        assert cred.owner == ''
        assert cred.appID == 'some app'
        assert cred.privateKey == 'fake private key'
        assert cred.id == 'a1796c2d4cf34fb91f027fe47243190061532ebecfc921b4ccfe72a2ffa2f0e8'
    }
    @Test public void test_GitHubAppRsaCredentialImpl_key_closure() {
        assert cred.id == 'a1796c2d4cf34fb91f027fe47243190061532ebecfc921b4ccfe72a2ffa2f0e8'
        cred.resolvePrivateKey = {-> 'another key' }
        assert cred.id == '1119abc281c15bab6071494ddbdac304b0f2b29ba2c693e37b15f2a900a01274'
    }
}
