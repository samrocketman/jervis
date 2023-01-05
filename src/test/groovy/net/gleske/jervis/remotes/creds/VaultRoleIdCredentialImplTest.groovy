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
//the VaultRoleIdCredentialImplTest() class automatically sees the VaultRoleIdCredentialImpl() class because they're in the same package

import org.junit.Test

class VaultRoleIdCredentialImplTest extends GroovyTestCase {
    @Test public void test_VaultRoleIdCredentialImpl_newInstance() {
        VaultRoleIdCredentialImpl cred = new VaultRoleIdCredentialImpl('myrole', 'somesecret')

        assert cred.role_id == 'myrole'
        assert cred.secret_id == 'somesecret'
        assert cred.getRole_id() == 'myrole'
        assert cred.getSecret_id() == 'somesecret'
    }
    @Test public void test_VaultRoleIdCredentialImpl_fail_on_set_prop() {
        VaultRoleIdCredentialImpl cred = new VaultRoleIdCredentialImpl('myrole', 'somesecret')

        shouldFail(ReadOnlyPropertyException) {
            cred.role_id = 'myrole'
        }
        shouldFail(ReadOnlyPropertyException) {
            cred.secret_id = 'somesecret'
        }
    }
}
