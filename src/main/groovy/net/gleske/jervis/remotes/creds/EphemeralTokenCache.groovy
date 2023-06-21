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

import net.gleske.jervis.exceptions.TokenException
import net.gleske.jervis.remotes.interfaces.EphemeralTokenCredential
import net.gleske.jervis.tools.CipherMap
import net.gleske.jervis.tools.LockableFile
import net.gleske.jervis.tools.YamlOperator

import java.time.Instant
import java.time.format.DateTimeParseException

/**
  A flexible token cache with pluggable backend storage which encrypts the
  cache before storing it in the backend.  By default, this uses a Linux file-based
  backend storage; refer to <tt>{@link #cacheFile}</tt>.  This is intentionally
  designed for shared use by multiple unrelated token issuers.  This provides
  centralized generic storage for all ephemeral tokens.

  This class, when instantiated, represents a single token which has an expiration
  and automated rotation.  The backend cache is a shared resource amongst many
  unique tokens with expirations.  The purpose is to reduce API calls to 3rd party
  services to request new tokens be issued.  Reuse already issued tokens if they're
  still valid or request a rotation for tokens that have expired.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

  <p>Basic sample usage using keys on disk for storage.</p>
<pre><code>
import net.gleske.jervis.remotes.creds.EphemeralTokenCache

EphemeralTokenCache tokenCred = new EphemeralTokenCache('src/test/resources/rsa_keys/good_id_rsa_4096')
</code></pre>

  <p>However, ideally you would load the private key from a more secure
  credential backend such as HashiCorp Vault or Jenkins.  The following example
  is for reading the credential from Jenkins.</p>

<pre><code>
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins

import net.gleske.jervis.exceptions.KeyPairDecodeException
import net.gleske.jervis.remotes.creds.EphemeralTokenCache

// Dynamically load an RSA private key from Jenkins.  Key must be 2048-bit or
// greater; 4096-bit recommended.
String credentialId = 'some-credential-id'
EphemeralTokenCache tokenCred = new EphemeralTokenCache({-&gt;
    CredentialsProvider.lookupCredentials(BasicSSHUserPrivateKey, Jenkins.instance, Jenkins.instance.ACL.SYSTEM).find {
        it.id == credentialId
    }.with { jenkinsCred -&gt;
        if(!jenkinsCred?.getPrivateKey()) {
            // Throw an exception because this class will disable encryption if
            // the credential does not exist i.e. returns null
            throw new KeyPairDecodeException("Private key ${credentialId} must not be null.")
        }
        // return private
        jenkinsCred.getPrivateKey()
    }
})
</code></pre>
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
      @see #obtainLock Instantiates obtainLock closure
      @see #saveCache Instantiates saveCache closure
      @see net.gleske.jervis.exceptions.TokenException
      @param resolvePrivateKeyString An executable <tt>Closure</tt> that takes
                                     no parameters and returns a PKCS1 or PKCS8
                                     PEM formatted RSA private key.  If this
                                     ever returns null or empty string, then a
                                     <tt>TokenException</tt> will be thrown.
      */
    EphemeralTokenCache(Closure resolvePrivateKeyString) throws TokenException {
        setupClosures(resolvePrivateKeyString)
    }

    /**
      An ephemeral token cache which provides no encryption at rest.  If using
      this, then you should update the <tt>loadCache</tt> and <tt>saveCache</tt>
      closures to use something other than a file-based backend.

      @see #EphemeralTokenCache(groovy.lang.Closure)
      @see #loadCache Instantiates loadCache closure
      @see #saveCache Instantiates saveCache closure
      @see net.gleske.jervis.exceptions.TokenException
      @Warning Encryption at rest is disabled.  This is intended to provide
               flexibility.  The cache entries contain sensitive API tokens and
               should be encrypted at rest; if not encrypted by
               <tt>CipherMap</tt>, then encrypted by the backend service.
      @param allowEmptyPrivateKey Uses a plain text cache if <tt>true</tt>.  It
                                  will throw a <tt>TokenException</tt> if
                                  <tt>false</tt>.
      */
    EphemeralTokenCache(Boolean allowEmptyPrivateKey) throws TokenException {
        this.allowEmptyPrivateKey = allowEmptyPrivateKey
        setupClosures({-> ''})
    }

    /**
      Creates an encrypted ephemeral token cache from a private key on disk.
      This assumes a private key is insecurely stored on disk if no 3rd party
      credential backend is used.

      <p>This constructor is provided for convenience and is equavalent to the
      following closure constructor.</p>

<pre><code>
new EphemeralTokenCache({-&gt; new File(privateKeyPath).text })
</code></pre>

      @Warning Because the private key is insecurely stored on disk this is not
               truly encryption at rest.  Encryption at rest requires the
               decryption keys are not immediately available in plain form.
               This constructor is intended to ease testing and provide an easy
               way to experiment with the class interactively.
      @see #EphemeralTokenCache(groovy.lang.Closure)
      @param resolvePrivateKeyString Path to a file which contains a PKCS1 or
                                     PKCS8 PEM formatted RSA private key.
      */
    EphemeralTokenCache(String privateKeyPath) {
        Closure resolvePrivateKeyString = {->
            new File(privateKeyPath).text
        }
        setupClosures(resolvePrivateKeyString)
    }

    /**
      Configures the load and save cache closures for a default file-based
      approach.  This occurs when a valid RSA key is provided.
      */
    private void setupClosures(Closure privateKeyClosure) {
        String privateKey = privateKeyClosure()
        if(privateKey) {
            // Quickly test validity and private key strength (it will throw an
            // exception for a weak key)
            new CipherMap(privateKey)
            this.getPrivateKey = privateKeyClosure
        }
        else if(!this.allowEmptyPrivateKey) {
            throw new TokenException('Private key is empty when user intends persistent cache to be encrypted.')
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
        this.obtainLock = { Closure body ->
            new LockableFile(this.cacheLockFile).withLock {
                body()
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
      Forces the user to declare they want an unencrypted cache.
      */
    private Boolean allowEmptyPrivateKey = false

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
            this.cipherMap = new CipherMap(privateKey, this.hash_iterations)
            if(data) {
                this.cipherMap << data
            }
        }
        else if(!this.allowEmptyPrivateKey) {
            throw new TokenException('Private key is empty when user intends persistent cache to be encrypted.')
        }
        if(this.cipherMap) {
            temp = this.cipherMap.getPlainMap()
        }
        else if(data) {
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
        if(loadCache && saveCache && obtainLock) {
            obtainLock(body)
        }
        else {
            body()
        }
    }

    /**
      The path to a lock file which serializes read and write access to
      persistent cache where issued tokens are stored.

      @default <tt>/dev/shm/jervis-token-cache.lock</tt>
      @see <a href="https://docs.kernel.org/filesystems/tmpfs.html" target=_blank><tt>/dev/shm</tt> filesystem</a> which is in-memory file storage so very fast
      @see #cacheFile
      @see #obtainLock
      */
    String cacheLockFile = '/dev/shm/jervis-token-cache.lock'

    /**
      The path to the persistent cache file if this class is initialized with
      encryption at rest.

      @default <tt>/dev/shm/jervis-token-cache.yaml</tt>
      @see <a href="https://docs.kernel.org/filesystems/tmpfs.html" target=_blank><tt>/dev/shm</tt> filesystem</a> which is in-memory file storage so very fast
      */
    String cacheFile = '/dev/shm/jervis-token-cache.yaml'

    /**
      A centralized lock obtained in order to serialize loading and persisting
      backend cache.  Since the backend cache is file-based, this lock is also
      file-based by default.  You can replace the default with a distributed
      lock.
      @default
<pre><code>
{ Closure body -&gt;
    new LockableFile(this.cacheLockFile).withLock {
        body()
    }
}
</code></pre>
      @see #cacheLockFile
      @see net.gleske.jervis.tools.LockableFile
      */
    Closure obtainLock

    /**
      A closure which should return a <tt>String</tt> from loading the cache.
      Persistent caching of tokens is optional.  Before this is called a file
      lock is obtained on a lock file.
      @default
<pre><code>
{-&gt;
    File f = new File(this.cacheFile)
    if(!f.exists()) {
        return ''
    }
    f.text
}
</code></pre>
      @see #cacheFile
      @see #cacheLockFile
      */
    Closure loadCache

    /**
      A closure which which should support a <tt>String</tt> parameter used to
      optionally persist a cache.  Before this is called a file lock is obtained on a lock file.
      @default
<pre><code>
{ String cache -&gt;
    File f = new File(this.cacheFile)
    // initialize file with private permissions
    if(!f.exists()) {
        ['/bin/sh', '-ec', "touch '&#36;{this.cacheFile}'; chmod 600 '&#36;{this.cacheFile}'"].execute()
    }
    // write out cache
    f.withWriter('UTF-8') { Writer w -&gt;
        w &lt;&lt; cache
    }
}
</code></pre>
      @see #cacheFile
      @see #cacheLockFile
      */
    Closure saveCache

    /**
      A closure that returns a PKCS1 or PKCS8 PEM formatted RSA private key as
      a <tt>String</tt> used to encipher the cache for encryption at rest.

      <h2>Sample usage</h2>

      <p>This assumes a private key is insecurely stored as a file.  Ideally,
      the private key is stored in a 3rd party credentials backend.</p>

<pre><code>
import net.gleske.jervis.remotes.creds.EphemeralTokenCache

// instantiates getPrivateKey for you
EphemeralTokenCache cred = new EphemeralTokenCache({-&gt; new File('path/to/private_key').text })

// alternately you can manually set it
cred.getPrivateKey = {-&gt; new File('path/to/private_key').text }
</pre></code>

      @see net.gleske.jervis.tools.CipherMap CipherMap provides encryption at rest
      */
    Closure getPrivateKey = {-> '' }

    /**
      The time buffer before a renewal is forced.  This is to account for clock
      drift and is customizable by the client.  The value is number of seconds.

      @default 30
      */
    Long renew_buffer = 30

    /**
      Customize the number of SHA-256 hash iterations performed during AES
      encryption operations.  Due to encryption strength, <tt>CipherMap</tt>
      automatically rotating keys, and the short-lived nature of the ephemeral
      tokens this value is lowered from the <tt>DEFAULT_AES_ITERATIONS</tt>.

      @default 0
      @see net.gleske.jervis.tools.SecurityIO#DEFAULT_AES_ITERATIONS
      @see net.gleske.jervis.tools.CipherMap CipherMap provides encryption at rest
      */
    Integer hash_iterations = 0

    /**
      Set cache file location when the cache is saved to local disk.

      @see #cacheFile
      @param cacheFile A local filesystem path to a YAML file.  The contents
                       will be YAML.
      */
    void setCacheFile(String cacheFile) {
        if(cacheFile == this.cacheLockFile) {
            throw new TokenException('cacheFile and cacheLockFile must not be the same.')
        }
        this.cacheFile = cacheFile
    }

    /**
      Set cache lock file location when the cache is saved to local disk.

      @see #cacheLockFile
      @param cacheLockFile A local filesystem path to a file.  This will be used
                           for file locking to serialize reading and updating
                           the local <tt>{@link #cacheFile}</tt>.
      */
    void setCacheLockFile(String cacheLockFile) {
        if(cacheLockFile == this.cacheFile) {
            throw new TokenException('cacheFile and cacheLockFile must not be the same.')
        }
        this.cacheLockFile = cacheLockFile
    }

    /**
      Returns renew buffer for the current token.

      @see #renew_buffer
      @return <tt>0</tt> or a <tt>renew_buffer</tt> greater than <tt>0</tt>.
      */
    Long getRenew_buffer() {
        this.cache[this.hash]?.renew_buffer ?: this.renew_buffer
    }

    /**
      Sets a renew buffer.  Does not allow renew buffer to be undefined or go below zero.
      */
    void setRenew_buffer(Long renew_buffer) {
        if(!renew_buffer || renew_buffer <= 0) {
            this.renew_buffer = 0
        }
        else {
            this.renew_buffer = renew_buffer
        }
    }


    /**
      Checks if a token is expired.

      @see #getRenew_buffer()
      @see #isExpired(java.time.Instant)
      @param hash A hash used for performing a lookup on an internal token
                  cache.
      @return Returns <tt>true</tt> if the issued token is expired requiring
              another to be issued.
      */
    Boolean isExpired(String hash) {
        this.hash = hash
        if(!this.cache) {
            tryLock {
                tryLoadCache()
            }
        }
        if(!getExpiration()) {
            return true
        }
        isExpired()
    }

    /**
      Checks if a token is expired.

      @see #getRenew_buffer()
      @see #isExpired(java.time.Instant)
      @return Returns <tt>true</tt> if the issued token is expired requiring
              another to be issued.
      */
    Boolean isExpired() {
        if(!getExpiration()) {
            return true
        }
        isExpired(Instant.parse(getExpiration()))
    }

    /**
      Checks if the provided time has passed.

      @see #getRenew_buffer()
      @see java.time.Instant#now()
      @param expires Check if this instant is expired based on
                     <tt>getRenew_buffer()</tt> and <tt>Instant.now</tt>.
      @return Returns <tt>true</tt> if the GitHub token is expired requiring
              another to be issued.
      */
    Boolean isExpired(Instant expires) {
        checkExpiration(expires, getRenew_buffer())
    }

    /**
      Similar to other isExpired methods but allows passing in the renew_buffer
      as an argument.
      @return <tt>true</tt> if expired or <tt>false</tt> if valid.
      */
    private Boolean checkExpiration(Instant expires, Long renew_before) {
        Long renewBefore = (renew_before > 0) ? renew_before : 0
        Long renewAt = expires.epochSecond - renewBefore
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
            if(!(entry in Map) || !entry?.expires_at) {
                return true
            }
            Long renewBefore = entry?.renew_buffer ?: 0
            checkExpiration(Instant.parse(entry.expires_at), renewBefore)
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
      @see #getRenew_buffer()
      @see java.time.format.DateTimeParseException
      */
    void updateTokenWith(String token, String expiration, String hash) throws DateTimeParseException {
        if(isExpired(Instant.parse(expiration))) {
            throw new TokenException("Cannot update cache with an already expired token.  You may want to adjust the renew_buffer below ${this.renew_buffer} second(s).")
        }
        if(!token) {
            throw new TokenException('Updated token must not be empty or null.')
        }
        this.hash = hash
        tryLock {
            tryLoadCache()
            this.cache[hash].token = token
            setExpiration(expiration)
            this.cache[hash].expires_at = expiration
            this.cache[hash].renew_buffer = this.renew_buffer
            // Removes expired cache entries
            cleanupCache()
            trySaveCache()
        }
    }

    /**
      Sets the expiration for a given token.
      @see java.time.Instant#toString()
      @see java.time.format.DateTimeParseException
      @param expiration An ISO instant formatted string like
                        <tt>Instant.toString</tt>.
      */
    void setExpiration(String expiration) throws DateTimeParseException {
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
