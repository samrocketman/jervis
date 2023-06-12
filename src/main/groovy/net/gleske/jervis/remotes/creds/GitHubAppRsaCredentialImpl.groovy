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

import net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential
import net.gleske.jervis.tools.SecurityIO

/**
  A basic implementation of the
  <tt>{@link net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential}</tt>.
  In general, a more secure credential implementation is suggested.  For an
  example, see <tt>GitHubAppRsaCredential</tt> API documentation for examples.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl

GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl('123456', new File('github-app-private-key.pem').text)
// Update owner to query app installations
rsaCred.owner = 'samrocketman'
</code></pre>

  */

class GitHubAppRsaCredentialImpl implements GitHubAppRsaCredential {
    /**
      The GitHub App ID for a GitHub credential.
      */
    private final String appID

    /**
      The GitHub App ID for a GitHub credential.
      @default null
      */
    String getAppID() {
        this.appID
    }

    /**
      A custom closure meant for resolving the private key dynamically from a
      secured credential backend.
      <h2>Sample usage</h2>
      <p>The following example illustrates Jenkins credentials backend.
      However, you can use any backend for the closure such as HashiCorp
      Vault.</p>
<pre><code>
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials

import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl

GitHubAppCredentials appCred = CredentialsProvider.lookupCredentials(GitHubAppCredentials, Jenkins.instance, Jenkins.instance.ACL.SYSTEM).find {
    it.id == 'some-credential-id'
}

// Set the private key to an empty string because it is unused.  Get all values
// from the backend Jenkins credential.
GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl(appCred.appID, '', appCred.apiUri)
rsaCred.resolvePrivateKey = {-&gt;
    appCred.privateKey.plainText
}

// Update owner to query app installation@see https://jwt.io/ About JSON Web Tokenss
rsaCred.owner = appCred.owner ?: ''
</code></pre>
      @default null
      */
    Closure resolvePrivateKey

    /**
      Setter for custom closure meant for private key lookup.  This will
      recalculate <tt>{@link #getId()}</tt>.

      @see <a href="https://jwt.io/" target=_blank>About JSON Web Tokens</a>
      @param resolvePrivateKey A closure that returns a PKCS1 or PKCS8 PEM
                               formatted RSA private key as a <tt>String</tt>.
                               It is used to create a JSON Web Token (JWT) for
                               interacting with the GitHub API on behalf of the
                               GitHub App.
      */
    void setResolvePrivateKey(Closure resolvePrivateKey) {
        this.resolvePrivateKey = resolvePrivateKey
        recalculateId()
    }

    /**
      The GitHub API URL for querying GitHub App API in case of self-hosted
      GitHub Enterprise.

      @see net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API
      */
    private final String apiUri

    /**
      The GitHub API URL for querying GitHub App API in case of self-hosted
      GitHub Enterprise.

      @see net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API
      */
    String getApiUri() {
        this.apiUri
    }

    /**
      When querying for App installations this is necessary to select the
      install for a user or organization where the GitHub App is installed.
      @default Empty String <tt>''</tt>
      */
    String owner = ''

    /**
      Sets the owner.
      @param owner An owner to query installation ID from a GitHub app.
      */
    void setOwner(String owner) {
        this.owner = owner
        recalculateId()
    }

    /**
      A private key for a GitHub App necessary for signing JSON Web Tokens (JWT).
      */
    private final String privateKey

    /**
      A private key for a GitHub App necessary for signing JSON Web Tokens (JWT).

      @see <a href="https://jwt.io/" target=_blank>About JSON Web Tokens</a>
      @return A PKCS1 or PKCS8 PEM formatted RSA private key.
      */
    String getPrivateKey() {
        if(this.resolvePrivateKey) {
            return resolvePrivateKey()
        }
        this.privateKey
    }

    /**
      Instantiates an RSA credential for a GitHub App used to generate API
      tokens.
      @see net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API
      @param github_app_id An app ID for a GitHub App.
      @param private_key An RSA private key associated with the github_app_id
                         used to generate API tokens.  Format is PKCS1 or PKCS8
                         PEM RSA private key.
      @param api_url A custom URL to the GitHub API for GitHub Enterprise.
      */
    GitHubAppRsaCredentialImpl(String github_app_id, String private_key, String api_uri = GitHubAppCredential.DEFAULT_GITHUB_API) {
        this.appID = github_app_id
        this.privateKey = private_key
        this.apiUri = api_uri
    }

    /**
      Instantiates an RSA credential for a GitHub App used to generate API
      tokens.
      @see net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API
      @see <a href="https://jwt.io/" target=_blank>About JSON Web Tokens</a>
      @param github_app_id An app ID for a GitHub App.
      @param resolvePrivateKey A closure that returns a PKCS1 or PKCS8 PEM
                               formatted RSA private key as a <tt>String</tt>.
                               It is used to create a JSON Web Token (JWT) for
                               interacting with the GitHub API on behalf of the
                               GitHub App.
      @param api_url A custom URL to the GitHub API for GitHub Enterprise.
      */
    GitHubAppRsaCredentialImpl(String github_app_id, Closure resolvePrivateKey, String api_uri = GitHubAppCredential.DEFAULT_GITHUB_API) {
        this.appID = github_app_id
        this.resolvePrivateKey = resolvePrivateKey
        this.apiUri = api_uri
    }

    /**
      Recalculates the ID for this credential.
      */
    private void recalculateId() {
        // recalculate ID
        this.hash = ''
        getId()
    }

    /**
      A hash unique to this credential.
      */
    private String hash

    /**
      An ID unique to this credential.
      */
    String getId() {
        if(!this.hash) {
            this.hash = SecurityIO.sha256Sum(
                [
                    this.apiUri,
                    this.appID,
                    this.owner,
                    getPrivateKey()
                ].join('\n')
            )
        }
        this.hash
    }
}
