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
<pre><code>
import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl

GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl('123456', new File('github-app-private-key.pem').text)
// Update owner to query app installations
rsaCred.owner = 'samrocketman'
</code>
  */

class GitHubAppRsaCredentialImpl implements GitHubAppRsaCredential {
    /**
      The GitHub App ID for a GitHub credential.
      @default null
      */
    final String appID

    /**
      The GitHub API URL for querying GitHub App API in case of self-hosted
      GitHub Enterprise.

      @default DEFAULT_GITHUB_API from GitHubAppCredential
      @see net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API
      */
    String apiUri = GitHubAppCredential.DEFAULT_GITHUB_API

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
      A private key for a GitHub App necessary for signing JSON web tokens.
      @default null
      */
    final String privateKey

    /**
      Instantiates an RSA credential for a GitHub App used to generate API
      tokens.
      @param github_app_id An app ID for a GitHub App.
      @param private_key An RSA private key associated with the github_app_id
                         used to generate API tokens.  Format is PKCS1 or PKCS8
                         PEM RSA private key.
      */
    GitHubAppRsaCredentialImpl(String github_app_id, String private_key) {
        this.appID = github_app_id
        this.privateKey = private_key
    }

    /**
      Recalculates the ID for this credential.
      */
    private void recalculateId() {
        // recalculate ID
        this.id = ''
        getId()
    }

    private String id

    /**
      An ID unique to this credential.
      */
    String getId() {
        if(!this.id) {
            this.id = SecurityIO.sha256Sum(
                [
                    this.apiUri,
                    this.appID,
                    this.owner,
                    this.privateKey
                ].join('\n')
            )
        }
        this.id
    }
}
