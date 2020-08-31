/*
   Copyright 2014-2020 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.remotes
//the VaultServiceTest() class automatically sees the VaultService() class because they're in the same package
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.interfaces.VaultCredential
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

class VaultServiceTest extends GroovyTestCase {
    def myvault
    def url
    Map request_meta = [:]

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256')
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        myvault = new VaultService('http://active.vault.service.consul:8200', cred)
    }
    //tear down after every test
    @After protected void tearDown() {
        myvault = null
        request_meta = [:]
        super.tearDown()
    }
    @Test public void test_VaultService_newInstance_and_baseUrl() {
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        List urls = [
            'http://active.vault.service.consul:8200',
            'http://active.vault.service.consul:8200/',
            'http://active.vault.service.consul:8200/v1',
            'http://active.vault.service.consul:8200/v1/'
        ]
        urls.each { String vault_url ->
            myvault = new VaultService(vault_url, cred)
            assert myvault.baseUrl() == 'http://active.vault.service.consul:8200/v1/'
            assert myvault.credential instanceof TokenCredential
            assert myvault.header() == ['X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
        }
        urls.each { String vault_url ->
            VaultCredential vault_cred = [getVault_url: {-> vault_url }, getToken: {-> 'fake-token2' }] as VaultCredential
            myvault = new VaultService(vault_cred)
            assert myvault.baseUrl() == 'http://active.vault.service.consul:8200/v1/'
            assert myvault.credential instanceof VaultCredential
            assert myvault.header() == ['X-Vault-Token': 'fake-token2', 'X-Vault-Request': 'true']
        }
    }
    @Test public void test_VaultService_headers() {
        assert myvault.headers == [:]
        assert myvault.header() == ['X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
        myvault.headers = [foo: 'bar']
        assert myvault.headers == [foo: 'bar']
        assert myvault.header() == [foo: 'bar', 'X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
        myvault.headers = ['X-Vault-Token': 'hacked', 'X-Vault-Request': 'false']
        assert myvault.headers == ['X-Vault-Token': 'hacked', 'X-Vault-Request': 'false']
        assert myvault.header() == ['X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
    }
    @Test public void test_VaultService_getSecret_kv_v1() {
        assert myvault.getSecret('secret/foo') == [test: 'data']
        assert myvault.getSecret('secret/foo/bar') == [someother: 'data']
        assert myvault.getSecret('secret/foo/bar/baz') == [more: 'secrets']
    }
    @Test public void test_VaultService_getSecret_kv_v2() {
        assert myvault.getSecret('kv/foo') == [another: 'secret', hello: 'world']
        assert myvault.getSecret('kv/foo/bar') == [hello: 'friend']
        assert myvault.getSecret('kv/foo/bar/baz') == [foo: 'bar']
    }
    @Test public void test_VaultService_getSecret_kv_v2_older_version_1() {
        assert myvault.getSecret('kv/foo', 1) == [hello: 'world']
    }
}
