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
    private Map cache = [:]
    String token
    String expiration

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
	
    Boolean isExpired(String hash) {
        if(!(hash in cache.keySet()) || !cache[hash].expires_at || !cache[hash].token) {
            return true
        }
        this.token = cache[hash].token
        this.expiration = cache[hash].expires_at
        isExpired()
    }

    Boolean isExpired() {
        isExpired(Instant.parse(this.expiration))
    }
    Boolean isExpired(Instant expires) {
    }
    void updateTokenWith(String token, String expiration, String hash)
    String getExpiration()
    String getToken() {
    }
}
