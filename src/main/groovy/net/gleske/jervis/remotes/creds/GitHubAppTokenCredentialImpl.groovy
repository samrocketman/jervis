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

import net.gleske.jervis.remotes.interfaces.GitHubAppTokenCredential

import java.time.Instant

/**
  A basic implementation of the
  <tt>{@link net.gleske.jervis.remotes.interfaces.GitHubAppTokenCredential}</tt>.
  In general, a more secure credential implementation is suggested.  For an
  example, see <tt>GitHubAppTokenCredential</tt> API documentation for examples.
  */
class GitHubAppTokenCredentialImpl implements GitHubAppTokenCredential, ReadonlyTokenCredential {
    /**
      An internal cache meant for storing issued credentials until their
      expiration.
      */
    private Map cache = [:].withDefault { key ->
        [:]
    }

    /**
      The hash to be uses for token storage and lookup.
      */
    private String hash

    /**
      The time buffer before a renewal is forced.  This is to account for clock
      drift and is customizable by the client.  By default the value is
      <tt>5</tt> seconds.  All time-based calculations around token renewal
      assume this is set correctly by the caller.  If it is incorrectly set then
      <tt>renew_buffer</tt> is <tt>0</tt> seconds.
      */
    Long renew_buffer = 30

    /**
      Returns renew buffer.  Does not allow renew buffer to be undefined or go below zero.

      @return <tt>0</tt> or a <tt>renew_buffer</tt> greater than <tt>0</tt>.
      */
    Long getRenew_buffer() {
        if(!renew_buffer || renew_buffer <= 0 || renew_buffer >= ttl) {
            return 0
        }
        this.renew_buffer
    }


    /**
      Checks if a token is expired.

      @param hash A hash used for performing a lookup on an internal token
                  cache.
      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired(String hash) {
        this.hash = hash
        // This would normally load from the cache but this example
        // implementation directly references the cache (internally just a
        // HashMap).
        if(!getExpiration() || !getToken()) {
            return true
        }
        isExpired()
    }

    /**
      Checks if a token is expired.

      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired() {
        if(!getExpiration()) {
            return true
        }
        isExpired(Instant.parse(getExpiration()))
    }

    /**
      Checks if a token is expired.

      @param expires Check if this instant is expired based on
                     <tt>{@link #getRenew_buffer()}</tt> and
                     <tt>{@link java.time.Instant#now()}</tt>.
      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired(Instant expires) {
        Long renewAt = expires.epochSecond - getRenew_buffer()
        Instant now = new Date().toInstant()
        now.epochSecond >= renewAt
    }

    /**
      This method is more for demostrating cache cleanup.  A real backend cache
      would look slightly different but cleanup of expired entries should still
      occur.
      */
    private void cleanupCache() {
        // Find expired cache entries.
        List cleanup = this.cache.findAll { hash, entry ->
            !entry?.expires_at || isExpired(Instant.parse(entry.expires_at))
        }.collect { hash, entry ->
            hash
        } ?: []

        // Delete expired entries from the cache.
        cleanup.each { hash ->
            this.cache.remove(hash)
        }
    }

    /**
      A new token has been issued so this method is meant to update this class
      instance as well as perform any backend cache operations such as cleanup
      of expired tokens.
      */
    void updateTokenWith(String token, String expiration, String hash) {
        this.hash = hash
        this.cache[hash].token = token
        setExpiration(expiration)
        this.cache[hash].expires_at = expiration
        // Removes expired cache entries
        cleanupCache()
    }

    /**
      Sets the expiration for a given token.
      @param expiration An ISO instant formatted string like <tt>{@link java.time.Instant#toString()}</tt>.
      */
    void setExpiration(String expiration) {
        this.cache[this.hash].expires_at = expiration
    }

    /**
      Gets expiration of the token.
      */
    String getExpiration() {
        this.cache[this.hash]?.expires_at
    }

    /**
      Gets the GitHub App access token used for GitHub API authentication.
      */
    String getToken() {
        this.cache[this.hash]?.token
    }
}
