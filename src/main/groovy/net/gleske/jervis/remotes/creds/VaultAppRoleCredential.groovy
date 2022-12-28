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

import java.time.Instant
import net.gleske.jervis.remotes.SimpleRestServiceSupport
import net.gleske.jervis.remotes.interfaces.VaultCredential
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

// TODO add java doc
class VaultAppRoleCredential implements VaultCredential, ReadonlyTokenCredential, SimpleRestServiceSupport {
    // values specific to token instance
    private final String vault_url
    private final VaultRoleIdCredential credential

    // values specific to token lifetime
    private Integer ttl = 0
    private String token
    private String token_type
    private Boolean renewable = false
    private Instant leaseCreated

    /**
      Customizable HTTP headers which get sent to Vault in addition to
      authentication headers.
      */
    Map<String, String> headers = [:]

    /**
      The time buffer before a renewal is forced.  This is to account for clock
      drift and is customizable by the client.
      */
    Long renew_buffer = 15


    String getVault_url() {
        this.vault_url
    }

    String baseUrl() {
        this.vault_url
    }

    Map header(Map headers = [:]) {
        Map tempHeaders = this.headers + headers
        // https://www.vaultproject.io/api-docs#the-x-vault-request-header
        tempHeaders['X-Vault-Request'] = "true"
        if(headers['X-Jervis-Vault-Login']) {
            tempHeaders.remove('X-Jervis-Vault-Login')
        }
        else {
            tempHeaders['X-Vault-Token'] = getToken()
        }
        tempHeaders
    }

    VaultAppRoleCredential(String vault_url, String role_id, String secret_id) {
        this(vault_url, new VaultRoleIdCredentialImpl(role_id, secret_id))
    }

    VaultAppRoleCredential(String vault_url, VaultRoleIdCredential credential) {
        this.vault_url = addTrailingSlash(vault_url, 'v1/')
        this.credential = credential
    }

    private Boolean isExpired() {
        if(!this.token) {
            return true
        }
        Instant now = new Date().toInstant()
        if(this.ttl - (now.epochSecond - this.leaseCreated.epochSecond) > renew_buffer) {
            return false
        }
        true
    }

    void revokeToken() {
        if(!this.token) {
            return
        }
        if(this.token_type != 'batch') {
            apiFetch('auth/token/revoke-self', [:], 'POST')
        }
        this.token = null
    }

    private void leaseToken() {
        if(!isExpired() || tryRenewToken()) {
            return
        }
        this.token = null
        Map data = [role_id: this.credential.role_id, secret_id: this.credential.secret_id]
        this.leaseCreated = new Date().toInstant()
        Map response = apiFetch('auth/approle/login', ['X-Jervis-Vault-Login': true], 'POST', data)
        this.ttl = response.auth.lease_duration
        this.renewable = response.auth.renewable
        this.token = response.auth.client_token
        this.token_type = response.auth.token_type
    }

    // (new Date().toInstant().epochSecond) - createdLease.epochSecond // gives seconds elapsed
    // new Date(((Integer) epoc_seconds as Long)*1000).toInstant() // gets instant from given epoch timestamp
    String getToken() {
        if(!isExpired()) {
            return this.token
        }
        leaseToken()
        this.token
    }

    Boolean tryRenewToken() {
        if(!this.token || !this.renewable) {
            return false
        }
        try {
            Map data = [increment: "${this.ttl}s"]
            Map response = apiFetch('auth/token/renew-self', [:], 'POST', data)
            this.leaseCreated = new Date().toInstant()
            this.ttl = response.auth.lease_duration
            this.renewable = response.auth.renewable
            this.token = response.auth.client_token
            this.token_type = response.auth.token_type
            return true
        } catch(IOException e) {
            return false
        }
    }

    Map lookupSelfToken() throws IOException {
        apiFetch('auth/token/lookup-self')
    }
}
