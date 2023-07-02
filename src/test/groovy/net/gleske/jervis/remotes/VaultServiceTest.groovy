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
  class.  This uses auto-generated mock data using real API responses.

  <h2>Generate Mock Data</h2>

  Mock data has already been generated.  This is the script which captured mock
  data.

<pre><code>
import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls

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

TokenCredential creds = [getToken: {-&gt; 'some token' }] as TokenCredential
VaultService myvault = new VaultService('http://vault:8200/v1/', creds)

myvault.discoverKVMounts()

// enable secrets mounts
Boolean init_kv = false
Boolean init_kv_cas = false
Boolean init_secret = false
Boolean init_kv_slash = false
Boolean init_secret_slash = false
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
if(!('kv2/withslash' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/kv2/withslash', [:], 'POST', '{"type": "kv", "options": {"version": "2"}}')
    init_kv_slash = true
}
if(!('secret2/withslash' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/secret2/withslash', [:], 'POST', '{"type": "kv", "options": {"version": "1"}}')
    init_secret_slash = true
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
myvault.&#64;mountVersions = [:]
myvault.&#64;cas_required = []

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

// Test recursive copy and delete operations
myvault.copyAllKeys('kv/foo', 'kv2/withslash/rename')
myvault.copyAllKeys('kv2/withslash/rename', 'secret2/withslash/subpath/')
myvault.copyAllKeys('secret/foo', 'secret2/withslash/rename')
myvault.copyAllKeys('secret2/withslash/rename', 'kv2/withslash/subpath/')
myvault.isDeletedSecret('kv/foo')
myvault.isDeletedSecret('secret/foo')
myvault.isDeletedSecret('secret/foo2')
myvault.isDeletedSecret('kv/foo2')
myvault.copySecret('secret/foo', 'kv2/withslash/deleteone')
myvault.copySecret('kv/foo', 'kv2/withslash/deleteone', 1)
myvault.copySecret('secret/foo', 'kv2/withslash/deleteone')
myvault.copySecret('kv/foo', 'kv2/withslash/deleteone')
myvault.deleteSecret('kv2/withslash/deleteone')
myvault.deleteSecret('kv2/withslash/deleteone', [1])
myvault.deleteSecret('kv2/withslash/deleteone', [3], true)
myvault.isDeletedSecret('kv2/withslash/deleteone')
myvault.isDeletedSecret('kv2/withslash/deleteone', 1)
myvault.isDeletedSecret('kv2/withslash/deleteone', 2)
myvault.isDeletedSecret('kv2/withslash/deleteone', 3)
myvault.isDeletedSecret('kv2/withslash/deleteone', 4)
myvault.getSecret('kv2/withslash/deleteone', 2)
myvault.copyAllKeys('kv/foo', 'kv2/withslash/somepath')
myvault.copyAllKeys('kv/foo', 'secret2/withslash/somepath')
myvault.deletePath('kv2/withslash/somepath')
myvault.deletePath('kv2/withslash/somepath', true)
myvault.deletePath('secret2/withslash/somepath')
myvault.copySecret('kv/foo', 'kv2/withslash/destroyone', 1)
myvault.copySecret('secret/foo', 'kv2/withslash/destroyone')
myvault.deleteSecret('kv2/withslash/destroyone', [2], true)
myvault.isDeletedSecret('kv2/withslash/destroyone')
myvault.isDeletedSecret('kv2/withslash/destroyone', 1)
myvault.setSecret('kv2/withslash/multitype', [number: 23, bool: true, 'nullkey': null, 'listkey': [1,2,3], 'mapkey': [simple: 'submap'], validvar: 'somevalue', '%user': 'special symbol'])
myvault.setSecret('secret2/withslash/multitype', [number: 23, bool: true, 'nullkey': null, 'listkey': [1,2,3], 'mapkey': [simple: 'submap'], validvar: 'somevalue', '%user': 'special symbol'])
myvault.setSecret('kv2/withslash/emptysecret', [:])

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
myvault.getSecret('kv2/withslash/multitype')
myvault.getSecret('secret2/withslash/multitype')
myvault.getSecret('kv2/withslash/emptysecret')

println 'Success.'
</code></pre>

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
        TokenCredential cred = [getToken: {-> 'fake-token' }] as TokenCredential
        myvault = new VaultService(DEFAULT_VAULT_URL, cred)
        myvault.mountVersions = ['secret':'1', 'kv2/withslash':'2', 'secret2/withslash':'1', 'kv':'2', 'kv_cas':'2']
        myvault.cas_required = ['kv_cas']
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
            assert myvault.credential != null
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
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1']
        List methods = ['GET']
        List datas = ['']
        assert myvault.getSecret('kv/foo', 1) == [hello: 'world']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_getSecret_location_map_kv_v1() {
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
    @Test public void test_VaultService_getSecret_location_map_kv_v1_slash() {
        assert myvault.getSecret(mount: 'secret', path: '/foo') == [test: 'data']
        assert myvault.getSecret(mount: 'secret', path: '/foo/bar') == [someother: 'data']
        assert myvault.getSecret(mount: 'secret', path: '/foo/bar/baz') == [more: 'secrets']
        assert myvault.getSecret(mount: 'secret', path: '/foo/bar/baz', dont_care: 'value') == [more: 'secrets']
    }
    @Test public void test_VaultService_getSecret_location_map_kv_v2() {
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
    @Test public void test_VaultService_getSecret_location_map_kv_v2_slash() {
        assert myvault.getSecret(mount: 'kv', path: '/foo') == [another: 'secret', hello: 'world']
        assert myvault.getSecret(mount: 'kv', path: '/foo/bar') == [hello: 'friend']
        assert myvault.getSecret(mount: 'kv', path: '/foo/bar/baz') == [foo: 'bar']
        assert myvault.getSecret(mount: 'kv', path: '/foo/bar/baz', dont_care: 'value') == [foo: 'bar']
    }
    @Test public void test_VaultService_getSecret_location_map_kv_v2_older_version_1() {
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
    @Test public void test_VaultService_getSecret_location_map_kv_v2_older_version_1_slash() {
        assert myvault.getSecret(mount: 'kv', path: '/foo', 1) == [hello: 'world']
    }
    @Test public void test_VaultService_discover_mount_versions() {
        myvault.@mountVersions = [:]
        myvault.@cas_required = []
        assert myvault.mountVersions == [:]
        assert myvault.cas_required == []
        myvault.discoverKVMounts()
        assert myvault.mountVersions == ['secret':'1', 'kv2/withslash':'2', 'secret2/withslash':'1', 'kv':'2', 'kv_cas':'2']
        assert myvault.cas_required == ['kv_cas']
        assert request_history*.url == ['http://vault:8200/v1/sys/mounts', 'http://vault:8200/v1/kv/config', 'http://vault:8200/v1/kv_cas/config', 'http://vault:8200/v1/kv2/withslash/config']
        assert request_history*.method == ['GET', 'GET', 'GET', 'GET']
    }
    @Test public void test_VaultService_discover_mount_versions_skip_cas_check() {
        myvault.@mountVersions = [:]
        myvault.@cas_required = ['kv2/withslash', 'kv', 'kv_cas']
        assert myvault.mountVersions == [:]
        assert myvault.cas_required == ['kv2/withslash', 'kv', 'kv_cas']
        myvault.discoverKVMounts()
        assert myvault.mountVersions == ['secret':'1', 'kv2/withslash':'2', 'secret2/withslash':'1', 'kv':'2', 'kv_cas':'2']
        assert myvault.cas_required == ['kv2/withslash', 'kv', 'kv_cas']
        assert request_history*.url == ['http://vault:8200/v1/sys/mounts']
        assert request_history*.method == ['GET']
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
        List data = ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz', 'secret/v1_set', 'secret/v1_set_force_cas', 'secret/v2_to_v1', 'secret/v2_to_v1_version_1']
        assert myvault.findAllKeys('secret') == data
        assert myvault.findAllKeys('secret/') == data
        assert myvault.findAllKeys('secret/', 1) == data.findAll { it.count('/') <= 1 }
        assert myvault.findAllKeys('secret/', 2) == data.findAll { it.count('/') <= 2 }
        assert myvault.findAllKeys('secret/', 3) == data.findAll { it.count('/') <= 3 }
    }
    @Test public void test_VaultService_findAllKeys_v1_subkey() {
        assert myvault.findAllKeys('secret/foo') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/foo/') == ['secret/foo/bar', 'secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v1_subkey_subpath() {
        assert myvault.findAllKeys('secret/foo/bar') == ['secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys('secret/foo/bar/') == ['secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v1_doesnotexist() {
        assert [] == myvault.findAllKeys('secret/zerg')
        assert [] == myvault.findAllKeys('secret/zerg/')
    }
    @Test public void test_VaultService_findAllKeys_v2() {
        List data = ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'kv/v1_to_v2', 'kv/v2_force_cas', 'kv/v2_force_cas_update', 'kv/v2_no_cas', 'kv/v2_to_v2/v2_to_v2', 'kv/v2_to_v2_version_1']
        assert myvault.findAllKeys('kv') == data
        assert myvault.findAllKeys('kv/') == data
        assert myvault.findAllKeys('kv/', 1) == data.findAll { it.count('/') <= 1 }
        assert myvault.findAllKeys('kv/', 2) == data.findAll { it.count('/') <= 2 }
        assert myvault.findAllKeys('kv/', 3) == data.findAll { it.count('/') <= 3 }
    }
    @Test public void test_VaultService_findAllKeys_v2_subkey() {
        assert myvault.findAllKeys('kv/foo') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/foo/') == ['kv/foo/bar', 'kv/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2_subkey_subpath() {
        assert myvault.findAllKeys('kv/foo/bar') == ['kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys('kv/foo/bar/') == ['kv/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2_doesnotexist() {
        assert [] == myvault.findAllKeys('kv/zerg')
        assert [] == myvault.findAllKeys('kv/zerg/')
    }
    @Test public void test_VaultService_findAllKeys_locaion_map_fail() {
        shouldFail(VaultException) {
            myvault.findAllKeys(path: '')
        }
        shouldFail(VaultException) {
            myvault.findAllKeys(mount: 'secret')
        }
        shouldFail(VaultException) {
            myvault.findAllKeys(mount: 'secret', path: 3)
        }
        shouldFail(VaultException) {
            myvault.findAllKeys(mount: 3, path: '')
        }
    }
    @Test public void test_VaultService_findAllKeys_v1_location_map() {
        List data = ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz', 'secret/v1_set', 'secret/v1_set_force_cas', 'secret/v2_to_v1', 'secret/v2_to_v1_version_1']
        assert myvault.findAllKeys(mount: 'secret', path: '') == data
        assert myvault.findAllKeys(mount: 'secret', path: '/') == data
        assert myvault.findAllKeys([mount: 'secret', path: '/'], 1) == data.findAll { it.count('/') <= 1 }
        assert myvault.findAllKeys([mount: 'secret', path: '/'], 2) == data.findAll { it.count('/') <= 2 }
        assert myvault.findAllKeys([mount: 'secret', path: '/'], 3) == data.findAll { it.count('/') <= 3 }
    }
    @Test public void test_VaultService_findAllKeys_v1_location_map_subkey() {
        assert myvault.findAllKeys(mount: 'secret', path: 'foo') == ['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'secret', path: 'foo/') == ['secret/foo/bar', 'secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v1_location_map_subkey_subpath() {
        assert myvault.findAllKeys(mount: 'secret', path: '/foo/bar') == ['secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'secret', path: 'foo/bar') == ['secret/foo/bar', 'secret/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'secret', path: '/foo/bar/') == ['secret/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'secret', path: 'foo/bar/') == ['secret/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v1_location_map_doesnotexist() {
        assert [] == myvault.findAllKeys(mount: 'secret', path: 'zerg')
        assert [] == myvault.findAllKeys(mount: 'secret', path: 'zerg/')
    }
    @Test public void test_VaultService_findAllKeys_v2_location_map() {
        List data = ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'kv/v1_to_v2', 'kv/v2_force_cas', 'kv/v2_force_cas_update', 'kv/v2_no_cas', 'kv/v2_to_v2/v2_to_v2', 'kv/v2_to_v2_version_1']
        assert myvault.findAllKeys(mount: 'kv', path: '') == data
        assert myvault.findAllKeys(mount: 'kv', path: '/') == data
        assert myvault.findAllKeys([mount: 'kv', path: '/'], 1) == data.findAll { it.count('/') <= 1 }
        assert myvault.findAllKeys([mount: 'kv', path: '/'], 2) == data.findAll { it.count('/') <= 2 }
        assert myvault.findAllKeys([mount: 'kv', path: '/'], 3) == data.findAll { it.count('/') <= 3 }
    }
    @Test public void test_VaultService_findAllKeys_v2_location_map_subkey() {
        assert myvault.findAllKeys(mount: 'kv', path: 'foo') == ['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'kv', path: 'foo/') == ['kv/foo/bar', 'kv/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2_location_map_subkey_subpath() {
        assert myvault.findAllKeys(mount: 'kv', path: '/foo/bar') == ['kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'kv', path: 'foo/bar') == ['kv/foo/bar', 'kv/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'kv', path: '/foo/bar/') == ['kv/foo/bar/baz']
        assert myvault.findAllKeys(mount: 'kv', path: 'foo/bar/') == ['kv/foo/bar/baz']
    }
    @Test public void test_VaultService_findAllKeys_v2_location_map_doesnotexist() {
        assert [] == myvault.findAllKeys(mount: 'kv', path: 'zerg')
        assert [] == myvault.findAllKeys(mount: 'kv', path: 'zerg/')
    }
    @Test public void test_VaultService_listPath_v1() {
        List data = ['foo', 'foo/', 'v1_set', 'v1_set_force_cas', 'v2_to_v1', 'v2_to_v1_version_1']
        assert myvault.listPath('secret') == data
        assert myvault.listPath('secret/') == data
        assert myvault.listPath('secret/foo') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/') == ['bar', 'bar/']
        assert myvault.listPath('secret/foo/bar') == ['baz']
        assert myvault.listPath('secret/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath('secret/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_listPath_v2() {
        List data = ['foo', 'foo/', 'v1_to_v2', 'v2_force_cas', 'v2_force_cas_update', 'v2_no_cas', 'v2_to_v2/', 'v2_to_v2_version_1']
        assert myvault.listPath('kv') == data
        assert myvault.listPath('kv/') == data
        assert myvault.listPath('kv/foo') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/') == ['bar', 'bar/']
        assert myvault.listPath('kv/foo/bar') == ['baz']
        assert myvault.listPath('kv/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath('kv/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_listPath_v1_location_map() {
        List data = ['foo', 'foo/', 'v1_set', 'v1_set_force_cas', 'v2_to_v1', 'v2_to_v1_version_1']
        assert myvault.listPath(mount: 'secret', path: '') == data
        assert myvault.listPath(mount: 'secret', path: '/') == data
        assert myvault.listPath(mount: 'secret', path: 'foo') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'secret', path: '/foo') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'secret', path: 'foo/') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'secret', path: '/foo/') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'secret', path: 'foo/bar') == ['baz']
        assert myvault.listPath(mount: 'secret', path: '/foo/bar') == ['baz']
        assert myvault.listPath(mount: 'secret', path: 'foo/bar/') == ['baz']
        assert myvault.listPath(mount: 'secret', path: '/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath(mount: 'secret', path: 'foo/bar/baz/')
        }
        shouldFail(IOException) {
            myvault.listPath(mount: 'secret', path: '/foo/bar/baz/')
        }
    }
    @Test public void test_VaultService_listPath_v2_location_map() {
        List data = ['foo', 'foo/', 'v1_to_v2', 'v2_force_cas', 'v2_force_cas_update', 'v2_no_cas', 'v2_to_v2/', 'v2_to_v2_version_1']
        assert myvault.listPath(mount: 'kv', path: '') == data
        assert myvault.listPath(mount: 'kv', path: '/') == data
        assert myvault.listPath(mount: 'kv', path: 'foo') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'kv', path: '/foo') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'kv', path: 'foo/') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'kv', path: '/foo/') == ['bar', 'bar/']
        assert myvault.listPath(mount: 'kv', path: 'foo/bar') == ['baz']
        assert myvault.listPath(mount: 'kv', path: '/foo/bar') == ['baz']
        assert myvault.listPath(mount: 'kv', path: 'foo/bar/') == ['baz']
        assert myvault.listPath(mount: 'kv', path: '/foo/bar/') == ['baz']
        shouldFail(IOException) {
            myvault.listPath(mount: 'kv', path: 'foo/bar/baz/')
        }
        shouldFail(IOException) {
            myvault.listPath(mount: 'kv', path: '/foo/bar/baz/')
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
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/secret/v2_to_v1_version_1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"hello":"world"}']
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
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/kv/metadata/v2_to_v2_version_1', 'http://vault:8200/v1/kv/data/v2_to_v2_version_1']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v1_to_v2() {
        myvault.copySecret([mount: 'secret', path: 'foo'], [mount: 'kv', path: 'v1_to_v2'])
        List urls = ['http://vault:8200/v1/secret/foo', 'http://vault:8200/v1/kv/metadata/v1_to_v2', 'http://vault:8200/v1/kv/data/v1_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v1_to_v2_slashes() {
        myvault.copySecret([mount: 'secret', path: '/foo'], [mount: 'kv', path: '/v1_to_v2'])
        List urls = ['http://vault:8200/v1/secret/foo', 'http://vault:8200/v1/kv/metadata/v1_to_v2', 'http://vault:8200/v1/kv/data/v1_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v1() {
        myvault.copySecret([mount: 'kv', path: 'foo'], [mount: 'secret', path: 'v2_to_v1'])
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/secret/v2_to_v1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"another":"secret","hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v1_slashes() {
        myvault.copySecret([mount: 'kv', path: '/foo'], [mount: 'secret', path: '/v2_to_v1'])
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/secret/v2_to_v1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"another":"secret","hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v1_version_1() {
        myvault.copySecret([mount: 'kv', path: 'foo'], [mount: 'secret', path: 'v2_to_v1_version_1'], 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/secret/v2_to_v1_version_1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v1_version_1_slashes() {
        myvault.copySecret([mount: 'kv', path: '/foo'], [mount: 'secret', path: '/v2_to_v1_version_1'], 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/secret/v2_to_v1_version_1']
        List methods = ['GET', 'POST']
        List datas = ['', '{"hello":"world"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v2() {
        myvault.copySecret([mount: 'kv', path: 'foo'], [mount: 'kv', path: 'v2_to_v2/v2_to_v2'])
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/kv/metadata/v2_to_v2/v2_to_v2', 'http://vault:8200/v1/kv/data/v2_to_v2/v2_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v2_slashes() {
        myvault.copySecret([mount: 'kv', path: '/foo'], [mount: 'kv', path: '/v2_to_v2/v2_to_v2'])
        List urls = ['http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/kv/metadata/v2_to_v2/v2_to_v2', 'http://vault:8200/v1/kv/data/v2_to_v2/v2_to_v2']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v2_version_1() {
        myvault.copySecret([mount: 'kv', path: 'foo'], [mount: 'kv', path: 'v2_to_v2_version_1'], 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/kv/metadata/v2_to_v2_version_1', 'http://vault:8200/v1/kv/data/v2_to_v2_version_1']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copySecret_location_map_v2_to_v2_version_1_slashes() {
        myvault.copySecret([mount: 'kv', path: 'foo'], [mount: 'kv', path: 'v2_to_v2_version_1'], 1)
        List urls = ['http://vault:8200/v1/kv/data/foo?version=1', 'http://vault:8200/v1/kv/metadata/v2_to_v2_version_1', 'http://vault:8200/v1/kv/data/v2_to_v2_version_1']
        List methods = ['GET', 'GET', 'POST']
        List datas = ['', '', '{"data":{"hello":"world"},"options":{"cas":0}}']
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
    @Test public void test_VaultService_setSecret_location_map_v1() {
        myvault.setSecret([mount: 'secret', path: 'v1_set'], [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/secret/v1_set']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_location_map_v1_slash() {
        myvault.setSecret([mount: 'secret', path: '/v1_set'], [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/secret/v1_set']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_location_map_v1_force_cas() {
        myvault.setSecret([mount: 'secret', path: 'v1_set_force_cas'], [another: 'secret', hello: 'world'], true)
        List urls = ['http://vault:8200/v1/secret/v1_set_force_cas']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_location_map_v1_force_cas_slash() {
        myvault.setSecret([mount: 'secret', path: '/v1_set_force_cas'], [another: 'secret', hello: 'world'], true)
        List urls = ['http://vault:8200/v1/secret/v1_set_force_cas']
        List methods = ['POST']
        List datas = ['{"another":"secret","hello":"world"}']
        List response_codes = [204]
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
        assert request_history*.response_code == response_codes
    }
    @Test public void test_VaultService_setSecret_location_map_v2_no_cas() {
        myvault.setSecret([mount: 'kv', path: 'v2_no_cas'], [test: 'data'])
        List urls = ['http://vault:8200/v1/kv/data/v2_no_cas']
        List methods = ['POST']
        List datas = ['{"data":{"test":"data"}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_no_cas_slash() {
        myvault.setSecret([mount: 'kv', path: '/v2_no_cas'], [test: 'data'])
        List urls = ['http://vault:8200/v1/kv/data/v2_no_cas']
        List methods = ['POST']
        List datas = ['{"data":{"test":"data"}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_force_cas() {
        myvault.setSecret([mount: 'kv', path: 'v2_force_cas'], [test: 'data'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas', 'http://vault:8200/v1/kv/data/v2_force_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_force_cas_slash() {
        myvault.setSecret([mount: 'kv', path: '/v2_force_cas'], [test: 'data'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas', 'http://vault:8200/v1/kv/data/v2_force_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"data"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_force_cas_update_secret() {
        myvault.setSecret([mount: 'kv', path: 'v2_force_cas_update'], [test: 'update'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas_update', 'http://vault:8200/v1/kv/data/v2_force_cas_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"update"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_force_cas_update_secret_slash() {
        myvault.setSecret([mount: 'kv', path: '/v2_force_cas_update'], [test: 'update'], true)
        List urls = ['http://vault:8200/v1/kv/metadata/v2_force_cas_update', 'http://vault:8200/v1/kv/data/v2_force_cas_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"test":"update"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_detect_cas() {
        myvault.setSecret([mount: 'kv_cas', path: 'v2_detect_cas'], [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/v2_detect_cas', 'http://vault:8200/v1/kv_cas/data/v2_detect_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_detect_cas_slash() {
        myvault.setSecret([mount: 'kv_cas', path: '/v2_detect_cas'], [another: 'secret', hello: 'world'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/v2_detect_cas', 'http://vault:8200/v1/kv_cas/data/v2_detect_cas']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_detect_cas_update_secret() {
        myvault.setSecret([mount: 'kv_cas', path: 'data_to_update'], [update: 'secret'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/data_to_update', 'http://vault:8200/v1/kv_cas/data/data_to_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"update":"secret"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_setSecret_location_map_v2_detect_cas_update_secret_slash() {
        myvault.setSecret([mount: 'kv_cas', path: '/data_to_update'], [update: 'secret'])
        List urls = ['http://vault:8200/v1/kv_cas/metadata/data_to_update', 'http://vault:8200/v1/kv_cas/data/data_to_update']
        List methods = ['GET', 'POST']
        List datas = ['', '{"data":{"update":"secret"},"options":{"cas":1}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_getMountFromPath_nonempty_path() {
        assert myvault.getMountFromPath('kv/foo') == 'kv'
        assert myvault.getMountFromPath('secret/foo') == 'secret'
        assert myvault.getMountFromPath('kv_cas/foo') == 'kv_cas'
    }
    @Test public void test_VaultService_getMountFromPath_empty_path() {
        assert myvault.getMountFromPath('kv/') == 'kv'
        assert myvault.getMountFromPath('secret/') == 'secret'
        assert myvault.getMountFromPath('kv_cas/') == 'kv_cas'
        assert myvault.getMountFromPath('kv') == 'kv'
        assert myvault.getMountFromPath('secret') == 'secret'
        assert myvault.getMountFromPath('kv_cas') == 'kv_cas'
    }
    @Test public void test_VaultService_getMountFromPath_invalid_mount() {
        shouldFail(VaultException) {
            myvault.getMountFromPath('some/fake/path')
        }
        myvault.@mountVersions = [:]
        shouldFail(VaultException) {
            myvault.getMountFromPath('kv')
        }
    }
    @Test public void test_VaultService_getLocationFromPath_empty_path() {
        assert myvault.getLocationFromPath('kv/foo') == 'foo'
        assert myvault.getLocationFromPath('secret/foo') == 'foo'
        assert myvault.getLocationFromPath('kv_cas/foo') == 'foo'
    }
    @Test public void test_VaultService_getLocationFromPath_nonempty_path() {
        assert myvault.getLocationFromPath('kv/') == ''
        assert myvault.getLocationFromPath('secret/') == ''
        assert myvault.getLocationFromPath('kv_cas/') == ''
        assert myvault.getLocationFromPath('kv') == ''
        assert myvault.getLocationFromPath('secret') == ''
        assert myvault.getLocationFromPath('kv_cas') == ''
    }
    @Test public void test_VaultService_getLocationFromPath_invalid_mount() {
        shouldFail(VaultException) {
            myvault.getLocationFromPath('some/fake/path')
        }
    }
    @Test public void test_VaultService_getLocationMapFromPath_empty_path() {
        assert myvault.getLocationMapFromPath('kv/foo') == [mount: 'kv', path: 'foo']
        assert myvault.getLocationMapFromPath('secret/foo') == [mount: 'secret', path: 'foo']
        assert myvault.getLocationMapFromPath('kv_cas/foo') == [mount: 'kv_cas', path: 'foo']
    }
    @Test public void test_VaultService_getLocationMapFromPath_nonempty_path() {
        assert myvault.getLocationMapFromPath('kv/') == [mount: 'kv', path: '']
        assert myvault.getLocationMapFromPath('secret/') == [mount: 'secret', path: '']
        assert myvault.getLocationMapFromPath('kv_cas/') == [mount: 'kv_cas', path: '']
        assert myvault.getLocationMapFromPath('kv') == [mount: 'kv', path: '']
        assert myvault.getLocationMapFromPath('secret') == [mount: 'secret', path: '']
        assert myvault.getLocationMapFromPath('kv_cas') == [mount: 'kv_cas', path: '']
    }
    @Test public void test_VaultService_getLocationMapFromPath_invalid_mount() {
        shouldFail(VaultException) {
            myvault.getLocationMapFromPath('some/fake/path')
        }
    }
    @Test public void test_VaultService_getPathFromLocationMap_empty_path() {
        assert myvault.getPathFromLocationMap([mount: 'kv', path: 'foo']) == 'kv/foo'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: 'foo']) == 'secret/foo'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: 'foo']) == 'kv_cas/foo'
        assert myvault.getPathFromLocationMap([mount: 'kv', path: '/foo']) == 'kv/foo'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: '/foo']) == 'secret/foo'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: '/foo']) == 'kv_cas/foo'
        assert myvault.getPathFromLocationMap([mount: 'kv', path: '/foo/']) == 'kv/foo/'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: '/foo/']) == 'secret/foo/'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: '/foo/']) == 'kv_cas/foo/'
        assert myvault.getPathFromLocationMap([mount: 'kv', path: 'foo/']) == 'kv/foo/'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: 'foo/']) == 'secret/foo/'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: 'foo/']) == 'kv_cas/foo/'
    }
    @Test public void test_VaultService_getPathFromLocationMap_nonempty_path() {
        assert myvault.getPathFromLocationMap([mount: 'kv', path: '']) == 'kv/'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: '']) == 'secret/'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: '']) == 'kv_cas/'
        assert myvault.getPathFromLocationMap([mount: 'kv', path: '/']) == 'kv/'
        assert myvault.getPathFromLocationMap([mount: 'secret', path: '/']) == 'secret/'
        assert myvault.getPathFromLocationMap([mount: 'kv_cas', path: '/']) == 'kv_cas/'
    }
    @Test public void test_VaultService_getPathFromLocationMap_invalid_mount() {
        shouldFail(VaultException) {
            myvault.getPathFromLocationMap(mount: 'some', path: 'fake/path')
        }
    }
    @Test public void test_VaultService_copyAllKeys_kv2_withslash_rename() {
        myvault.copyAllKeys('kv/foo', 'kv2/withslash/rename')
        List urls = ['http://vault:8200/v1/kv/metadata/', 'http://vault:8200/v1/kv/metadata/foo/', 'http://vault:8200/v1/kv/metadata/foo/bar/', 'http://vault:8200/v1/kv/data/foo?version=0', 'http://vault:8200/v1/kv2/withslash/metadata/rename', 'http://vault:8200/v1/kv2/withslash/data/rename', 'http://vault:8200/v1/kv/data/foo/bar?version=0', 'http://vault:8200/v1/kv2/withslash/metadata/rename/bar', 'http://vault:8200/v1/kv2/withslash/data/rename/bar', 'http://vault:8200/v1/kv/data/foo/bar/baz?version=0', 'http://vault:8200/v1/kv2/withslash/metadata/rename/bar/baz', 'http://vault:8200/v1/kv2/withslash/data/rename/bar/baz']
        List methods = ['LIST', 'LIST', 'LIST', 'GET', 'GET', 'POST', 'GET', 'GET', 'POST', 'GET', 'GET', 'POST']
        List datas = ['', '', '', '', '', '{"data":{"another":"secret","hello":"world"},"options":{"cas":0}}', '', '', '{"data":{"hello":"friend"},"options":{"cas":0}}', '', '', '{"data":{"foo":"bar"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copyAllKeys_kv2_to_kv1_withslash_subpath() {
        myvault.copyAllKeys('kv2/withslash/rename', 'secret2/withslash/subpath/')
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/', 'http://vault:8200/v1/kv2/withslash/metadata/rename/', 'http://vault:8200/v1/kv2/withslash/metadata/rename/bar/', 'http://vault:8200/v1/kv2/withslash/data/rename?version=0', 'http://vault:8200/v1/secret2/withslash/subpath/rename', 'http://vault:8200/v1/kv2/withslash/data/rename/bar?version=0', 'http://vault:8200/v1/secret2/withslash/subpath/rename/bar', 'http://vault:8200/v1/kv2/withslash/data/rename/bar/baz?version=0', 'http://vault:8200/v1/secret2/withslash/subpath/rename/bar/baz']
        List methods = ['LIST', 'LIST', 'LIST', 'GET', 'POST', 'GET', 'POST', 'GET', 'POST']
        List datas = ['', '', '', '', '{"another":"secret","hello":"world"}', '', '{"hello":"friend"}', '', '{"foo":"bar"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copyAllKeys_kv1_to_kv2_withslash_rename() {
        myvault.copyAllKeys('secret/foo', 'secret2/withslash/rename')
        List urls = ['http://vault:8200/v1/secret/', 'http://vault:8200/v1/secret/foo/', 'http://vault:8200/v1/secret/foo/bar/', 'http://vault:8200/v1/secret/foo', 'http://vault:8200/v1/secret2/withslash/rename', 'http://vault:8200/v1/secret/foo/bar', 'http://vault:8200/v1/secret2/withslash/rename/bar', 'http://vault:8200/v1/secret/foo/bar/baz', 'http://vault:8200/v1/secret2/withslash/rename/bar/baz']
        List methods = ['LIST', 'LIST', 'LIST', 'GET', 'POST', 'GET', 'POST', 'GET', 'POST']
        List datas = ['', '', '', '', '{"test":"data"}', '', '{"someother":"data"}', '', '{"more":"secrets"}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_copyAllKeys_kv1_to_kv2_withslash_subpath() {
        myvault.copyAllKeys('secret2/withslash/rename', 'kv2/withslash/subpath/')
        List urls = ['http://vault:8200/v1/secret2/withslash/', 'http://vault:8200/v1/secret2/withslash/rename/', 'http://vault:8200/v1/secret2/withslash/rename/bar/', 'http://vault:8200/v1/secret2/withslash/rename', 'http://vault:8200/v1/kv2/withslash/metadata/subpath/rename', 'http://vault:8200/v1/kv2/withslash/data/subpath/rename', 'http://vault:8200/v1/secret2/withslash/rename/bar', 'http://vault:8200/v1/kv2/withslash/metadata/subpath/rename/bar', 'http://vault:8200/v1/kv2/withslash/data/subpath/rename/bar', 'http://vault:8200/v1/secret2/withslash/rename/bar/baz', 'http://vault:8200/v1/kv2/withslash/metadata/subpath/rename/bar/baz', 'http://vault:8200/v1/kv2/withslash/data/subpath/rename/bar/baz']
        List methods = ['LIST', 'LIST', 'LIST', 'GET', 'GET', 'POST', 'GET', 'GET', 'POST', 'GET', 'GET', 'POST']
        List datas = ['', '', '', '', '', '{"data":{"test":"data"},"options":{"cas":0}}', '', '', '{"data":{"someother":"data"},"options":{"cas":0}}', '', '', '{"data":{"more":"secrets"},"options":{"cas":0}}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_isDeletedSecret_versions_1_through_4() {
        assert true == myvault.isDeletedSecret('kv2/withslash/deleteone')
        assert true == myvault.isDeletedSecret('kv2/withslash/deleteone', 1)
        assert false == myvault.isDeletedSecret('kv2/withslash/deleteone', 2)
        assert true == myvault.isDeletedSecret('kv2/withslash/deleteone', 3)
        assert true == myvault.isDeletedSecret('kv2/withslash/deleteone', 4)
    }
    @Test public void test_VaultService_getSecret_kv2_withslash_version_2() {
        assert [hello: 'world'] == myvault.getSecret('kv2/withslash/deleteone', 2)
        List urls = ['http://vault:8200/v1/kv2/withslash/data/deleteone?version=2']
        List methods = ['GET']
        List datas = ['']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_kv2_withslash_softdelete() {
        myvault.deletePath('kv2/withslash/somepath')
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/', 'http://vault:8200/v1/kv2/withslash/data/somepath/bar/baz', 'http://vault:8200/v1/kv2/withslash/data/somepath/bar', 'http://vault:8200/v1/kv2/withslash/data/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_kv2_withslash_destroy() {
        myvault.deletePath('kv2/withslash/somepath', true)
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/baz', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar', 'http://vault:8200/v1/kv2/withslash/metadata/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_kv1_withslash_softdelete() {
        myvault.deletePath('secret2/withslash/somepath')
        List urls = ['http://vault:8200/v1/secret2/withslash/', 'http://vault:8200/v1/secret2/withslash/somepath/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/baz', 'http://vault:8200/v1/secret2/withslash/somepath/bar', 'http://vault:8200/v1/secret2/withslash/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_kv1_withslash_destroy() {
        myvault.deletePath('secret2/withslash/somepath', true)
        List urls = ['http://vault:8200/v1/secret2/withslash/', 'http://vault:8200/v1/secret2/withslash/somepath/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/baz', 'http://vault:8200/v1/secret2/withslash/somepath/bar', 'http://vault:8200/v1/secret2/withslash/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_location_map_kv2_withslash_softdelete() {
        myvault.deletePath(mount: 'kv2/withslash', path: 'somepath')
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/', 'http://vault:8200/v1/kv2/withslash/data/somepath/bar/baz', 'http://vault:8200/v1/kv2/withslash/data/somepath/bar', 'http://vault:8200/v1/kv2/withslash/data/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_location_map_kv2_withslash_destroy() {
        myvault.deletePath([mount: 'kv2/withslash', path: 'somepath'], true)
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar/baz', 'http://vault:8200/v1/kv2/withslash/metadata/somepath/bar', 'http://vault:8200/v1/kv2/withslash/metadata/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_location_map_kv1_withslash_softdelete() {
        myvault.deletePath(mount: 'secret2/withslash', path: 'somepath')
        List urls = ['http://vault:8200/v1/secret2/withslash/', 'http://vault:8200/v1/secret2/withslash/somepath/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/baz', 'http://vault:8200/v1/secret2/withslash/somepath/bar', 'http://vault:8200/v1/secret2/withslash/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deletePath_location_map_kv1_withslash_destroy() {
        myvault.deletePath([mount: 'secret2/withslash', path: 'somepath'], true)
        List urls = ['http://vault:8200/v1/secret2/withslash/', 'http://vault:8200/v1/secret2/withslash/somepath/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/', 'http://vault:8200/v1/secret2/withslash/somepath/bar/baz', 'http://vault:8200/v1/secret2/withslash/somepath/bar', 'http://vault:8200/v1/secret2/withslash/somepath']
        List methods = ['LIST', 'LIST', 'LIST', 'DELETE', 'DELETE', 'DELETE']
        List datas = ['', '', '', '', '', '']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_isDeletedSecret() {
        assert false == myvault.isDeletedSecret('kv/foo')
        assert false == myvault.isDeletedSecret('secret/foo')
    }
    @Test public void test_VaultService_isDeletedSecret_with_history_version_0_destroyed() {
        assert true == myvault.isDeletedSecret('kv2/withslash/destroyone')
        assert false == myvault.isDeletedSecret('kv2/withslash/destroyone', 1)
    }
    @Test public void test_VaultService_isDeletedSecret_path_fail() {
        shouldFail(VaultException) {
            myvault.isDeletedSecret('kv/foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret('secret/foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret('kv')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret('secret')
        }
    }
    @Test public void test_VaultService_isDeletedSecret_doesnotexist() {
        assert true == myvault.isDeletedSecret('secret/doesnotexist')
        assert true == myvault.isDeletedSecret('kv/doesnotexist')
    }
    @Test public void test_VaultService_isDeletedSecret_versions() {
        assert false == myvault.isDeletedSecret('kv/foo', 1)
        // version does not exist
        assert true == myvault.isDeletedSecret('kv/foo', 25)
    }


    @Test public void test_VaultService_isDeletedSecret_location_map() {
        assert false == myvault.isDeletedSecret(mount: 'kv', path: '/foo')
        assert false == myvault.isDeletedSecret(mount: 'kv', path: 'foo')
        assert false == myvault.isDeletedSecret(mount: 'secret', path: '/foo')
        assert false == myvault.isDeletedSecret(mount: 'secret', path: 'foo')
    }
    @Test public void test_VaultService_isDeletedSecret_location_map_path_fail() {
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'kv', path: '/foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'kv', path: 'foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'secret', path: '/foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'secret', path: 'foo/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'kv', path: '/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'kv', path: '')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'secret', path: '/')
        }
        shouldFail(VaultException) {
            myvault.isDeletedSecret(mount: 'secret', path: '')
        }
    }
    @Test public void test_VaultService_isDeletedSecret_location_map_doesnotexist() {
        assert true == myvault.isDeletedSecret(mount: 'secret', path: '/doesnotexist')
        assert true == myvault.isDeletedSecret(mount: 'secret', path: 'doesnotexist')
        assert true == myvault.isDeletedSecret(mount: 'kv', path: '/doesnotexist')
        assert true == myvault.isDeletedSecret(mount: 'kv', path: 'doesnotexist')
    }
    @Test public void test_VaultService_isDeletedSecret_location_map_versions() {
        assert false == myvault.isDeletedSecret([mount: 'kv', path: '/foo'], 1)
        assert false == myvault.isDeletedSecret([mount: 'kv', path: 'foo'], 1)
        // version does not exist
        assert true == myvault.isDeletedSecret([mount: 'kv', path: '/foo'], 25)
        assert true == myvault.isDeletedSecret([mount: 'kv', path: 'foo'], 25)
    }
    @Test public void test_VaultService_deleteSecret_kv2_softdelete() {
        myvault.deleteSecret('kv2/withslash/deleteone')
        List urls = ['http://vault:8200/v1/kv2/withslash/data/deleteone']
        List methods = ['DELETE']
        List datas = ['']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_kv2_destroy() {
        myvault.deleteSecret('kv2/withslash/somepath', true)
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/somepath']
        List methods = ['DELETE']
        List datas = ['']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_kv2_softdelete_version_1() {
        myvault.deleteSecret('kv2/withslash/deleteone', [1])
        List urls = ['http://vault:8200/v1/kv2/withslash/delete/deleteone']
        List methods = ['POST']
        List datas = ['{"versions":[1]}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_kv2_destroy_version_3() {
        myvault.deleteSecret('kv2/withslash/deleteone', [3], true)
        List urls = ['http://vault:8200/v1/kv2/withslash/destroy/deleteone']
        List methods = ['POST']
        List datas = ['{"versions":[3]}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_path_fail() {
        shouldFail(VaultException) {
            myvault.deleteSecret('kv/foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret('secret/foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret('kv')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret('secret')
        }
    }
    @Test public void test_VaultService_deleteSecret_location_map_kv2_softdelete() {
        myvault.deleteSecret(mount: 'kv2/withslash', path: 'deleteone')
        List urls = ['http://vault:8200/v1/kv2/withslash/data/deleteone']
        List methods = ['DELETE']
        List datas = ['']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_location_map_kv2_destroy() {
        myvault.deleteSecret([mount: 'kv2/withslash', path: 'somepath'], true)
        List urls = ['http://vault:8200/v1/kv2/withslash/metadata/somepath']
        List methods = ['DELETE']
        List datas = ['']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_location_map_kv2_softdelete_version_1() {
        myvault.deleteSecret([mount: 'kv2/withslash', path: 'deleteone'], [1])
        List urls = ['http://vault:8200/v1/kv2/withslash/delete/deleteone']
        List methods = ['POST']
        List datas = ['{"versions":[1]}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_location_map_kv2_destroy_version_3() {
        myvault.deleteSecret([mount: 'kv2/withslash', path: 'deleteone'], [3], true)
        List urls = ['http://vault:8200/v1/kv2/withslash/destroy/deleteone']
        List methods = ['POST']
        List datas = ['{"versions":[3]}']
        assert request_history*.url == urls
        assert request_history*.method == methods
        assert request_history*.data == datas
    }
    @Test public void test_VaultService_deleteSecret_location_map_path_fail() {
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'kv', path: '/foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'kv', path: 'foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'secret', path: '/foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'secret', path: 'foo/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'kv', path: '/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'kv', path: '')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'secret', path: '/')
        }
        shouldFail(VaultException) {
            myvault.deleteSecret(mount: 'secret', path: '')
        }
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2() {
        assert [another: 'secret', hello: 'world'] == myvault.getEnvironmentSecret('kv/foo')
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_version_1() {
        assert [hello: 'world'] == myvault.getEnvironmentSecret('kv/foo', 1)
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv1() {
        assert [test: 'data'] == myvault.getEnvironmentSecret('secret/foo')
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_location_map() {
        assert [another: 'secret', hello: 'world'] == myvault.getEnvironmentSecret(mount: 'kv', path: '/foo')
        assert [another: 'secret', hello: 'world'] == myvault.getEnvironmentSecret(mount: 'kv', path: 'foo')
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_location_map_version_1() {
        assert [hello: 'world'] == myvault.getEnvironmentSecret([mount: 'kv', path: '/foo'], 1)
        assert [hello: 'world'] == myvault.getEnvironmentSecret([mount: 'kv', path: 'foo'], 1)
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv1_location_map() {
        assert [test: 'data'] == myvault.getEnvironmentSecret(mount: 'secret', path: '/foo')
        assert [test: 'data'] == myvault.getEnvironmentSecret(mount: 'secret', path: 'foo')
    }
    @Test public void test_VaultService_getEnvironmentSecrets_fail() {
        shouldFail(VaultException) {
            myvault.getEnvironmentSecrets(['foo', 'foo/bar', 'foo/bar/baz'])
        }
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend']
        assert result == myvault.getEnvironmentSecrets(['kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_reverse_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'world']
        assert result == myvault.getEnvironmentSecrets(['kv/foo/bar/baz', 'kv/foo/bar', 'kv/foo'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_missingkey() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend']
        assert result == myvault.getEnvironmentSecrets(['kv/doesnotexist', 'kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_all_missingkey() {
        Map result = [:]
        assert result == myvault.getEnvironmentSecrets(['kv/doesnotexist', 'kv/doesnotexist2', 'kv/doesnotexist3'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_location_map_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend']
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: '/foo'], [mount: 'kv', path: '/foo/bar'], [mount: 'kv', path: '/foo/bar/baz']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: 'foo'], [mount: 'kv', path: 'foo/bar'], [mount: 'kv', path: 'foo/bar/baz']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_location_map_reverse_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'world']
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: '/foo/bar/baz'], [mount: 'kv', path: '/foo/bar'], [mount: 'kv', path: '/foo']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: 'foo/bar/baz'], [mount: 'kv', path: 'foo/bar'], [mount: 'kv', path: 'foo']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_location_map_missingkey() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend']
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: '/doesnotexist'], [mount: 'kv', path: '/foo'], [mount: 'kv', path: '/foo/bar'], [mount: 'kv', path: '/foo/bar/baz']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'kv', path: 'doesnotexist'], [mount: 'kv', path: 'foo'], [mount: 'kv', path: 'foo/bar'], [mount: 'kv', path: 'foo/bar/baz']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_order() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_reverse_order() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['secret/foo/bar/baz', 'secret/foo/bar', 'secret/foo'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_missingkey() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['secret/doesnotexist', 'secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_all_missingkey() {
        Map result = [:]
        assert result == myvault.getEnvironmentSecrets(['secret/doesnotexist', 'secret/doesnotexist2', 'secret/doesnotexist3'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_location_map_order() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: '/foo'], [mount: 'secret', path: '/foo/bar'], [mount: 'secret', path: '/foo/bar/baz']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: 'foo'], [mount: 'secret', path: 'foo/bar'], [mount: 'secret', path: 'foo/bar/baz']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_location_map_reverse_order() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: '/foo/bar/baz'], [mount: 'secret', path: '/foo/bar'], [mount: 'secret', path: '/foo']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: 'foo/bar/baz'], [mount: 'secret', path: 'foo/bar'], [mount: 'secret', path: 'foo']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_location_map_missingkey() {
        Map result = [more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: '/doesnotexist'], [mount: 'secret', path: '/foo'], [mount: 'secret', path: '/foo/bar'], [mount: 'secret', path: '/foo/bar/baz']])
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: 'doesnotexist'], [mount: 'secret', path: 'foo'], [mount: 'secret', path: 'foo/bar'], [mount: 'secret', path: 'foo/bar/baz']])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_kv2_mix_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend', more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['kv/doesnotexist', 'kv/foo', 'kv/foo/bar', 'kv/foo/bar/baz', 'secret/doesnotexist', 'secret/foo', 'secret/foo/bar', 'secret/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_kv2_mix_reverse_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'world', more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['secret/foo/bar/baz', 'secret/foo/bar', 'secret/foo', 'secret/doesnotexist', 'kv/foo/bar/baz', 'kv/foo/bar', 'kv/foo', 'kv/doesnotexist'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_kv2_location_map_mix_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'friend', more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets(['kv/doesnotexist', [mount: 'kv', path: '/foo'], 'kv/foo/bar', 'kv/foo/bar/baz', 'secret/doesnotexist', 'secret/foo', [mount: 'secret', path: '/foo/bar'], 'secret/foo/bar/baz'])
        assert result == myvault.getEnvironmentSecrets(['kv/doesnotexist', [mount: 'kv', path: 'foo'], 'kv/foo/bar', 'kv/foo/bar/baz', 'secret/doesnotexist', 'secret/foo', [mount: 'secret', path: 'foo/bar'], 'secret/foo/bar/baz'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_kv2_location_map_mix_reverse_order() {
        Map result = [another: 'secret', foo: 'bar', hello: 'world', more: 'secrets', someother: 'data', test: 'data']
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: '/foo/bar/baz'], 'secret/foo/bar', 'secret/foo', 'secret/doesnotexist', 'kv/foo/bar/baz', [mount: 'kv', path: '/foo/bar'], 'kv/foo', 'kv/doesnotexist'])
        assert result == myvault.getEnvironmentSecrets([[mount: 'secret', path: 'foo/bar/baz'], 'secret/foo/bar', 'secret/foo', 'secret/doesnotexist', 'kv/foo/bar/baz', [mount: 'kv', path: 'foo/bar'], 'kv/foo', 'kv/doesnotexist'])
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv1_mixdata() {
        Map result = [bool: 'true', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecret('secret2/withslash/multitype')
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv1_mixdata_allowInvalidKeys() {
        Map result = ['%user': 'special symbol', bool: 'true', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecret('secret2/withslash/multitype', 0, true)
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_mixdata() {
        Map result = [bool: 'true', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecret('kv2/withslash/multitype')
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_mixdata_allowInvalidKeys() {
        Map result = ['%user': 'special symbol', bool: 'true', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecret('kv2/withslash/multitype', 0, true)
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_mixdata() {
        Map result = [bool: 'true', number: '23', someother: 'data', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecrets(['secret2/withslash/multitype', 'secret/foo/bar'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv1_mixdata_allowInvalidKeys() {
        Map result = ['%user': 'special symbol', bool: 'true', number: '23', someother: 'data', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecrets(['secret2/withslash/multitype', 'secret/foo/bar'], true)
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_mixdata() {
        Map result = [bool: 'true', hello: 'friend', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecrets(['kv2/withslash/multitype', 'kv/foo/bar'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_mixdata_allowInvalidKeys() {
        Map result = ['%user': 'special symbol', bool: 'true', hello: 'friend', number: '23', validvar: 'somevalue']
        assert result == myvault.getEnvironmentSecrets(['kv2/withslash/multitype', 'kv/foo/bar'], true)
    }
    @Test public void test_VaultService_getEnvironmentSecret_kv2_emptydata() {
        assert [:] == myvault.getEnvironmentSecret('kv2/withslash/emptysecret')
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_emptydata() {
        assert [:] == myvault.getEnvironmentSecrets(['kv2/withslash/emptysecret'])
    }
    @Test public void test_VaultService_getEnvironmentSecrets_kv2_emptydata_in_list() {
        assert [hello: 'friend'] == myvault.getEnvironmentSecrets(['kv2/withslash/emptysecret', 'kv/foo/bar'])
    }
}
