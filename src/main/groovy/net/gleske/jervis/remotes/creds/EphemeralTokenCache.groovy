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

import net.gleske.jervis.remotes.interfaces.EphemeralTokenCredential
import net.gleske.jervis.tools.CipherMap
import net.gleske.jervis.tools.LockableFile
import net.gleske.jervis.tools.YamlOperator

import java.time.Instant

/**
  A flexible token cache with pluggable backend storage which encrypts the
  cache before storing it in the backend.  By default, this uses a file-based
  backend storage on
  <a href="https://docs.kernel.org/filesystems/tmpfs.html" target=_blank><tt>/dev/shm</tt> filesystem</a>
  which is in-memory file storage so very fast.
  */
class EphemeralTokenCache implements EphemeralTokenCredential, ReadonlyTokenCredential {
    /**
      Disallows instantiation without a private key.
      */
    private EphemeralTokenCache() throws IllegalStateException {
        throw new IllegalStateException('ERROR: This utility class must be instantiated with an RSA private key.  Use another constructor.')
    }

    /**
      An ephemeral token cache which provides encryption at rest.  A dynamic
      resolution of the private key from a 3rd party credential backend is the
      intention of this constructor.

      @see net.gleske.jervis.tools.CipherMap CipherMap provides encryption at rest
      @see #getPrivateKey Instantiates getPrivateKey closure
      @see #loadCache Instantiates loadCache closure
      @see #saveCache Instantiates saveCache closure
      @Warning If an empty <tt>String</tt> or <tt>null</tt> is returned by
               <tt>resolvePrivateKeyString</tt>, then encryption at rest is
               disabled.  This is intended to provide flexibility.  The cache
               entries contain sensitive API tokens and should be encrypted at
               rest if not by built-in, then by the backend service.
      @param resolvePrivateKeyString An executable <tt>Closure</tt> that takes
                                     no parameters and returns a PKCS1 or PKCS8
                                     formatted RSA private key as a
                                     <tt>String</tt>.
      */
    EphemeralTokenCache(Closure resolvePrivateKeyString) {
        setupClosures(resolvePrivateKeyString)
    }

    /**
      An ephemeral token cache which provides encryption at rest.  This assumes
      a private key is insecurely stored on disk if no 3rd party credential
      backend is used.

      @see net.gleske.jervis.tools.CipherMap
      @param resolvePrivateKeyString A PKCS1 or PKCS8 formatted RSA private key
                                     as a <tt>String</tt>.
      */
    EphemeralTokenCache(String privateKeyPath) {
        this({->
            new File(privateKeyPath).text
        })
    }

    /**
      Configures the load and save cache closures for a default file-based
      approach.  This occurs when a valid RSA key is provided.
      */
    private void setupClosures(Closure privateKeyClosure) {
        String privateKey = privateKeyClosure()
        if(privateKey) {
            // quickly test private key strength (it will throw an exception for a weak key)
            new CipherMap(privateKey)
            this.getPrivateKey = privateKeyClosure
        }
        this.loadCache = {->
            File f = new File(this.cacheFile)
            if(!f.exists()) {
                return ''
            }
            f.text
        }
        this.saveCache = { String cache ->
            File f = new File(this.cacheFile)
            // initialize file with private permissions
            if(!f.exists()) {
                ['/bin/sh', '-ec', "touch '${this.cacheFile}'; chmod 600 '${this.cacheFile}'"].execute()
            }
            // write out cache
            f.withWriter('UTF-8') { Writer w ->
                w << cache
            }
        }
    }
    /**
      An internal cache meant for storing issued credentials until their
      expiration.
      */
    private transient Map cache = [:].withDefault { key ->
        [:]
    }

    /**
      For encrypting at rest and loading encrypted data with regard to
      persistent cache.
      */
    private transient CipherMap cipherMap

    /**
      The hash to be used for token storage and lookup.
      */
    private String hash

    /**
      Load cache if both loading and saving cache are configured.
      */
    private void tryLoadCache() {
        if(!(loadCache && saveCache)) {
            return
        }
        Map temp = [:]
        String data = this.loadCache()
        String privateKey = this.getPrivateKey()
        if(privateKey) {
            this.cipherMap = new CipherMap(privateKey)
            this.cipherMap.hash_iterations = this.hash_iterations
            this.cipherMap << data
        }
        if(this.cipherMap) {
            temp = this.cipherMap.getPlainMap()
        }
        else {
            temp = YamlOperator.loadYamlFrom(data)
        }
        this.cache = temp.withDefault { key -> [:] }
    }

    /**
      Save cache if both loading and saving cache are configured.
      */
    private void trySaveCache() {
        if(!(loadCache && saveCache)) {
            return
        }
        String data
        if(this.cipherMap) {
            this.cipherMap.setPlainMap(this.cache)
            data = this.cipherMap.toString()
        }
        else {
            data = YamlOperator.writeObjToYaml(this.cache)
        }
        this.saveCache(data)
    }

    /**
      A function which may obtain a lock file if loading and saving a cache is
      configured.

      @param body A closure to execute with or without a lock.
      */
    private void tryLock(Closure body) {
        if(loadCache && saveCache) {
            new LockableFile(this.cacheLockFile).withLock {
                body()
            }
        }
        else {
            body()
        }
    }

    /**
      The path to a lock file which serializes read and write access to
      persistent cache where tokens issued by GitHub App are stored.

      Defaults to <tt>/dev/shm/jervis-gh-app-token-cache.lock</tt>.  On Linux,
      <tt>/dev/shm</tt> is similar to <tt>/tmp</tt> but is a RAM disk so very
      fast.
      */
    String cacheLockFile = '/dev/shm/jervis-token-cache.lock'

    /**
      The path to the persistent cache file if this class is initialized with
      encryption at rest.
      */
    String cacheFile = '/dev/shm/jervis-token-cache.yml'

    /**
      A closure which should return a <tt>String</tt> from loading the cache.
      Persistent caching of tokens is optional.

      <h2>Sample usage</h2>

      <p><b>WARNING:</b> The cache entries contain sensitive API tokens and
      should be encrypted at rest.  This example is plain text tokens.  It is
      recommended to encrypt at rest.  Refer to <tt>{@link #getPrivateKey}</tt>
      which includes an example for encrypting at rest.</p>

      <p>This example shows an insecure file cache. Both <tt>loadCache</tt> and
      <tt>saveCache</tt> must be set for caching to activate.</p>

<pre><code>
import net.gleske.jervis.remotes.creds.EphemeralTokenCache

// Instantiate without encryption at rest.
EphemeralTokenCache tokenCred = new EphemeralTokenCache({-&gt; ''})

tokenCred.loadCache = {-&gt;
    File f = new File('/dev/shm/cache.yml')
    if(!f.exists()) {
        return ''
    }
    f.text
}

tokenCred.saveCache = { String cache -&gt;
    // initialize file with private permissions
    ['/bin/sh', '-ec', 'touch /dev/shm/cache.yml; chmod 600 /dev/shm/cache.yml'].execute()
    // write out cache
    new File('/dev/shm/cache.yml').withWriter('UTF-8') { Writer w -&gt;
        w &lt;&lt; cache + '\n'
    }
}
</code></pre>
      */
    Closure loadCache

    /**
      A closure which which should support a <tt>String</tt> parameter used to
      optionally persist a cache.  See <tt>{@link #loadCache}</tt> (plaintext)
      or <tt>{@link #getPrivateKey}</tt> (encrypted at rest) for an example.
      */
    Closure saveCache

    /**
      A closure that returns an RSA private key used to encipher the cache for
      storing API tokens issued by GitHub App for encryption at rest.
      Encryption privided by <tt>CipherMap</tt>.

      <h2>Sample usage</h2>

      <p>This assumes a private key is insecurely stored as a file.</p>

<pre><code>
import net.gleske.jervis.remotes.creds.EphemeralTokenCache

// instantiates getPrivateKey for you
EphemeralTokenCache cred = new EphemeralTokenCache({-&gt; new File('path/to/private_key').text })

// alternately you can manually set it
cred.getPrivateKey = {-&gt; new File('path/to/private_key').text }
</pre></code>

      @see net.gleske.jervis.tools.CipherMap
      */
    Closure getPrivateKey = {-> '' }

    /**
      The time buffer before a renewal is forced.  This is to account for clock
      drift and is customizable by the client.

      @default 30
      */
    Long renew_buffer = 30

    /**
      Customize the number of SHA-256 hash iterations performed during AES
      encryption operations.  Default: <tt>0</tt> iterations.

      @see net.gleske.jervis.tools.SecurityIO#DEFAULT_AES_ITERATIONS
      */
    Integer hash_iterations = 0

    /**
      Returns renew buffer.  Does not allow renew buffer to be undefined or go below zero.

      @return <tt>0</tt> or a <tt>renew_buffer</tt> greater than <tt>0</tt>.
      */
    Long getRenew_buffer() {
        if(!renew_buffer || renew_buffer <= 0) {
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
        if(!this.cache) {
            tryLock {
                tryLoadCache()
            }
        }
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
            (entry in Map) &&
            (!entry?.expires_at || isExpired(Instant.parse(entry.expires_at)))
        }.collect { hash, entry ->
            hash
        } ?: []

        // Delete expired entries from the cache.
        cleanup.each { hash ->
            this.cache.remove(hash)
        }
    }

    /**
      A new token has been issued so this method will update the backend cache
      to store the new token.  This method will also remove expired tokens
      before persisting the cache.
      */
    void updateTokenWith(String token, String expiration, String hash) {
        this.hash = hash
        tryLock {
            tryLoadCache()
            this.cache[hash].token = token
            setExpiration(expiration)
            this.cache[hash].expires_at = expiration
            // Removes expired cache entries
            cleanupCache()
            trySaveCache()
        }
    }

    /**
      Sets the expiration for a given token.
      @param expiration An ISO instant formatted string like <tt>{@link java.time.Instant#toString()}</tt>.
      */
    void setExpiration(String expiration) {
        // A quick parse check
        Instant.parse(expiration)
        this.cache[this.hash].expires_at = expiration
    }

    /**
      Gets expiration of the token.
      */
    String getExpiration() {
        this.cache[this.hash]?.expires_at
    }

    /**
      Gets the access token used for API authentication retrieved from the
      cache.  Before calling this, you must call <tt>isExpired</tt> or
      <tt>updateTokenWith</tt> method first otherwise <tt>null</tt> will be
      returned.

      @see #isExpired(java.lang.String)
      @see #updateTokenWith(java.lang.String, java.lang.String, java.lang.String)
      @return Returns a valid API token meant to be used in authentication.
      */
    String getToken() {
        this.cache[this.hash]?.token
    }
}
