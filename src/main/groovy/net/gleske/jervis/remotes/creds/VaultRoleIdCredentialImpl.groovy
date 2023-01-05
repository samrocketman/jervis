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

import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

/**
  A simple credential for instantiating a vault approle credentials.
  */
class VaultRoleIdCredentialImpl implements VaultRoleIdCredential {
    private final String role_id
    private final String secret_id

    /**
      Instantiate a role ID credential meant to be used with approle
      authentication.
      @param role_id A role ID from an approle.
      @param secret_id A secret ID from an approle.
      */
    VaultRoleIdCredentialImpl(String role_id, String secret_id) {
        this.role_id = role_id
        this.secret_id = secret_id
    }

    /**
      Returns the Role ID used for approle authentication.
      */
    final String getRole_id() {
        this.role_id
    }

    /**
      Returns the Secret ID used for approle authentication.
      */
    final String getSecret_id() {
        this.secret_id
    }
}
