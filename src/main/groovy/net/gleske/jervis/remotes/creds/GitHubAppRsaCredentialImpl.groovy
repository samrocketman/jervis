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

import net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential

/**
  A basic implementation of the
  <tt>{@link net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential}</tt>.
  In general, a more secure credential implementation is suggested.  For an
  example, see <tt>GitHubAppRsaCredential</tt> API documentation for examples.
  */

class GitHubAppRsaCredentialImpl implements GitHubAppRsaCredential {
    String installation_id
    String appID
    String apiUri
    String owner
    String privateKey

    private static String GITHUB_API_URI = 'https://api.github.com/'

    GitHubAppRsaCredentialImpl(String github_app_id) {
        this.appID = github_app_id
    }

    private void resolveInstallationId() {
    }

    String getInstallation_id() {
        if(!this.installation_id) {
            resolveInstallationId()
        }
        this.installation_id
    }

    String getApiUri() {
        this.apiUrl ?: this.GITHUB_API_URI
    }

    String getOwner() {
        this.owner ?: ''
    }
}
