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
//the EphemeralTokenCacheTest() class automatically sees the EphemeralTokenCache() class because they're in the same package

import net.gleske.jervis.exceptions.TokenException
import net.gleske.jervis.tools.YamlOperator

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.junit.After
import org.junit.Before
import org.junit.Test

class EphemeralTokenCacheTest extends GroovyTestCase {

    EphemeralTokenCache tokenCache

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        Boolean usePlainTextCache = true
        this.tokenCache = new EphemeralTokenCache(usePlainTextCache)
        // in-memory cache by default
        tokenCache.loadCache = null
        tokenCache.saveCache = null
        tokenCache.obtainLock = null
    }
    //tear down after every test
    @After protected void tearDown() {
        this.tokenCache = null
        super.tearDown()
    }
    @Test public void test_EphemeralTokenCache_fail_instantiation() {
        shouldFail(IllegalStateException) {
            new EphemeralTokenCache()
        }
    }
    @Test public void test_EphemeralTokenCache_fail_empty_private_key() {
        shouldFail(TokenException) {
            new EphemeralTokenCache({-> ''})
        }
        // read "empty" file
        shouldFail(TokenException) {
            new EphemeralTokenCache('/dev/null')
        }
    }
    @Test public void test_EphemeralTokenCache_empty_private_key() {
        assert this.tokenCache.getPrivateKey() == ''
    }
    @Test public void test_EphemeralTokenCache_isExpired_without_existing() {
        String hash = 'fake'
        assert this.tokenCache.isExpired(hash) == true
    }
    @Test public void test_EphemeralTokenCache_isExpired_with_existing() {
        String hash = 'fake'
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()

        // without 30 second renew buffer
        this.tokenCache.renew_buffer = 0
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, hash)
        assert this.tokenCache.isExpired(hash) == false

        // with 30 second renew buffer
        this.tokenCache.renew_buffer = 30
        assert this.tokenCache.isExpired(hash) == true
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_protect_renew_buffer_misconfiguration() {
        String hash = 'fake'
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        shouldFail(TokenException) {
            this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, hash)
        }
        this.tokenCache.renew_buffer = 0
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, hash)
        assert this.tokenCache.token == 'sometoken'
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_negative_renew_buffer_misconfiguration() {
        assert this.tokenCache.renew_buffer == 30
        this.tokenCache.renew_buffer = -1
        assert this.tokenCache.renew_buffer == 0
        this.tokenCache.renew_buffer = null
        assert this.tokenCache.renew_buffer == 0
        this.tokenCache.renew_buffer = 0
        assert this.tokenCache.renew_buffer == 0
        this.tokenCache.renew_buffer = 5
        assert this.tokenCache.renew_buffer == 5
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_mixed_cache_with_renew_buffers_and_cleanup() {
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        String thirtyFiveSecondsFromNow = Instant.now().plus(35, ChronoUnit.SECONDS).toString()

        this.tokenCache.renew_buffer = 0
        shouldFail(TokenException) {
            this.tokenCache.updateTokenWith('', tenSecondsFromNow, '10sHash')
        }
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        this.tokenCache.renew_buffer = 30
        this.tokenCache.updateTokenWith('sometoken2', thirtyFiveSecondsFromNow, '30sHash')

        assert this.tokenCache.cache.keySet().toList() == ['10sHash', '30sHash']
        assert this.tokenCache.cache['10sHash'].renew_buffer == 0
        assert this.tokenCache.cache['30sHash'].renew_buffer == 30
        assert this.tokenCache.token == 'sometoken2'
        // check automated cleanup of expired tokens
        this.tokenCache.cache['10sHash'].renew_buffer = 30
        this.tokenCache.updateTokenWith('sometoken3', thirtyFiveSecondsFromNow, '30sHash')
        assert this.tokenCache.cache.keySet().toList() == ['30sHash']
        assert this.tokenCache.cache['30sHash'].renew_buffer == 30
        assert this.tokenCache.token == 'sometoken3'
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_cleanup_remove_type_mismatch() {
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        String thirtyFiveSecondsFromNow = Instant.now().plus(35, ChronoUnit.SECONDS).toString()

        this.tokenCache.renew_buffer = 0
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        this.tokenCache.cache['customType'] = 'somestring'
        this.tokenCache.cache['10sHash'].renew_buffer = 30
        assert this.tokenCache.cache.keySet().toList() == ['10sHash', 'customType']
        this.tokenCache.renew_buffer = 30
        this.tokenCache.updateTokenWith('sometoken2', thirtyFiveSecondsFromNow, '30sHash')
        assert this.tokenCache.cache.keySet().toList() == ['30sHash']
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_cleanup_remove_when_missing_expiration() {
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        String thirtyFiveSecondsFromNow = Instant.now().plus(35, ChronoUnit.SECONDS).toString()

        this.tokenCache.renew_buffer = 0
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        this.tokenCache.cache['10sHash'].remove('expires_at')
        assert this.tokenCache.cache.keySet().toList() == ['10sHash']
        this.tokenCache.renew_buffer = 30
        this.tokenCache.updateTokenWith('sometoken2', thirtyFiveSecondsFromNow, '30sHash')
        assert this.tokenCache.cache.keySet().toList() == ['30sHash']
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_setCacheFile() {
        assert this.tokenCache.cacheFile == '/dev/shm/jervis-token-cache.yaml'
        shouldFail(TokenException) {
            this.tokenCache.cacheFile = '/dev/shm/jervis-token-cache.lock'
        }
        this.tokenCache.cacheFile = 'hello.yaml'
        assert this.tokenCache.cacheFile == 'hello.yaml'
    }
    @Test public void test_EphemeralTokenCache_updateTokenWith_setCacheLockFile() {
        assert this.tokenCache.cacheLockFile == '/dev/shm/jervis-token-cache.lock'
        shouldFail(TokenException) {
            this.tokenCache.cacheLockFile = '/dev/shm/jervis-token-cache.yaml'
        }
        this.tokenCache.cacheLockFile = 'hello.yaml'
        assert this.tokenCache.cacheLockFile == 'hello.yaml'
    }
    @Test public void test_EphemeralTokenCache_null_expiration() {
        assert this.tokenCache.expiration == null
        assert this.tokenCache.isExpired() == true
        assert this.tokenCache.isExpired('fakehash') == true
    }
    @Test public void test_EphemeralTokenCache_persistent_cache() {
        // potentially clean up files on disk before test
        new File('build/tmp/cache.yaml').with { f ->
            if(f.exists()) {
                f.delete()
            }
        }

        Boolean usePlainTextCache = true

        // create new instance with plain text file backend
        this.tokenCache = new EphemeralTokenCache(usePlainTextCache)
        this.tokenCache.cacheFile = 'build/tmp/cache.yaml'
        this.tokenCache.cacheLockFile = 'build/tmp/cache.lock'
        this.tokenCache.renew_buffer = 0

        // update a token
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        assert (new File('build/tmp/cache.yaml').exists())
        assert !this.tokenCache.isExpired()

        // corrupt in-memory cache but it should be ignored if checking against hash.
        this.tokenCache.cache['10sHash'].renew_buffer = 30
        assert this.tokenCache.isExpired()
        assert this.tokenCache.isExpired('10sHash')
        this.tokenCache.cache = [:]
        // pull from cache on disk
        assert !this.tokenCache.isExpired('10sHash')

        // load from persistent cache and update it
        String thirtyFiveSecondsFromNow = Instant.now().plus(35, ChronoUnit.SECONDS).toString()
        // pull from cache on disk and update
        this.tokenCache.cache = [:]
        this.tokenCache.updateTokenWith('sometoken2', thirtyFiveSecondsFromNow, '30sHash')
        assert this.tokenCache.cache.keySet().toList() == ['10sHash', '30sHash']

        // verify cache is plain text
        Map dataOnDisk = YamlOperator.loadYamlFrom(new File('build/tmp/cache.yaml'))
        dataOnDisk.keySet().toList() == ['10sHash', '30sHash']
    }

    @Test public void test_EphemeralTokenCache_encrypted_persistent_cache_closure() {
        // potentially clean up files on disk before test
        new File('build/tmp/cache-encrypted.yaml').with { f ->
            if(f.exists()) {
                f.delete()
            }
        }

        // create new instance with plain text file backend
        this.tokenCache = new EphemeralTokenCache({->
            this.getClass().getResource('/rsa_keys/good_id_rsa_2048').content.text
        })
        this.tokenCache.cacheFile = 'build/tmp/cache-encrypted.yaml'
        this.tokenCache.cacheLockFile = 'build/tmp/cache.lock'
        this.tokenCache.renew_buffer = 0

        // update a token
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        assert (new File('build/tmp/cache-encrypted.yaml').exists())
        assert !this.tokenCache.isExpired()

        // corrupt in-memory cache but it should be ignored if checking against hash.
        this.tokenCache.cache = [:]
        // pull from cache on disk
        assert !this.tokenCache.isExpired('10sHash')

        // throw decryption error due to unexpected empty private key
        this.tokenCache.cache = [:]
        this.tokenCache.getPrivateKey = {-> ''}
        shouldFail(TokenException) {
            this.tokenCache.isExpired('10sHash')
        }
    }
    @Test public void test_EphemeralTokenCache_encrypted_persistent_cache_file() {
        // potentially clean up files on disk before test
        new File('build/tmp/cache-encrypted.yaml').with { f ->
            if(f.exists()) {
                f.delete()
            }
        }

        // create new instance with plain text file backend
        this.tokenCache = new EphemeralTokenCache('src/test/resources/rsa_keys/good_id_rsa_2048')
        this.tokenCache.cacheFile = 'build/tmp/cache-encrypted.yaml'
        this.tokenCache.cacheLockFile = 'build/tmp/cache.lock'
        this.tokenCache.renew_buffer = 0

        // update a token
        String tenSecondsFromNow = Instant.now().plus(10, ChronoUnit.SECONDS).toString()
        this.tokenCache.updateTokenWith('sometoken', tenSecondsFromNow, '10sHash')
        assert (new File('build/tmp/cache-encrypted.yaml').exists())
        assert !this.tokenCache.isExpired()

        // load from persistent cache and update it
        String thirtyFiveSecondsFromNow = Instant.now().plus(35, ChronoUnit.SECONDS).toString()
        // pull from cache on disk and update
        this.tokenCache.cache = [:]
        this.tokenCache.updateTokenWith('sometoken2', thirtyFiveSecondsFromNow, '30sHash')
        assert this.tokenCache.cache.keySet().toList() == ['10sHash', '30sHash']

        // verify cache is encrypted by CipherMap
        Map dataOnDisk = YamlOperator.loadYamlFrom(new File('build/tmp/cache-encrypted.yaml'))
        dataOnDisk.keySet().toList() == ['age', 'cipher', 'data', 'signature']
    }
}
