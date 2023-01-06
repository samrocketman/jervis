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
  */

class GitHubAppRsaCredentialImpl implements GitHubAppRsaCredential {
    /**
      The GitHub App ID for a GitHub credential.
      */
    String appID

    /**
      The GitHub API URL for querying GitHub App API in case of self-hosted
      GitHub Enterprise.  Defaults to <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential#DEFAULT_GITHUB_API}</tt>.
      */
    String apiUri = GitHubAppCredential.DEFAULT_GITHUB_API

    /**
      When querying for App installations this is necessary to select the
      install for a user or organization where the GitHub App is installed.
      */
    String owner = ''

    /**
      A private key for a GitHub App necessary for signing JSON web tokens.
      */
    String privateKey

    GitHubAppRsaCredentialImpl(String github_app_id, String private_key) {
        this.appID = github_app_id
        this.privateKey = private_key
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
