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
package net.gleske.jervis.remotes
//the VaultServiceTest() class automatically sees the VaultService() class because they're in the same package
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

import net.gleske.jervis.exceptions.VaultException
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.interfaces.VaultCredential

import org.junit.After
import org.junit.Before
import org.junit.Test

class VaultServiceTest extends GroovyTestCase {
    def myvault
    def url
    Map request_meta = [:]
    List request_history = []
    private static String DEFAULT_VAULT_URL = 'http://vault:8200/v1/'

    List metaResult() {
        [request_history*.url.inspect(), request_history*.method.inspect(), request_history*.data.inspect(), request_history*.response_code.inspect()]
    }

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256', request_history)
        //TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        TokenCredential cred = [getToken: {-> 'hvs.CT1912OdOBRWnn1UVQntX9Ld' }] as TokenCredential
        myvault = new VaultService(DEFAULT_VAULT_URL, cred)
    }
    //tear down after every test
    @After protected void tearDown() {
        myvault = null
        request_meta.clear()
        request_history.clear()
        super.tearDown()
    }
    @Test public void test_VaultService_newInstance_and_baseUrl() {
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        List urls = [
            'http://vault:8200',
            'http://vault:8200/',
            'http://vault:8200/v1',
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
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        myvault = new VaultService(DEFAULT_VAULT_URL, cred)
        assert myvault.headers == [:]
        assert 'X-Vault-Token' in myvault.header().keySet()
        assert myvault.header()['X-Vault-Request'] == true
        myvault.headers = [foo: 'bar']
        assert myvault.headers == [foo: 'bar']
        assert 'X-Vault-Token' in myvault.header().keySet()
        assert myvault.header()['X-Vault-Request'] == true
        assert myvault.header().foo == 'bar'
        myvault.headers = ['X-Vault-Token': 'hacked', 'X-Vault-Request': 'false']
        assert myvault.headers == ['X-Vault-Token': 'hacked', 'X-Vault-Request': 'false']
        assert myvault.header() == ['X-Vault-Token': 'fake-token', 'X-Vault-Request': 'true']
    }
    @Test public void test_VaultService_getSecret_kv_v1() {
        myvault.discoverKVMounts()
        assert myvault.getSecret('secret/foo') == [test: 'data']
        assert myvault.getSecret('secret/foo/bar') == [someother: 'data']
        assert myvault.getSecret('secret/foo/bar/baz') == [more: 'secrets']
    }
    @Test public void test_VaultService_getSecret_kv_v2() {
        myvault.discoverKVMounts()
        assert myvault.getSecret('kv/foo') == [another: 'secret', hello: 'world']
        assert myvault.getSecret('kv/foo/bar') == [hello: 'friend']
        assert myvault.getSecret('kv/foo/bar/baz') == [foo: 'bar']
    }
    @Test public void test_VaultService_getSecret_kv_v2_older_version_1() {
        myvault.discoverKVMounts()
        assert myvault.getSecret('kv/foo', 1) == [hello: 'world']
    }
    @Test public void test_VaultService_discover_mount_versions() {
        assert myvault.mountVersions == [:]
        myvault.discoverKVMounts()
        assert myvault.mountVersions == [kv: '2', secret: '1']
    }
    @Test public void test_VaultService_getSecret_map_kv_v1() {
        myvault.discoverKVMounts()
        assert myvault.getSecret(mount: 'secret', path: 'foo') == [test: 'data']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar') == [someother: 'data']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar/baz') == [more: 'secrets']
        assert myvault.getSecret(mount: 'secret', path: 'foo/bar/baz', dont_care: 'value') == [more: 'secrets']
        shouldFail(VaultException) {
            myvault.getSecret([:])
        }
        shouldFail(VaultException) {
            myvault.getSecret(mount: 'secret')
        }
        shouldFail(VaultException) {
            myvault.getSecret(path: 'foo/bar/baz')
        }
    }
    @Test public void test_VaultService_getSecret_map_kv_v2() {
        myvault.discoverKVMounts()
        assert myvault.getSecret(mount: 'kv', path: 'foo') == [another: 'secret', hello: 'world']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar') == [hello: 'friend']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar/baz') == [foo: 'bar']
        assert myvault.getSecret(mount: 'kv', path: 'foo/bar/baz', dont_care: 'value') == [foo: 'bar']
        shouldFail(VaultException) {
            myvault.getSecret([:])
        }
        shouldFail(VaultException) {
            myvault.getSecret(mount: 'kv')
        }
        shouldFail(VaultException) {
            myvault.getSecret(path: 'foo/bar/baz')
        }
        shouldFail(VaultException) {
            myvault.getSecret(mount: 'doesnotexist', path: 'foo/bar/baz')
        }
    }
    @Test public void test_VaultService_getSecret_map_kv_v2_older_version_1() {
        myvault.discoverKVMounts()
        assert myvault.getSecret(mount: 'kv', path: 'foo', 1) == [hello: 'world']
        shouldFail(VaultException) {
            myvault.getSecret([:], 1)
        }
        shouldFail(VaultException) {
            myvault.getSecret(mount: 'kv', 1)
        }
        shouldFail(VaultException) {
            myvault.getSecret(path: 'foo', 1)
        }
        shouldFail(VaultException) {
            myvault.getSecret(mount: 'doesnotexist', path: 'foo', 1)
        }
    }
    @Test public void test_VaultService_setMountVersions_String() {
        myvault.setMountVersions('kv', '2')
        assert myvault.mountVersions == [kv: '2']
        myvault.setMountVersions('secret', '1')
        assert myvault.mountVersions == [kv: '2', secret: '1']
        shouldFail(VaultException) {
            myvault.setMountVersions('secret', 3)
        }
        shouldFail(VaultException) {
            myvault.setMountVersions('kv', '3')
        }
        shouldFail(VaultException) {
            myvault.setMountVersions('another', 'hello')
        }
    }
    @Test public void test_VaultService_setMountVersions_Map() {
        myvault.mountVersions = [kv: '2']
        assert myvault.mountVersions == [kv: '2']
        myvault.mountVersions = [secret: '1']
        assert myvault.mountVersions == [kv: '2', secret: '1']
        shouldFail(VaultException) {
            myvault.mountVersions = [secret: 3]
        }
        shouldFail(VaultException) {
            myvault.mountVersions = [kv: '3']
        }
        shouldFail(VaultException) {
            myvault.mountVersions = [another: 'hello']
        }
        shouldFail(VaultException) {
            myvault.mountVersions = [kv: '2', secret: '1', another: 'hello']
        }
    }
    @Test public void test_VaultService_findAllKeys_v1() {
        myvault.discoverKVMounts()
        assert myvault.findAllKeys('secret') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/', 1) == ['secret/foo']
        assert myvault.findAllKeys('secret/', 2) == ['secret/foo', 'secret/foo/bar']
        assert myvault.findAllKeys('secret/', 3) == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2() {
        myvault.discoverKVMounts()
        assert myvault.findAllKeys('kv') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/', 1) == ['kv/foo']
        assert myvault.findAllKeys('kv/', 2) == ['kv/foo', 'kv/foo/bar']
        assert myvault.findAllKeys('kv/', 3) == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
    }
    @Test public void test_VaultService_listPath_v1() {
        myvault.discoverKVMounts()
        assert myvault.listPath('secret') == ['foo', 'foo/']
        assert myvault.listPath('secret/') == ['foo', 'foo/']
        assert myvault.listPath('secret/foo') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/bar') == ['baz']
        assert myvault.listPath('secret/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath('secret/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_listPath_v2() {
        myvault.discoverKVMounts()
        assert myvault.listPath('kv') == ['foo', 'foo/']
        assert myvault.listPath('kv/') == ['foo', 'foo/']
        assert myvault.listPath('kv/foo') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/bar') == ['baz']
        assert myvault.listPath('kv/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath('kv/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_copySecret_v1_to_v2() {
        myvault.discoverKVMounts()
        myvault.copySecret('secret/foo', 'kv/v1_to_v2')
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_copySecret_v2_to_v1() {
        myvault.discoverKVMounts()
        myvault.copySecret('kv/foo', 'secret/v2_to_v1')
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_copySecret_v2_to_v1_version_1() {
        myvault.discoverKVMounts()
        myvault.copySecret('kv/foo', 'secret/v2_to_v1_version_1', 1)
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_copySecret_v2_to_v2() {
        myvault.discoverKVMounts()
        myvault.copySecret('kv/foo', 'kv/v2_to_v2/v2_to_v2')
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_copySecret_v2_to_v2_version_1() {
        myvault.discoverKVMounts()
        myvault.copySecret('kv/foo', 'kv/v2_to_v2_version_1', 1)
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v1() {
        myvault.discoverKVMounts()
        myvault.setSecret('secret/v1_set', [another: 'secret', hello: 'world'])
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v1_force_cas() {
        myvault.discoverKVMounts()
        myvault.setSecret('secret/v1_set_force_cas', [another: 'secret', hello: 'world'], true)
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v2_no_cas() {
        myvault.discoverKVMounts()
        myvault.setSecret('kv/v2_no_cas', [test: 'data'])
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v2_force_cas() {
        myvault.discoverKVMounts()
        myvault.setSecret('kv/v2_force_cas', [test: 'data'], true)
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v2_force_cas_update_secret() {
        myvault.discoverKVMounts()
        myvault.setSecret('kv/v2_force_cas_update', [test: 'update'], true)
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }

    @Test public void test_VaultService_setSecret_v2_detect_cas() {
        myvault.discoverKVMounts()
        myvault.setSecret('kv_cas/v2_detect_cas', [another: 'secret', hello: 'world'])
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v2_detect_cas_update_secret() {
        myvault.discoverKVMounts()
        myvault.setSecret('kv_cas/data_to_update', [update: 'secret'])
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        assert metaResult() == []
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
}
