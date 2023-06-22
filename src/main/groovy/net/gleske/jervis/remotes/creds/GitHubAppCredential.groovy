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

import net.gleske.jervis.exceptions.GitHubAppException
import net.gleske.jervis.remotes.SimpleRestServiceSupport
import net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential
import net.gleske.jervis.remotes.interfaces.EphemeralTokenCredential
import net.gleske.jervis.tools.SecurityIO

/**
  Provides GitHub App Credential for API authentication.

  <h2>Sample usage</h2>
<pre><code>
import net.gleske.jervis.remotes.creds.EphemeralTokenCache
import net.gleske.jervis.remotes.creds.GitHubAppCredential
import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl

import java.time.Instant

// Configure the private key downloaded from GitHub App.
GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl('123456', new File('app-private-key.pem').text)
rsaCred.owner = 'gh-organization'

// Configure encrypted token storage
EphemeralTokenCache tokenCred = new EphemeralTokenCache('src/test/resources/rsa_keys/good_id_rsa_4096')

// a small timing function
Long time(Closure c) {
    Instant before = Instant.now()
    c()
    Instant after = Instant.now()
    after.epochSecond - before.epochSecond
}

// Issue a token; if called multiple times then this token will retrieve the
// token from the cache.  It will issue a new token if the existing token
// expires.
println("Execution time: ${time { println('GitHub token: ' + new GitHubAppCredential(rsaCred, tokenCred).getToken()) }} second(s).")
println('Try again...')
println("Execution time: ${time { println('GitHub token: ' + new GitHubAppCredential(rsaCred, tokenCred).getToken()) }} second(s).")

println('\n' + ['='*80, 'Encrypted cache below', '='*80].join('\n') + '\n')

// Read the encrypted cache which is an encrypted YAML document.
println(new File(tokenCred.cacheFile).text)
</code></pre>
  */
class GitHubAppCredential implements ReadonlyTokenCredential, SimpleRestServiceSupport {
    private GitHubAppRsaCredential rsaCredential
    private EphemeralTokenCredential tokenCredential

    /**
      Optionally set an installation ID for a GitHub app.  Set this to avoid
      extra API calls.  Querying for the app installation.  This ID is used
      when issuing ephemeral GitHub API tokens.
      */
    String installation_id

    /**
      A JSON Web Token (JWT) issued for GitHub App API authentication.
      */
    private String jwtToken

    /**
      A unique hash identifying this credential.  This is a dynamically
      calculated hash.
      */
    private String hash

    /**
      Resolves installation ID for the GitHub app if it is not defined.
      */
    private void resolveInstallationId() {
        if(this.installation_id) {
            return
        }
        String installOwner = rsaCredential.getOwner()
        if(installOwner) {
            String installation = (ownerIsUser) ? "users/${installOwner}/installation" : "orgs/${installOwner}/installation"
            this.installation_id = apiFetch(installation)?.id
        } else {
            this.installation_id = apiFetch('app/installations')?.find()?.id
        }
    }


    /**
      If installation ID is not set, then it will automatically resolve an ID
      from app installations.  Automatic resolution will be attempted from the
      list of app installations based on the owner.  If owner is not set then
      the first item in the list of installations is selected.

      @return An installation ID for the installed GitHub App.
      */
    String getInstallation_id() {
        resolveInstallationId()
        if(!this.installation_id) {
            throw new GitHubAppException('No GitHub App installations found.  Did you install the GitHub App after creating it?')
        }
        this.installation_id
    }

    /**
      The public hosted GitHub API URL.
      @default <tt>https://api.github.com/</tt>
      */
    static String DEFAULT_GITHUB_API = 'https://api.github.com/'

    /**
      The URL which will be used for API requests to GitHub.
      @default <tt>https://api.github.com/</tt>
      */
    String github_api_url

    /**
      Pre-defined headers to add to the request.
      @default <tt>[:]</tt>
      */
    Map headers = [:]

    /**
      The scope a GitHub token should have when it is created.  By default, full
      GitHub app scope.  Learn more about
      <a href="https://docs.github.com/en/rest/apps/apps?apiVersion=2022-11-28#create-an-installation-access-token-for-an-app" target=_blank>available scopes when creating a token for a GitHub app</a>.

      <h2>Sample usage</h2>
<pre><code>
// Limit scope to readonly access to two repositories
github_app.scope = [repositories: ['repo1', 'repo2'], permissions: [contents: 'read']]
</code></pre>
      @default <tt>[:]</tt> or no scope which means it will get the full scope
               of the GitHub App.
      */
    Map scope = [:]

    /**
      Find app installation for user instead of organization.

      @default <tt>false</tt>
      */
    Boolean ownerIsUser = false

    /**
      Creates a new instance of a <tt>GitHubAppCredential</tt> meant to serve
      as an easy to use credential in API clients such as
      <tt>{@link net.gleske.jervis.remotes.GitHubGraphQL}</tt>
      and
      <tt>{@link net.gleske.jervis.remotes.GitHub}</tt>.

      @param rsaCredential Is an RSA private key with other GitHub app details
                           such as GitHub App ID and owner of an installation
                           which would be use to retrieve the
                           <tt>{@link #installation_id}</tt>.
      @param tokenCredential This will be used to store ephemeral tokens issued
                             by the GitHub App.  This parameter is provided as
                             a means to securely store the token in any
                             credential backend of choice.  As opposed to
                             storing the token within this class instance.
                             This is necessary due Jenkins serialization of
                             data to disk in Jenkins pipelines.  Refer to the
                             interface for a recommended example.
      */
    GitHubAppCredential(GitHubAppRsaCredential rsaCredential, EphemeralTokenCredential tokenCredential) {
        String rsaApiUrl = addTrailingSlash(rsaCredential.getApiUri())
        this.github_api_url = (rsaApiUrl == this.DEFAULT_GITHUB_API) ? this.DEFAULT_GITHUB_API : rsaApiUrl
        this.rsaCredential = rsaCredential
        this.tokenCredential = tokenCredential
    }

    /**
      Used for API access to issue tokens.
      @see #github_api_url
      @return A base URL for making GitHub API requests.
      **/
    String baseUrl() {
        this.github_api_url
    }

    /**
      Headers used for authentication.
      @see #headers
      @param headers Custom headers can be provided and combined with default
                     and pre-defined headers.
      @return Returns as set of headers with authorization configured meant for
              issuing tokens.
      */
    Map header(Map headers = [:]) {
        SecurityIO operator = new SecurityIO(rsaCredential.getPrivateKey())
        if(!this.jwtToken || !operator.verifyGitHubJWTPayload(jwtToken)) {
            this.jwtToken = operator.getGitHubJWT(rsaCredential.getAppID())
        }
        Map tempHeaders = this.headers + headers
        tempHeaders['Authorization'] = "Bearer ${this.jwtToken}"
        if(!('Accept' in tempHeaders.keySet())) {
            tempHeaders['Accept'] = 'application/vnd.github+json'
        }
        if(!('X-GitHub-Api-Version' in tempHeaders.keySet())) {
            tempHeaders['X-GitHub-Api-Version'] = '2022-11-28'
        }
        tempHeaders
    }

    /**
      Get a valid GitHub App API token meant for cloning code or interacting
      with GitHub APIs.
      @return Returns a valid GitHub API token.  The returned token will expire
              within an hour.  Calling this function should return the same
              token until it expires and a new token is automatically rotated.
      */
    String getToken() {
        String hash = getHash()
        if(tokenCredential.isExpired(hash)) {
            String id = getInstallation_id()
            Map response = apiFetch("app/installations/${id}/access_tokens", [:], 'POST', this.scope)
            tokenCredential.updateTokenWith(response.token, response.expires_at, hash)
        }
        tokenCredential.getToken()
    }

    /**
      Sets the RSA credential used for authentication.
      @see #getToken()
      @see <a href="https://jwt.io/" target=_blank>About JSON Web Tokens</a>
      @param cred A GitHub App RSA key used to generate a JSON Web Token (JWT)
                  for issuing API credentials.
      */
      void setRsaCredential(GitHubAppRsaCredential cred) {
          this.rsaCredential = cred
          // reset hash
          this.hash = ''
          getHash()
      }

    /**
      Sets the scope for issuing tokens.  This scope can be limited to specific
      repositories or a subset of permissions from the GitHub App.
      @see #scope
      */
      void setScope(Map scope) {
          this.scope = scope
          // reset hash
          this.hash = ''
          getHash()
      }

    /**
      A hash of <tt>{@link net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential#getId()}</tt> and requested token scope.

      @see net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential#getId()
      @see #scope
      @return A <tt>SHA-256</tt> hash value.
      */
    String getHash() {
        if(!this.hash) {
            this.hash = SecurityIO.sha256Sum([rsaCredential.getId(), this.scope.inspect()].join('\n'))
        }
        this.hash
    }

    /**
      This method will throw an exception because the hash calculation is
      dynamic and must not be set.
      @param hash An empty string or null is allowed to force hash
                  recalculation.  Any other value will throw an exception.
      @see #getHash()
      @see net.gleske.jervis.exceptions.GitHubAppException
      */
    void setHash(String hash) throws GitHubAppException {
        if(hash) {
            throw new GitHubAppException('Setting hash manually is not allowed.')
        }
        else {
            this.hash = hash
        }
    }
}
