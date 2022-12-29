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
package net.gleske.jervis.remotes.creds
//the VaultAppRoleCredentialTest() class automatically sees the VaultAppRoleCredential() class because they're in the same package

import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
  This class tests the
  <tt>{@link net.gleske.jervis.remotes.creds.VaultAppRoleCredential}</tt>
  class.  This uses auto-generated mock data using real API responses.

  <h2>Generate Mock Data</h2>

  Mock data has already been generated.  This is the script which captured mock
  data.

<pre><code>
import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
import net.gleske.jervis.remotes.SimpleRestServiceSupport

root_credential = 'hvs.bu4PfApCPrpSL0P1iOfC8EDE'

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
import net.gleske.jervis.tools.YamlOperator
import net.gleske.jervis.remotes.creds.VaultRoleIdCredentialImpl
import net.gleske.jervis.remotes.creds.VaultAppRoleCredential

void createAppRoleCredential(String approle_file_name) {
    File approle_file = new File(approle_file_name)
    if(approle_file.exists()) {
        return
    }
    TokenCredential creds = [getToken: {-> root_credential }] as TokenCredential
    VaultService myvault = new VaultService('http://vault:8200/v1/', creds)

    // Create approle
    Map approle_settings = [
        token_ttl: '1m',
        //token_explicit_max_ttl: "1m",
        token_max_ttl: '10m',
        token_policies: ['my_role'],
        token_no_default_policy: true,
        token_type: 'service'
    ]

    // enable approle auth method
    if(!('approle/' in myvault.apiFetch('sys/auth').keySet())) {
        myvault.apiFetch('sys/auth/approle', [:], 'POST', [type: 'approle'])
        myvault.apiFetch('auth/approle/role/my_approle_service', [:], 'POST', approle_settings)
        approle_settings.token_ttl = '30s'
        approle_settings.token_type = 'batch'
        myvault.apiFetch('auth/approle/role/my_approle_batch', [:], 'POST', approle_settings)
    }

    // Create a policy
    Map policy = [
        policy: '''\
        # Allow tokens to look up their own properties
        path "auth/token/lookup-self" {
            capabilities = ["read"]
        }

        # Allow tokens to renew themselves
        path "auth/token/renew-self" {
            capabilities = ["update"]
        }

        # Allow tokens to revoke themselves
        path "auth/token/revoke-self" {
            capabilities = ["update"]
        }

        # Read only KV v2 Permissions
        path "kv/*" {
            capabilities = ["read", "list"]
        }
        '''.stripIndent()
    ]
    if(!('my_role' in myvault.apiFetch('sys/policy').policies)) {
        myvault.apiFetch('sys/policy/my_role', [:], 'POST', policy)
    }

    Map role_data = [:]
    role_data.role_id = myvault.apiFetch('auth/approle/role/my_approle_service/role-id').data.role_id
    role_data.secret_id = myvault.apiFetch('auth/approle/role/my_approle_service/secret-id', [:], 'POST')?.data?.secret_id
    YamlOperator.writeObjToYaml(approle_file, role_data)
        role_data.role_id = myvault.apiFetch('auth/approle/role/my_approle_batch/role-id').data.role_id
    role_data.secret_id = myvault.apiFetch('auth/approle/role/my_approle_batch/secret-id', [:], 'POST')?.data?.secret_id
    YamlOperator.writeObjToYaml(new File(approle_file_name + '-batch'), role_data)
}

createAppRoleCredential('/tmp/vault-approle')

// Record URL API data to files as mock data
recordMockUrls(url, URL, request_meta, true, 'SHA-256', request_history)

File approle_file = new File('/tmp/vault-approle')
VaultRoleIdCredentialImpl roleid_service = YamlOperator.loadYamlFrom(approle_file).with { role_data ->
    new VaultRoleIdCredentialImpl(role_data.role_id, role_data.secret_id)
}
approle_file = new File('/tmp/vault-approle-batch')
VaultRoleIdCredentialImpl roleid_batch = YamlOperator.loadYamlFrom(approle_file).with { role_data ->
    new VaultRoleIdCredentialImpl(role_data.role_id, role_data.secret_id)
}

VaultAppRoleCredential approle_service = new VaultAppRoleCredential('http://vault:8200', roleid_service)
VaultAppRoleCredential approle_batch = new VaultAppRoleCredential('http://vault:8200', roleid_batch)

approle_service.lookupToken()
approle_service.revokeToken()
approle_service.getToken()
approle_batch.getToken()
</code></pre>
  */
class VaultAppRoleCredentialTest extends GroovyTestCase {
    def approle_service
    def approle_batch
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
        approle_service = new VaultAppRoleCredential('http://vault:8200', 'some role id', 'some secret id')
        approle_batch = new VaultAppRoleCredential('http://vault:8200', 'some role id', 'some secret id')
    }
    //tear down after every test
    @After protected void tearDown() {
        approle_service = null
        approle_batch = null
        request_meta.clear()
        request_history.clear()
        super.tearDown()
    }
    @Test public void test_VaultRoleIdCredentialImpl_newInstance() {
        VaultAppRoleCredential cred = new VaultAppRoleCredential('https://vault.example.com', 'some role id', 'some secret id')
        assert cred.credential.role_id == 'some role id'
        assert cred.credential.secret_id == 'some secret id'
        assert cred.vault_url == 'https://vault.example.com/v1/'
    }
    @Test public void test_VaultRoleIdCredentialImpl_newInstance_credential() {
        // A file-based backend where the file has 'role_id:secret_id' as its content.
        VaultRoleIdCredential roleid = [
            getRole_id: {-> 'another role' },
            getSecret_id: {-> 'another secret' }
        ] as VaultRoleIdCredential
        VaultAppRoleCredential cred = new VaultAppRoleCredential('https://vault.example.com/v1', roleid)
        assert cred.credential.getRole_id() == 'another role'
        assert cred.credential.getSecret_id() == 'another secret'
        assert cred.vault_url == 'https://vault.example.com/v1/'
    }
    @Test public void test_VaultRoleIdCredentialImpl_approle_mount() {
        VaultAppRoleCredential cred = new VaultAppRoleCredential('https://vault.example.com', 'some role id', 'some secret id')
        cred.approle_mount = 'foo/'
        assert cred.approle_mount == 'foo'
        cred.approle_mount = '/foo'
        assert cred.approle_mount == 'foo'
        cred.approle_mount = '/foo/'
        assert cred.approle_mount == 'foo'
        cred.approle_mount = '/foo/bar/'
        assert cred.approle_mount == 'foo/bar'
    }
}
