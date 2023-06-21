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

import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl
import net.gleske.jervis.exceptions.GitHubAppException

import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
  This class tests the <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential}</tt>
  class.  This uses auto-generated mock data using real API responses.

  <h2>Generate Mock Data</h2>

  Mock data has already been generated.  This is the script which captured mock
  data.

<pre><code>
import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
import net.gleske.jervis.remotes.SimpleRestServiceSupport

if(!binding.hasVariable('url')) {
    String persistStr
    url = persistStr
}
if(binding.hasVariable('request_meta')) {
    request_meta.clear()
} else {
    request_meta = [:]
}

if(binding.hasVariable('request_history')) {
    request_history.clear()
} else {
    request_history = []
}

// Record URL API data to files as mock data
recordMockUrls(url, URL, request_meta, true, 'SHA-256', request_history)

import net.gleske.jervis.remotes.creds.EphemeralTokenCache
import net.gleske.jervis.remotes.creds.GitHubAppCredential
import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl
import net.gleske.jervis.tools.YamlOperator

import java.time.Instant
import java.time.temporal.ChronoUnit


// Configure the private key downloaded from GitHub App.
GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl('173962', new File('github-app.pem').text)
rsaCred.owner = 'sgleske-test'

// Configure in-memory token storage
EphemeralTokenCache tokenCred = new EphemeralTokenCache(true)
tokenCred.loadCache = null
tokenCred.saveCache = null
tokenCred.obtainLock = null

GitHubAppCredential app = new GitHubAppCredential(rsaCred, tokenCred)

app.token
app.token

rsaCred.owner = 'samrocketman'
app.hash = ''
app.ownerIsUser = true
app.installation_id = null
app.token

// SANITIZE sensitive information and reduce mock data
// update mock data to represent reponses 500 years in the future to simplify mocking responses
request_history.each { Map request -&gt;
    Map response = YamlOperator.loadYamlFrom(request.response)
    Boolean updateFile = false
    if(response.token) {
        response.token = 'some-token'
        updateFile = true
    }
    if(response.expires_at) {
        Long fiveHundredYearsInDays = 182500
        response.expires_at = Instant.now().plus(fiveHundredYearsInDays, ChronoUnit.DAYS).toString()
        updateFile = true
    }
    if(response?.app_id &amp;&amp; response?.account?.login) {
        response = [id: response.id]
        updateFile = true
    }
    if(updateFile) {
        request.response = YamlOperator.writeObjToYaml(response)
        YamlOperator.writeObjToYaml((new File(request.mock_file)), response)
    }
}

null
</code></pre>
  */
class GitHubAppCredentialTest extends GroovyTestCase {
    String hash = '7bb84c7e30164139b00e7f95fd0e801bfdfe190f7ffca08cc77ae0d8438be02b'
    GitHubAppCredential app
    GitHubAppRsaCredentialImpl rsaCred
    EphemeralTokenCache tokenCred

    // mock tracking variables
    def myvault
    def url
    Map request_meta = [:]
    List request_history = []
    Map custom_responses = [(null): null]
    List metaResult() {
        [request_history*.url.inspect(), request_history*.method.inspect(), request_history*.data.inspect(), request_history*.response_code.inspect()]
    }

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        // mock network
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256', request_history, custom_responses)
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
        request_meta.clear()
        request_history.clear()
        custom_responses.clear()
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
    @Test public void test_GitHubAppCredential_getToken_user() {
        rsaCred.owner = 'samrocketman'
        app.ownerIsUser = true
        app.hash = ''

        assert app.jwtToken == null
        app.token == 'some-token'
        assert app.jwtToken != null

        List urls = ['https://api.github.com/users/samrocketman/installation', 'https://api.github.com/app/installations/38741780/access_tokens']
        List methods = ['GET', 'POST']
        assert request_history*.url == urls
        assert request_history*.method == methods
    }

    @Test public void test_GitHubAppCredential_getToken_org_cached() {
        rsaCred.owner = 'sgleske-test'
        app.hash = ''

        assert app.jwtToken == null
        // should populate urls
        assert app.token == 'some-token'
        assert app.jwtToken != null
        // check against cached version (no additional urls)
        assert app.token == 'some-token'

        List urls = ['https://api.github.com/orgs/sgleske-test/installation', 'https://api.github.com/app/installations/32854008/access_tokens']
        List methods = ['GET', 'POST']
        assert request_history*.url == urls
        assert request_history*.method == methods
    }
    @Test public void test_GitHubAppCredential_getToken_org_expired() {
        rsaCred.owner = 'sgleske-test'
        app.hash = ''
        // pre-populate cache with a good token
        app.@tokenCredential.@cache = [
            (app.hash): [
                token: 'good-token',
                expires_at: '2523-02-18T22:51:47.533293Z',
                renew_buffer: 30
            ]
        ]

        // return good cached token instead of getting from network
        assert app.token == 'good-token'
        assert request_history*.url == []
        assert request_history*.method == []

        // intentionally expire the token
        app.@tokenCredential.@cache[app.hash]['expires_at'] = Instant.now().toString()

        // check for expired token renewal
        assert app.token == 'some-token'
        List urls = ['https://api.github.com/orgs/sgleske-test/installation', 'https://api.github.com/app/installations/32854008/access_tokens']
        List methods = ['GET', 'POST']
        assert request_history*.url == urls
        assert request_history*.method == methods
    }
    @Test public void test_GitHubAppCredential_installation_id() {
        assert app.@installation_id == null
        assert app.installation_id == '38741780'
        List urls = ['https://api.github.com/app/installations']
        List methods = ['GET']
        assert request_history*.url == urls
        assert request_history*.method == methods
    }
    @Test public void test_GitHubAppCredential_installation_id_preset() {
        app.installation_id = 'foo'
        assert app.installation_id == 'foo'
        // no network communication due to preset
        List urls = []
        List methods = []
        assert request_history*.url == urls
        assert request_history*.method == methods
    }
    @Test public void test_GitHubAppCredential_installation_id_fail() {
        custom_responses.put(
            'api.github.com_app_installations_94cd1485322def4aff1733b599cf889ac1b65536a13aaa7e19963fa6c6cdd344',
            'api.github.com_app_installations_empty')
        assert app.@installation_id == null
        shouldFail(GitHubAppException) {
            app.installation_id
        }
        assert app.@installation_id == null
        List urls = ['https://api.github.com/app/installations']
        List methods = ['GET']
        assert request_history*.url == urls
        assert request_history*.method == methods
    }
}
