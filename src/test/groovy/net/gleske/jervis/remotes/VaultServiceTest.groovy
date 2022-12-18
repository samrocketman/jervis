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

/**
  This class tests the <tt>{@link net.gleske.jervis.remotes.VaultService}</tt>
  class.  This uses auto-generated mock data using real API resonses.

  <h2>Generate Mock Data</h2>

  Mock data has already been generated.  This is the script which captured mock
  data.

<pre><tt>import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
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

import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

TokenCredential creds = [getToken: {-> 'hvs.CT1912OdOBRWnn1UVQntX9Ld' }] as TokenCredential
VaultService myvault = new VaultService('http://vault:8200/v1/', creds)

myvault.discoverKVMounts()

// enable secrets mounts
Boolean init_kv = false
Boolean init_kv_cas = false
Boolean init_secret = false
if(!('kv' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/kv', [:], 'POST', '{"type": "kv", "options": {"version": "2"}}')
    init_kv = true
}

if(!('kv_cas' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/kv_cas', [:], 'POST', '{"type": "kv", "options": {"version": "2"}}')
    myvault.apiFetch('kv_cas/config', [:], 'POST', '{"cas_required":true}')
    init_kv_cas = true
}
if(!('secret' in myvault.mountVersions.keySet())) {
  myvault.apiFetch('sys/mounts/secret', [:], 'POST', '{"type": "kv", "options": {"version": "1"}}')
  init_secret = true
}

// Discover mounts after potentially adding new secrets engines.
myvault.discoverKVMounts()

if(init_kv) {
    myvault.setSecret("kv/v2_force_cas_update", [test: 'data'], true)
    myvault.setSecret("kv/foo", ['hello':'world'])
    myvault.setSecret("kv/foo", ['another':'secret', 'hello':'world'])
    myvault.setSecret("kv/foo/bar", ['hello':'friend'])
    myvault.setSecret("kv/foo/bar/baz", ['foo':'bar'])
}
if(init_kv_cas) {
    myvault.setSecret("kv_cas/data_to_update", ['hello':'world'])
}
if(init_secret) {
    myvault.setSecret("secret/foo", ['test':'data'])
    myvault.setSecret("secret/foo/bar", ['someother':'data'])
    myvault.setSecret("secret/foo/bar/baz", ['more':'secrets'])
}

// Reset discovered mounts in order to capture API responses
myvault.@mountVersions = [:]
myvault.@cas_required = []

// Record URL API data to files as mock data
recordMockUrls(url, URL, request_meta, true, 'SHA-256', request_history)

// Rediscover mounts
myvault.discoverKVMounts()

// Read operations
myvault.getSecret('secret/foo')
myvault.getSecret('secret/foo/bar')
myvault.getSecret('secret/foo/bar/baz')
myvault.getSecret('kv/foo')
myvault.getSecret('kv/foo/bar')
myvault.getSecret('kv/foo/bar/baz')
myvault.getSecret('kv/foo', 1)
myvault.getSecret(mount: 'secret', path: 'foo')
myvault.getSecret(mount: 'secret', path: 'foo/bar')
myvault.getSecret(mount: 'secret', path: 'foo/bar/baz')
myvault.getSecret(mount: 'secret', path: 'foo/bar/baz', dont_care: 'value')
myvault.getSecret(mount: 'kv', path: 'foo')
myvault.getSecret(mount: 'kv', path: 'foo/bar')
myvault.getSecret(mount: 'kv', path: 'foo/bar/baz')
myvault.getSecret(mount: 'kv', path: 'foo/bar/baz', dont_care: 'value')
myvault.getSecret(mount: 'kv', path: 'foo', 1)
myvault.findAllKeys('secret')
myvault.findAllKeys('secret/')
myvault.findAllKeys('secret/', 1)
myvault.findAllKeys('secret/', 2)
myvault.findAllKeys('secret/', 3)
myvault.findAllKeys('kv')
myvault.findAllKeys('kv/')
myvault.findAllKeys('kv/', 1)
myvault.findAllKeys('kv/', 2)
myvault.findAllKeys('kv/', 3)
myvault.listPath('secret')
myvault.listPath('secret/')
myvault.listPath('secret/foo')
myvault.listPath('secret/foo/')
myvault.listPath('secret/foo/bar')
myvault.listPath('secret/foo/bar/')
myvault.listPath('kv')
myvault.listPath('kv/')
myvault.listPath('kv/foo')
myvault.listPath('kv/foo/')
myvault.listPath('kv/foo/bar')
myvault.listPath('kv/foo/bar/')

// Write operations
myvault.copySecret('secret/foo', 'kv/v1_to_v2')
myvault.copySecret('kv/foo', 'secret/v2_to_v1')
myvault.copySecret('kv/foo', 'secret/v2_to_v1_version_1', 1)
myvault.copySecret('kv/foo', 'kv/v2_to_v2/v2_to_v2')
myvault.copySecret('kv/foo', 'kv/v2_to_v2_version_1', 1)
myvault.setSecret('secret/v1_set', [another: 'secret', hello: 'world'])
myvault.setSecret('secret/v1_set_force_cas', [another: 'secret', hello: 'world'], true)
myvault.setSecret('kv/v2_no_cas', [test: 'data'])
myvault.setSecret('kv/v2_force_cas', [test: 'data'], true)
myvault.setSecret('kv/v2_force_cas_update', [test: 'update'], true)
myvault.setSecret('kv_cas/v2_detect_cas', [another: 'secret', hello: 'world'])
myvault.setSecret('kv_cas/data_to_update', [update: 'secret'])
println 'Success.'</tt></pre>

  */
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
        myvault.@mountVersions = ['kv':'2', 'kv_cas':'2', 'secret':'1']
        myvault.@cas_required = ['kv_cas']
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
        assert myvault.header()['X-Vault-Request'] == 'true'
        myvault.headers = [foo: 'bar']
        assert myvault.headers == [foo: 'bar']
        assert 'X-Vault-Token' in myvault.header().keySet()
        assert myvault.header()['X-Vault-Request'] == 'true'
        assert myvault.header().foo == 'bar'
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
        List urls = []
        List methods = []
        List datas = []
        List response_codes = []
        myvault.getSecret('kv/foo', 1)
        assert metaResult() == []
        assert myvault.getSecret('kv/foo', 1) == [hello: 'world']
    }
    @Test public void test_VaultService_discover_mount_versions() {
        myvault.@mountVersions = [:]
        myvault.@cas_required = []
        assert myvault.mountVersions == [:]
        assert myvault.cas_required == []
        myvault.discoverKVMounts()
        assert myvault.mountVersions == ['kv':'2', 'kv_cas':'2', 'secret':'1']
        assert myvault.cas_required == ['kv_cas']
    }
    @Test public void test_VaultService_getSecret_map_kv_v1() {
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
        myvault.@mountVersions = [:]
        myvault.@cas_required = []
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
        myvault.@mountVersions = [:]
        myvault.@cas_required = []
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
        assert myvault.findAllKeys('secret') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/', 1) == ['secret/foo']
        assert myvault.findAllKeys('secret/', 2) == ['secret/foo', 'secret/foo/bar']
        assert myvault.findAllKeys('secret/', 3) == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2() {
        assert myvault.findAllKeys('kv') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'kv/v2_force_cas_update']
        assert myvault.findAllKeys('kv/') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'kv/v2_force_cas_update']
        assert myvault.findAllKeys('kv/', 1) == ['kv/foo', 'kv/v2_force_cas_update']
        assert myvault.findAllKeys('kv/', 2) == ['kv/foo', 'kv/foo/bar', 'kv/v2_force_cas_update']
        assert myvault.findAllKeys('kv/', 3) == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'kv/v2_force_cas_update']
    }
    @Test public void test_VaultService_listPath_v1() {
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
        assert myvault.listPath('kv') == ['foo', 'foo/', 'v2_force_cas_update']
        assert myvault.listPath('kv/') == ['foo', 'foo/', 'v2_force_cas_update']
        assert myvault.listPath('kv/foo') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/bar') == ['baz']
        assert myvault.listPath('kv/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath('kv/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_copySecret_v1_to_v2() {
        myvault.copySecret('secret/foo', 'kv/v1_to_v2')
        List urls = ['http://vault:8200/v1/secret/foo', 'http://vault:8200/v1/kv/metadata/v1_to_v2', 'http://vault:8200/v1/kv/data/v1_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_v2_to_v1() {
        myvault.copySecret('kv/foo', 'secret/v2_to_v1')
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/secret/v2_to_v1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"another":"secret","hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_v2_to_v1_version_1() {
        myvault.copySecret('kv/foo', 'secret/v2_to_v1_version_1', 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/secret/v2_to_v1_version_1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"another":"secret","hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_v2_to_v2() {
        myvault.copySecret('kv/foo', 'kv/v2_to_v2/v2_to_v2')
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/kv/metadata/v2_to_v2/v2_to_v2', 'http://vault:8200/v1/kv/data/v2_to_v2/v2_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_v2_to_v2_version_1() {
        myvault.copySecret('kv/foo', 'kv/v2_to_v2_version_1', 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/kv/metadata/v2_to_v2_version_1', 'http://vault:8200/v1/kv/data/v2_to_v2_version_1']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_v1() {
        myvault.setSecret('secret/v1_set', [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/secret/v1_set']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v1_force_cas() {
        myvault.setSecret('secret/v1_set_force_cas', [another: 'secret', hello: 'world'], true)
        List urls = ['http://vault:8200/v1/secret/v1_set_force_cas']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_v2_no_cas() {
        myvault.setSecret('kv/v2_no_cas', [test: 'data'])
        List urls = ['http://vault:8200/v1/kv/data/v2_no_cas']
        List methods = ['POST']
        List datas = ['{"data":{"test":"data"}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_v2_force_cas() {
        myvault.setSecret('kv/v2_force_cas', [test: 'data'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas', 'http://vault:8200/v1/kv/data/v2_force_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_v2_force_cas_update_secret() {
        myvault.setSecret('kv/v2_force_cas_update', [test: 'update'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas_update', 'http://vault:8200/v1/kv/data/v2_force_cas_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"update"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }

    @Test public void test_VaultService_setSecret_v2_detect_cas() {
        myvault.setSecret('kv_cas/v2_detect_cas', [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/v2_detect_cas', 'http://vault:8200/v1/kv_cas/data/v2_detect_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_v2_detect_cas_update_secret() {
        myvault.setSecret('kv_cas/data_to_update', [update: 'secret'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/data_to_update', 'http://vault:8200/v1/kv_cas/data/data_to_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"update":"secret"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
}
