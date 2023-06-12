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
    String preCalculatedId

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        this.cred = new GitHubAppRsaCredentialImpl('some app', 'fake private key')
        this.preCalculatedId = this.cred.id
    }
    //tear down after every test
    @After protected void tearDown() {
        this.cred = null
        this.preCalculatedId = null
        super.tearDown()
    }
    @Test public void test_GitHubAppRsaCredentialImpl_basic_checks() {
        assert cred.apiUri == GitHubAppCredential.DEFAULT_GITHUB_API
        assert cred.owner == ''
        assert cred.appID == 'some app'
        assert cred.privateKey == 'fake private key'
        assert cred.id == preCalculatedId
    }
    @Test public void test_GitHubAppRsaCredentialImpl_key_closure() {
        assert cred.id == preCalculatedId
        cred.resolvePrivateKey = {-> 'another key' }
        assert cred.id == '1119abc281c15bab6071494ddbdac304b0f2b29ba2c693e37b15f2a900a01274'
    }
    @Test public void test_GitHubAppRsaCredentialImpl_key_closure_constructor() {
        this.cred = new GitHubAppRsaCredentialImpl('some app', {-> 'another key' })
        assert cred.id == '1119abc281c15bab6071494ddbdac304b0f2b29ba2c693e37b15f2a900a01274'
    }
    @Test public void test_GitHubAppRsaCredentialImpl_owner() {
        this.cred.owner = 'some owner'
        assert cred.id != preCalculatedId
        assert this.cred.owner == 'some owner'
    }
    @Test public void test_GitHubAppRsaCredentialImpl_apiUri() {
        this.cred = new GitHubAppRsaCredentialImpl('some app', 'fake private key', 'https://ghe.example.com/api/v3')
        assert this.cred.id != preCalculatedId
        assert this.cred.apiUri == 'https://ghe.example.com/api/v3'
    }
    @Test public void test_GitHubAppRsaCredentialImpl_apiUri_closure() {
        this.cred = new GitHubAppRsaCredentialImpl('some app', {-> 'another key' }, 'https://ghe.example.com/api/v3')
        assert this.cred.id == 'a34f6591ea71f5c96eb70c12df04ce78269deb793b901bc8bb3ea7b0df9cb2f2'
        assert this.cred.apiUri == 'https://ghe.example.com/api/v3'
    }
}
