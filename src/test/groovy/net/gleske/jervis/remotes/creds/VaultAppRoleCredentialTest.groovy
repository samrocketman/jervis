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

import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

import org.junit.Test

class VaultAppRoleCredentialTest extends GroovyTestCase {
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
