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
package net.gleske.jervis.remotes.interfaces

/**
  A basic credential whose only purpose is to get a Role ID and Secret ID for
  <a href="https://developer.hashicorp.com/vault/docs/auth/approle" target=_blank>HashCorp Vault AppRole</a>
  authentication.
  */
interface VaultRoleIdCredential extends JervisCredential {
    /**
      When implemented, this method should return a role ID used for
      authenticating with Vault.
      */
    String getRole_id()
    /**
      When implemented, this method should return a secret ID used for
      authenticating with Vault.
      */
    String getSecret_id()
}
