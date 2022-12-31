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

import net.gleske.jervis.remotes.SimpleRestServiceSupport
import net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential
import net.gleske.jervis.remotes.interfaces.GitHubAppTokenCredential

/**
  Provides GitHub App Credential for API authentication.
  */
class GitHubAppCredential implements ReadonlyTokenCredential, SimpleRestServiceSupport {
    GitHubAppRsaCredential credential
    /**
      The default GitHub API URL.
      */
    static String DEFAULT_GITHUB_API = 'https://api.github.com/'

    /**
      Optionally set an installation ID for a GitHub app.  Set this to avoid
      extra API calls.  Querying for the app installation.  This ID is used
      when issuing ephemeral GitHub API tokens.
      */
    String installation_id

    String github_api_url

    Map headers = [:]

    private GitHubAppRsaCredential rsaCredential
    private GitHubAppTokenCredential tokenCredential

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
    GitHubAppCredential(GitHubAppRsaCredential rsaCredential, GitHubAppTokenCredential tokenCredential) {
        if(addTrailingSlash(credential.getApiUri()) == this.DEFAULT_GITHUB_API) {
            this.github_api_url = this.DEFAULT_GITHUB_API
        }
    }

    String baseUrl() {
        this.github_api_url
    }

    Map header(Map headers = [:]) {
        this.headers
    }

    String getToken() {
        tokenCredential.getToken()
    }

    /**
      If installation ID is not set, then it will automatically resolve an ID
      from app installations.  Automatic resolution will be attempted from the
      list of app installations based on the owner.  If owner is not set then
      the first item in the list of installations is selected.
      */
    String getInstallation_id() {
        this.installation_id
    }
}