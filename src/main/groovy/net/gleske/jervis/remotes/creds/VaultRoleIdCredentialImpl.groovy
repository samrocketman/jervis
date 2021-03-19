/*
   Copyright 2014-2021 Sam Gleske - https://github.com/samrocketman/jervis

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
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

class VaultRoleIdCredentialImpl implements VaultRoleIdCredential {
    private final String role_id
    private final String secret_id
    VaultRoleIdCredentialImpl(String role_id, String secret_id) {
        this.role_id = role_id
        this.secret_id = secret_id
    }

    String getRole_id() {
        this.role_id
    }

    String getSecret_id() {
        this.secret_id
    }
}
