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
import net.gleske.jervis.remotes.interfaces.GitHubAppTokenCredential
import net.gleske.jervis.tools.SecurityIO

/**
  Provides GitHub App Credential for API authentication.

  <h2>Sample usage</h2>
<pre><code>
import net.gleske.jervis.remotes.creds.GitHubAppCredential
import net.gleske.jervis.remotes.creds.GitHubAppRsaCredentialImpl
import net.gleske.jervis.remotes.creds.GitHubAppTokenCredentialImpl

GitHubAppRsaCredentialImpl rsaCred = new GitHubAppRsaCredentialImpl('123456', new File('private-key.pem').text)
GitHubAppTokenCredentialImpl tokenCred = new GitHubAppTokenCredentialImpl()
tokenCred.loadCache = {-&gt;
    File f = new File('/tmp/cache.yml')
    if(!f.exists()) {
        return [:]
    }
    def cache = net.gleske.jervis.tools.YamlOperator.loadYamlFrom(f)
    (cache in Map) ? cache : [:]
}
tokenCred.saveCache = { Map cache -&gt;
    File f = new File('/tmp/cache.yml')
    net.gleske.jervis.tools.YamlOperator.writeObjToYaml(f, cache)
}

// Issue a token; if called multiple times then this token will retrieve the
// token from the cache.  It will issue a new token if the existing token
// expires.
new GitHubAppCredential(rsaCred, tokenCred).getToken()
</code></pre>
  */
class GitHubAppCredential implements ReadonlyTokenCredential, SimpleRestServiceSupport {
    private GitHubAppRsaCredential rsaCredential
    private GitHubAppTokenCredential tokenCredential

    /**
      Optionally set an installation ID for a GitHub app.  Set this to avoid
      extra API calls.  Querying for the app installation.  This ID is used
      when issuing ephemeral GitHub API tokens.
      */
    private String installation_id

    /**
      A JSON Web Token (JWT) issued for GitHub App API authentication.
      */
    private String jwtToken

    /**
      Resolves installation ID for the GitHub app if it is not defined.
      */
    private void resolveInstallationId() {
        if(this.installation_id) {
            return
        }
        List installations = apiFetch('app/installations')
        String installOwner = rsaCredential.getOwner()
        if(installOwner) {
            this.installation_id = installations.find {
                it?.account?.login == installOwner || it?.account?.slug == installOwner
            }?.id
        } else {
            this.installation_id = installations.first().id
        }
        if(!this.installation_id) {
            throw new GitHubAppException('No GitHub App installations found.  Did you install the GitHub App after creating it?')
        }
    }


    /**
      If installation ID is not set, then it will automatically resolve an ID
      from app installations.  Automatic resolution will be attempted from the
      list of app installations based on the owner.  If owner is not set then
      the first item in the list of installations is selected.

      @return An installation ID for the installed GitHub App.
      */
    private String getInstallation_id() {
        resolveInstallationId()
        this.installation_id
    }

    /**
      The public hosted GitHub API URL.  Set to <tt>https://api.github.com/</tt>.
      */
    static String DEFAULT_GITHUB_API = 'https://api.github.com/'

    /**
      The URL which will be used for API requests to GitHub.
      */
    String github_api_url

    /**
      Pre-defined headers to add to the request.
      */
    Map headers = [:]

    /**
      The scope a GitHub token should have when it is created.  By default, full
      GitHub app scope.  Learn more about
      <a href="https://docs.github.com/en/rest/apps/apps?apiVersion=2022-11-28#create-an-installation-access-token-for-an-app" target=_blank>available scopes when creating a token for a GitHub app</a>.

      <h2>Sample usage</h2>
<pre><code>
github_app.scope = [repositories: ["repo1", "repo2"], permissions: [contents: "read"]]
</code></pre>
      */
    Map scope = [:]

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
        String rsaApiUrl = addTrailingSlash(rsaCredential.getApiUri())
        this.github_api_url = (rsaApiUrl == this.DEFAULT_GITHUB_API) ? this.DEFAULT_GITHUB_API : rsaApiUrl
        this.rsaCredential = rsaCredential
        this.tokenCredential = tokenCredential
    }

    String baseUrl() {
        this.github_api_url
    }

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
      A hash of <tt>{@link net.gleske.jervis.remotes.interfaces.GitHubAppRsaCredential#getId()}</tt> and requested token scope.

      @return A <tt>SHA-256</tt> hash value.
      */
    String getHash() {
        SecurityIO.sha256Sum([rsaCredential.getId(), this.scope.inspect()].join('\n'))
    }
}
