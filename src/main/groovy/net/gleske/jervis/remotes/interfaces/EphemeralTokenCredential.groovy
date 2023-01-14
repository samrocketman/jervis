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

package net.gleske.jervis.remotes.interfaces

/**
  Provides a high level interface for ephemeral API tokens issued by a
  time-limited token issuing service.

  <ul>
  <li>
    This interface expects <tt>{@link #isExpired(java.lang.String)}</tt> to be
    the first function called.
  </li>
  <li>
    If a token is verified as expired or otherwise invalid, then it is expected
    that
    <tt>{@link #updateTokenWith(java.lang.String, java.lang.String, java.lang.String)}</tt>
    be called to set a valid token with its expiration.
  </li>
  </ul>

  For an example implementation, refer to
  <tt>{@link net.gleske.jervis.remotes.creds.EphemeralTokenCache}</tt>.
  */
interface EphemeralTokenCredential extends TokenCredential {

    /**
      Checks if a token is expired.  This method will be called first before any
      other.  A hash will be provided in case there's a backend cache.  This
      method could load from the cache before returning the <tt>Boolean</tt>.

      @param hash Unique to the token provided by issuer.  For an example see
                  <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential#getHash()}</tt>.
                  It is used for storing and retrieving issued tokens from the
                  backend cache.
      @return Returns <tt>true</tt> if the token is expired requiring another
              to be issued.
      */
    Boolean isExpired(String hash)

    /**
      Checks if a token is expired.  <tt>{@link #isExpired(java.lang.String)}</tt> or
      <tt>{@link #updateTokenWith(java.lang.String, java.lang.String, java.lang.String)}</tt>
      should be called before this method.
      @return Returns <tt>true</tt> if the token is expired requiring another
              to be issued.
      */
    Boolean isExpired()

    /**
      Update all three properties with a repeatable unique hash provided in case
      backend caching is used.

      @param token An ephemeral token issued by a a token issuer.  See
                   <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential}</tt>
                   for an example of a token issuer.
      @param expiration The instant a token expires.  The format must be
                        <tt>{@link java.time.format.DateTimeFormatter#ISO_INSTANT}</tt>.
      @param hash Unique to the token provided by issuer.  For an example see
                  <tt>{@link net.gleske.jervis.remotes.creds.GitHubAppCredential#getHash()}</tt>.
                  It is used for storing and retrieving issued tokens from a
                  backend cache.
      */
    void updateTokenWith(String token, String expiration, String hash)

    /**
      Returns when an issued ephemeral token will expire.
      <tt>{@link #isExpired(java.lang.String)}</tt> or
      <tt>{@link #updateTokenWith(java.lang.String, java.lang.String, java.lang.String)}</tt>
      should be called before this method.
      */
    String getExpiration()

    /**
      Sets when an ephemeral token will expire.  A quick parsing check can be
      performed with <tt>{@link java.time.Instant}</tt>.
      <tt>{@link #isExpired(java.lang.String)}</tt> or
      <tt>{@link #updateTokenWith(java.lang.String, java.lang.String, java.lang.String)}</tt>
      should be called before this method.

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

