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

/**
  This provides
  <a href="https://developer.hashicorp.com/vault/docs/auth/approle" target=_blank>HashiCorp Vault AppRole Authentication</a>
  which issues rotating ephemeral tokens automatically.  HashiCorp recommends
  using AppRole authentication for applications and services.  The high level
  operation of this class is the following.

  <ul>
  <li>
    Provide seamless Vault API authentication for
    <tt>{@link net.gleske.jervis.remotes.VaultService}</tt>.
  </li>
  <li>
    Use Role ID and Secret ID to issue, renew, rotate, and revoke Vault tokens
    automatically.  See examples in
    <tt>{@link net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential}</tt> and
    <tt>{@link net.gleske.jervis.remotes.creds.VaultRoleIdCredentialImpl}</tt>.
  </li>
  <li>
    Automatically injects issued Vault tokens as authentication headers.
  </li>
  <li>
    Tracks token expiration and automatically renews or rotates tokens
    transparent to <tt>VaultService</tt>.
  </li>
  <li>
    Can revoke issued tokens.
  </li>
  </ul>

  <h2>Vault Policy</h2>

  <p>If you decide to use renable tokens and allow all features provided by this
  class, then you'll need the following Vault policy.</p>

<pre><code>
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
</code></pre>

  <p>Bear in mind the above policy is just for this class for its credential
  management.  You'll want other policy rules for accessing secrets.</p>

  <h2>Recommended Vault Policy</h2>

  <p>In general, you'll want to use the most limited Vault Policy you can for
  your application.  For example, in this section you only need a two rule
  policy.</p>

<pre><code>
# Allow tokens to revoke themselves
path "auth/token/revoke-self" {
    capabilities = ["update"]
}

# Read only KV v2 Permissions
path "kv/*" {
    capabilities = ["read"]
}
</code></pre>

  <p>You can use the <tt>{@link net.gleske.jervis.remotes.VaultService}</tt> API
  client to apply this policy.  Alternately, you can do this in the Vault
  UI.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

Map data = [policy: '''\
# Allow tokens to revoke themselves
path "auth/token/revoke-self" {
    capabilities = ["update"]
}

# Read only KV v2 Permissions
path "kv/*" {
    capabilities = ["read"]
}
''']

// Create an API client using an admin human user vault token
TokenCredential creds = [getToken: {-> 'your admin token' }] as TokenCredential
VaultService myvault = new VaultService('https://vault.example.com/', creds)

// Upload policy
myvault.apiFetch('sys/policy/jenkins-limited', [:], 'POST', data)
</code></pre>

  <h2>Initializing AppRole</h2>

  <p>This section discusses how to enable AppRole authentication in Vault as
  well as generate an application secret for a service such as Jenkins.</p>

  <h5>Enable AppRole authentication engine</h5>

  <p>You can enable AppRole via the Vault UI and set it up via Vault CLI.  This
  example illustrates how to do the same thing with the
  <tt>{@link net.gleske.jervis.remotes.VaultService}</tt> API client.  The
  example includes recommended service role settings with a short TTL (1 minute)
  and non-renewable tokens.  This class will automatically manage obtaining new
  tokens if a token it uses expires.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService
import net.gleske.jervis.tools.YamlOperator

// Create an API client using an admin human user vault token
TokenCredential creds = [getToken: {-> 'your admin token' }] as TokenCredential
VaultService myvault = new VaultService('https://vault.example.com/', creds)

// Recommended AppRole settings
Map approle_settings = [
    token_ttl: "1m",
    token_explicit_max_ttl: "1m",
    token_policies: ["jenkins-limited"],
    token_no_default_policy: true,
    token_type: "service"
]

// Enable AppRole Authentication Engine
myvault.apiFetch('sys/auth/approle', [:], 'POST', [type: 'approle'])

// Create an application Role 'jenkins'
myvault.apiFetch('auth/approle/role/jenkins', [:], 'POST', approle_settings)

// Generate an AppRole Role ID and Secret ID for the 'jenkins' Role
Map role_data = [:]
role_data.role_id = myvault.apiFetch('auth/approle/role/jenkins/role-id').data.role_id
role_data.secret_id = myvault.apiFetch('auth/approle/role/jenkins/secret-id', [:], 'POST')?.data?.secret_id
println(YamlOperator.writeObjToYaml(role_data))
</code></pre>

    <p>Take the <tt>role_id</tt> and <tt>secret_id</tt>, and use it to
    instantiate examples for this class.</p>

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

  <p>The following offers basic usage.  However, there are better and more
  secure examples in
  <tt>{@link net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential}</tt>.</p>

<pre><code>
import net.gleske.jervis.remotes.creds.VaultAppRoleCredential
import net.gleske.jervis.remotes.VaultService

VaultAppRoleCredential approle = new VaultAppRoleCredential('https://vault.example.com', 'app role id', 'app secret id')

// Instantiate vault API client with approle
VaultService vault = new VaultService(approle)

// Set mount kv/ to be KV v2 secrets engine
vault.mountVersions = [kv: 2]

// Ready to perform secrets operations
vault.getSecret('kv/path/to/secret')

// When your application is done using Vault it can proceed to revoke its active
// token.
approle.revokeToken()
</code></pre>
  */
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
        Map data = [role_id: this.credential.getRole_id(), secret_id: this.credential.getSecret_id()]
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
