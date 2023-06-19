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
//the GitHubAppCredentialTest() class automatically sees the GitHubAppCredential() class because they're in the same package

import net.gleske.jervis.exceptions.GitHubAppException

import org.junit.After
import org.junit.Before
import org.junit.Test

class GitHubAppCredentialTest extends GroovyTestCase {

    GitHubAppCredential app
    GitHubAppRsaCredentialImpl rsaCred
    EphemeralTokenCache tokenCred
    String hash = '7bb84c7e30164139b00e7f95fd0e801bfdfe190f7ffca08cc77ae0d8438be02b'

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        // use in-memory token cache
        this.tokenCred = new EphemeralTokenCache(true)
        this.tokenCred.loadCache = null
        this.tokenCred.saveCache = null
        this.tokenCred.obtainLock = null
        // fake rsa app
        this.rsaCred = new GitHubAppRsaCredentialImpl('173962', this.getClass().getResource('/rsa_keys/good_id_rsa_2048').content.text)
        this.app = new GitHubAppCredential(rsaCred, tokenCred)
    }
    //tear down after every test
    @After protected void tearDown() {
        this.rsaCred = null
        this.tokenCred = null
        this.app = null
        super.tearDown()
    }
    @Test public void test_GitHubAppCredential_hash_consistency() {
        assert app.hash == hash
        app.rsaCredential = rsaCred
        assert app.hash == hash
        app.scope = [:]
        assert app.hash == hash
    }
    @Test public void test_GitHubAppCredential_hash_update() {
        assert app.hash == hash
        String hash1 = app.hash
        app.scope = [foo: 'bar']
        String hash2 = app.hash
        assert hash2 != hash1
        rsaCred.owner = 'some-owner'
        app.rsaCredential =  rsaCred
        String hash3 = app.hash
        assert hash3 != hash2

        rsaCred.owner = ''
        app.scope = [:]
        app.rsaCredential = rsaCred
        assert app.hash == hash
    }
    @Test public void test_GitHubAppCredential_hash_recalculate() {
        assert app.hash == hash
        rsaCred.owner = 'another-owner'
        assert app.hash == hash
        app.hash = ''
        assert app.hash != hash
        rsaCred.owner = ''
        app.hash = null
        assert app.hash == hash
        shouldFail(GitHubAppException) {
            app.hash = 'overwrite hash'
        }
    }
}
