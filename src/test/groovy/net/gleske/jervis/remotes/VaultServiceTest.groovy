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
import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.interfaces.VaultCredential
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

class VaultServiceTest extends GroovyTestCase {
    def myvault
    def url
    Map request_meta = [:]
    List request_history = []
    private static String DEFAULT_VAULT_URL = 'http://active.vault.service.consul:8200/v1/'

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256', request_history)
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        myvault = new VaultService(DEFAULT_VAULT_URL, cred)
    }
    //tear down after every test
    @After protected void tearDown() {
        myvault = null
        request_meta = [:]
        request_history = []
        super.tearDown()
    }
    @Test public void test_VaultService_newInstance_and_baseUrl() {
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        List urls = [
            'http://active.vault.service.consul:8200',
            'http://active.vault.service.consul:8200/',
            'http://active.vault.service.consul:8200/v1',
            DEFAULT_VAULT_URL
        ]
        urls.each { String vault_url ->
            myvault = new VaultService(vault_url, cred)
            assert myvault.baseUrl() == DEFAULT_VAULT_URL
            assert myvault.credential instanceof TokenCredential
            assert myvault.header() == ['X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
        }
        urls.each { String vault_url ->
            VaultCredential vault_cred = [getVault_url: {-> vault_url }, getToken: {-> 'fake-token2' }] as VaultCredential
            myvault = new VaultService(vault_cred)
            assert myvault.baseUrl() == DEFAULT_VAULT_URL
            assert myvault.credential instanceof VaultCredential
            assert myvault.header() == ['X-Vault-Token': 'fake-token2', 'X-Vault-Request': 'true']
        }
    }
    @Test public void test_VaultService_headers_and_header_method() {
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
    @Test public void test_VaultService_discover_mount_versions() {
        assert myvault.mountVersions == [:]
        myvault.getSecret('secret/foo')
        myvault.getSecret('kv/foo')
        assert myvault.mountVersions == [kv: '2', secret: '1']
    }
    @Test public void test_VaultService_discover_mount_version_v1() {
        assert myvault.mountVersions == [:]
        myvault.getSecret('secret/foo')
        assert myvault.mountVersions == [secret: '1']
    }
    @Test public void test_VaultService_discover_mount_version_v2() {
        assert myvault.mountVersions == [:]
        myvault.getSecret('kv/foo')
        assert myvault.mountVersions == [kv: '2']
    }
    @Test public void test_VaultService_getSecret_map_kv_v1() {
        assert myvault.getSecret(mount: 'secret', path: 'foo') == [test: 'data']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar') == [someother: 'data']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar/baz') == [more: 'secrets']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar/baz', dont_care: 'value') == [more: 'secrets']
        shouldFail(JervisException) {
            myvault.getSecret([:])
        }
        shouldFail(JervisException) {
            myvault.getSecret(mount: 'secret')
        }
        shouldFail(JervisException) {
            myvault.getSecret(path: 'foo/bar/baz')
        }
    }
    @Test public void test_VaultService_getSecret_map_kv_v2() {
        assert myvault.getSecret(mount: 'kv', path: 'foo') == [another: 'secret', hello: 'world']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar') == [hello: 'friend']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar/baz') == [foo: 'bar']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar/baz', dont_care: 'value') == [foo: 'bar']
        shouldFail(JervisException) {
            myvault.getSecret([:])
        }
        shouldFail(JervisException) {
            myvault.getSecret(mount: 'kv')
        }
        shouldFail(JervisException) {
            myvault.getSecret(path: 'foo/bar/baz')
        }
    }
    @Test public void test_VaultService_getSecret_map_kv_v2_older_version_1() {
        assert myvault.getSecret(mount: 'kv', path: 'foo', 1) == [hello: 'world']
        shouldFail(JervisException) {
            myvault.getSecret([:], 1)
        }
        shouldFail(JervisException) {
            myvault.getSecret(mount: 'kv', 1)
        }
        shouldFail(JervisException) {
            myvault.getSecret(path: 'foo', 1)
        }
    }
    //start
    @Test public void test_VaultService_discover_mount_versions_getSecret_map() {
        assert myvault.mountVersions == [:]
        myvault.getSecret(mount: 'secret', path: 'foo')
        myvault.getSecret(mount: 'kv', path: 'foo')
        assert myvault.mountVersions == [kv: '2', secret: '1']
    }
    @Test public void test_VaultService_discover_mount_version_v1_getSecret_map() {
        assert myvault.mountVersions == [:]
        myvault.getSecret(mount: 'secret', path: 'foo')
        assert myvault.mountVersions == [secret: '1']
    }
    @Test public void test_VaultService_discover_mount_version_v2_getSecret_map() {
        assert myvault.mountVersions == [:]
        myvault.getSecret(mount: 'kv', path: 'foo')
        assert myvault.mountVersions == [kv: '2']
    }
    @Test public void test_VaultService_setMountVersions_String() {
        myvault.setMountVersions('kv', '2')
        assert myvault.mountVersions == [kv: '2']
        myvault.setMountVersions('secret', '1')
        assert myvault.mountVersions == [kv: '2', secret: '1']
        shouldFail(JervisException) {
            myvault.setMountVersions('secret', 1)
        }
        shouldFail(JervisException) {
            myvault.setMountVersions('kv', 2)
        }
        shouldFail(JervisException) {
            myvault.setMountVersions('another', 'hello')
        }
    }
    @Test public void test_VaultService_setMountVersions_Map() {
        myvault.mountVersions = [kv: '2']
        assert myvault.mountVersions == [kv: '2']
        myvault.mountVersions = [secret: '1']
        assert myvault.mountVersions == [kv: '2', secret: '1']
        shouldFail(JervisException) {
            myvault.mountVersions = [secret: 1]
        }
        shouldFail(JervisException) {
            myvault.mountVersions = [kv: 2]
        }
        shouldFail(JervisException) {
            myvault.mountVersions = [another: 'hello']
        }
        shouldFail(JervisException) {
            myvault.mountVersions = [kv: '2', secret: '1', another: 'hello']
        }
    }
    @Test public void test_VaultService_findAllKeys_v1() {
        assert myvault.findAllKeys('secret') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/', 1) == ['secret/foo']
        assert myvault.findAllKeys('secret/', 2) == ['secret/foo', 'secret/foo/bar']
        assert myvault.findAllKeys('secret/', 3) == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2() {
        assert myvault.findAllKeys('kv') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/', 1) == ['kv/foo']
        assert myvault.findAllKeys('kv/', 2) == ['kv/foo', 'kv/foo/bar']
        assert myvault.findAllKeys('kv/', 3) == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
    }
    @Test public void test_VaultService_listPath_v1() {
        assert myvault.listPath('secret') == ['foo', 'foo/']
        assert myvault.listPath('secret/') == ['foo', 'foo/']
        assert myvault.listPath('secret/foo') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/bar') == ['baz']
        assert myvault.listPath('secret/foo/bar/') == ['baz']
        shouldFail(Exception) {
            myvault.listPath('secret/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_listPath_v2() {
        assert myvault.listPath('kv') == ['foo', 'foo/']
        assert myvault.listPath('kv/') == ['foo', 'foo/']
        assert myvault.listPath('kv/foo') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/bar') == ['baz']
        assert myvault.listPath('kv/foo/bar/') == ['baz']
        shouldFail(Exception) {
            myvault.listPath('kv/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_copySecret_v1_to_v2() {
        myvault.mountVersions = [kv: '2', secret: '1']
        myvault.copySecret('secret/foo', 'kv/foo')
        assert request_history[0].url == 'http://active.vault.service.consul:8200/v1/secret/foo'
        assert request_history[0].method == 'GET'
        assert request_history[1].url == 'http://active.vault.service.consul:8200/v1/kv/metadata/foo'
        assert request_history[1].method == 'GET'
        assert request_history[2].url == 'http://active.vault.service.consul:8200/v1/kv/data/foo'
        assert request_history[2].method == 'POST'
        assert request_history[2].data.toString() == '{"data":{"test":"data"},"options":{"cas":2}}'
    }
    @Test public void test_VaultService_copySecret_v2_to_v1() {
        myvault.mountVersions = [kv: '2', secret: '1']
        myvault.copySecret('kv/foo', 'secret/foo')
        assert request_history[0].url == 'http://active.vault.service.consul:8200/v1/kv/data/foo?version=0'
        assert request_history[0].method == 'GET'
        assert request_history[1].url == 'http://active.vault.service.consul:8200/v1/secret/foo'
        assert request_history[1].method == 'POST'
        assert request_history[1].data.toString() == '{"another":"secret","hello":"world"}'
    }
    @Test public void test_VaultService_setSecret_v1() {
        myvault.mountVersions = [secret: '1']
        myvault.setSecret('secret/foo', [another: 'secret', hello: 'world'])
    }
    @Test public void test_VaultService_copySecret_v2_to_v1_version_1() {
        myvault.mountVersions = [kv: '2', secret: '1']
        myvault.copySecret('kv/foo', 'secret/foo', 1)
        assert request_history[0].url == 'http://active.vault.service.consul:8200/v1/kv/data/foo?version=1'
        assert request_history[0].method == 'GET'
        assert request_history[1].url == 'http://active.vault.service.consul:8200/v1/secret/foo'
        assert request_history[1].method == 'POST'
        assert request_history[1].data.toString() == '{"hello":"world"}'
    }
    @Test public void test_VaultService_copySecret_v2_to_v2() {
        myvault.mountVersions = [kv: '2', secret: '1']
        myvault.copySecret('kv/foo', 'kv/foo/bar')
        assert request_history[0].url == 'http://active.vault.service.consul:8200/v1/kv/data/foo?version=0'
        assert request_history[0].method == 'GET'
        assert request_history[1].url == 'http://active.vault.service.consul:8200/v1/kv/metadata/foo/bar'
        assert request_history[1].method == 'GET'
        assert request_history[2].url == 'http://active.vault.service.consul:8200/v1/kv/data/foo/bar'
        assert request_history[2].method == 'POST'
        assert request_history[2].data.toString() == '{"data":{"another":"secret","hello":"world"},"options":{"cas":1}}'
    }
}
