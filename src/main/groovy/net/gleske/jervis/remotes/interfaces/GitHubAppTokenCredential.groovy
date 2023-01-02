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

import java.time.Instant

/**
  Provides a high level interface for ephemeral API tokens issued by a GitHub
  App installation.
  */
interface GitHubAppTokenCredential extends TokenCredential {

    /**
      Checks if a token is expired.  This method will be called first before any
      other.  A hash will be provided in case there's a backend cache.  This
      method could load from the cache before returning the <tt>Boolean</tt>.

      <p>The hash is unique to the token provided by <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential#getHash()}</tt>.</p>

      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired(String hash)

    /**
      Checks if a token is expired.
      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired()

    /**
      Update all three properties with a repeatable unique hash provided in case
      backend caching is used.

      @param token An ephemeral GitHub token issued by a GitHub App.
      @param expiration The instant a token expires.  The format must be
                        <tt>{@link java.time.format.DateTimeFormatter#ISO_INSTANT}</tt>.
      @param hash A unique hash meant to be used with a backend cache.  The hash
             is built from the GitHub App ID, GitHub App Installation ID, and the
             requested scopes.
      */
    void updateTokenWith(String token, String expiration, String hash)

    /**
      Returns when an issued ephemeral GitHub API token will expire.
      */
    String getExpiration()

    /**
      Sets when an ephemeral GitHub API token will expire.  A quick parsing
      check can be performed with <tt>{@link java.time.Instant}</tt>.

<pre><code class="language-groovy">
void setExpiration(String expiration) {
    // A quick parse check
    java.time.Instant.parse(expiration)
    this.expiration = expiration
}
</code></pre>
      */
    void setExpiration(String expiration)
}

