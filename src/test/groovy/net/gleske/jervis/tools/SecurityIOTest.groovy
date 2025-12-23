/*
   Copyright 2014-2025 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.tools
//the SecurityIOTest() class automatically sees the SecurityIO() class because they're in the same package

import static net.gleske.jervis.tools.SecurityIO.avoidTimingAttack
import net.gleske.jervis.exceptions.DecryptException
import net.gleske.jervis.exceptions.EncryptException
import net.gleske.jervis.exceptions.KeyPairDecodeException
import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.tools.YamlOperator

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test

class SecurityIOTest extends GroovyTestCase {
    def jervis_tmp
    def security
    /**
      Executes a closure of code and returns its timing in milliseconds.
      @param c A closure to execute.
      @return Returns the number of milliseconds the timed closure took to execute.
      */
    private Long timing(Closure c) {
        Long before = Instant.now().toEpochMilli()
        c()
        Long after = Instant.now().toEpochMilli()
        after - before
    }

    /**
      Executes a closure of code up to a limit of retries.
      @param limit The max number of retries.
      @param body A closure to execute that must return a <tt>Boolean</tt>
                  <tt>true</tt> to retry or <tt>false</tt> to stop retrying.
      */
    private void maxRetries(Integer limit, Closure body) {
        Integer current = 0
        while({->
            if(current > limit) {
                return false
            }
            body()
        }()) continue
    }
    //set up before every test
    @Before protected void setUp() {
        security = new SecurityIO()
    }
    //tear down after every test
    @After protected void tearDown() {
        //Path.deleteDir() method was introduced in Groovy 2.3.11 and later
        //jervis_tmp.deleteDir()
        //we must support Groovy 1.8.9 and later due to a Jenkins limitation
        def stderr = new StringBuilder()
        def proc = ['rm','-rf',jervis_tmp.toString()].execute()
        proc.waitForProcessOutput(null, stderr)
        if(proc.exitValue()) {
            throw new IOException(stderr.toString())
        }
        jervis_tmp = null
        security = null
    }
    @Test public void test_SecurityIO_init_default() {
        security = new SecurityIO()
        assert !security.key_pair
    }
    @Test public void test_SecurityIO_init_private_pem() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        assert security.rsa_keysize == 2048
    }
    //test SecurityIO().decodeBase64()
    @Test public void test_SecurityIO_decodeBase64String() {
        def s = 'data'
        String encoded = s.bytes.encodeBase64().toString()
        assert 'ZGF0YQ==' == encoded
        assert security.decodeBase64String(encoded) == s
    }
    @Test public void test_SecurityIO_decodeBase64Bytes() {
        def s = 'data'
        String encoded = s.bytes.encodeBase64().toString()
        assert 'ZGF0YQ==' == encoded
        assert security.decodeBase64Bytes(encoded) == s.bytes
    }
    @Test public void test_SecurityIO_encodeBase64String() {
        assert 'ZGF0YQ==' == security.encodeBase64('data')
    }
    @Test public void test_SecurityIO_encodeBase64Bytes() {
        assert 'ZGF0YQ==' == security.encodeBase64('data'.bytes)
    }
    @Test public void test_SecurityIO_rsaEncrypt_rsaDecrypt() {
        String plaintext = 'secret message'
        String ciphertext
        String decodedtext
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security.key_pair = url.content.text
        ciphertext = security.rsaEncrypt(plaintext)
        assert ciphertext.length() > 0
        decodedtext = security.rsaDecrypt(ciphertext)
        assert plaintext == decodedtext
    }
    @Test public void test_SecurityIO_fail_rsaEncrypt() {
        shouldFail(EncryptException) {
            def ciphertext = security.rsaEncrypt('some text')
        }
    }
    @Test public void test_SecurityIO_fail_rsaDecrypt() {
        shouldFail(DecryptException) {
            def decodedtext = security.rsaDecrypt('some text')
        }
    }
    @Test public void test_SecurityIO_isSecureField_map_nonsecure() {
        Map myobj = new HashMap()
        myobj.put('someprop', 'somevalue')
        assert false == security.isSecureField(myobj)
    }
    @Test public void test_SecurityIO_isSecureField_nonmap() {
        assert false == security.isSecureField([])
    }
    @Test public void test_SecurityIO_isSecureField_map_secure() {
        Map myobj = new HashMap()
        myobj.put('secure', 'somevalue')
        assert true == security.isSecureField(myobj)
    }
    @Test public void test_SecurityIO_load_key_pair() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        assert !security.key_pair
        security.key_pair = url.content.text
        assert security.key_pair
        assert security.rsa_keysize == 2048
        url = this.getClass().getResource('/rsa_keys/good_id_rsa_4096')
        security.key_pair = url.content.text
        assert security.rsa_keysize == 4096
    }
    @Test public void test_SecurityIO_load_pkcs8_key() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_pkcs8_2048')
        assert !security.key_pair
        security.key_pair = url.content.text
        assert security.key_pair
        assert security.rsa_keysize == 2048
        url = this.getClass().getResource('/rsa_keys/good_id_rsa_pkcs8_4096')
        security.key_pair = url.content.text
        assert security.rsa_keysize == 4096
    }
    @Test public void test_SecurityIO_bad_key_pair() {
        shouldFail(KeyPairDecodeException) {
            security.key_pair = "bad RSA key"
        }
    }
    @Test public void test_SecurityIO_unsupported_key_pair() {
        shouldFail(KeyPairDecodeException) {
            URL url = this.getClass().getResource('/rsa_keys/unsupported_crt_pair');
            security.key_pair = url.content.text
        }
    }
    @Test public void test_SecurityIO_serialization() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        assert !security.key_pair
        security.key_pair = url.content.text
        assert security.key_pair
        def ciphertext = security.rsaEncrypt('some text')
        def plaintext = security.rsaDecrypt(ciphertext)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(security)
    }
    @Test public void test_SecurityIO_fail_on_weak_keys() {
        URL url = this.getClass().getResource('/rsa_keys/bad_id_rsa_1024')
        assert !security.key_pair
        shouldFail(SecurityException) {
            security.key_pair = url.content.text
        }
    }

    @Test public void test_SecurityIO_fail_on_weak_pkcs8_keys() {
        URL url = this.getClass().getResource('/rsa_keys/bad_id_rsa_pkcs8_1024')
        assert !security.key_pair
        shouldFail(SecurityException) {
            security.key_pair = url.content.text
        }
    }

    @Test public void test_SecurityIO_signRS256Base64Url_and_verifyJsonWebToken() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // Create a valid JWT header and payload for signature verification
        String header = security.encodeBase64Url('{"alg":"RS256","typ":"JWT"}')
        String payload = security.encodeBase64Url('{"sub":"test","iat":1234567890}')
        String data = "${header}.${payload}"
        String signature = security.signRS256Base64Url(data)
        String jwt_like = "${data}.${signature}"
        assert true == security.verifyJsonWebToken(jwt_like)
    }

    @Test public void test_SecurityIO_verifyJsonWebToken_fail_to_verify() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // Create a valid JWT header but sign different data to fail signature verification
        String header = security.encodeBase64Url('{"alg":"RS256","typ":"JWT"}')
        String payload = security.encodeBase64Url('{"sub":"test","iat":1234567890}')
        String data = "${header}.${payload}"
        String signature = security.signRS256Base64Url(data)
        // Tamper with the payload to cause signature verification failure
        String tamperedPayload = security.encodeBase64Url('{"sub":"tampered","iat":1234567890}')
        String jwt_like = "${header}.${tamperedPayload}.${signature}"
        assert false == security.verifyJsonWebToken(jwt_like)
    }

    @Test public void test_SecurityIO_encodeBase64UrlBytes() {
        // this data should include both + and / characters
        byte[] data = '~~?~asdf~~?~asdf'.bytes
        String base64 = data.encodeBase64().toString()
        assert base64.contains('+') == true
        assert base64.contains('/') == true
        assert base64.tr('+/', '-_') == security.encodeBase64Url(data)
    }

    @Test public void test_SecurityIO_encodeBase64UrlString() {
        // this data should include both + and / characters
        String data = '~~?~asdf~~?~asdf'
        String base64 = data.bytes.encodeBase64().toString()
        assert base64.contains('+') == true
        assert base64.contains('/') == true
        assert base64.tr('+/', '-_') == security.encodeBase64Url(data)
    }

    @Test public void test_SecurityIO_decodeBase64UrlBytes() {
        // this data should include both - and _ characters
        byte[] data = '~~?~asdf~~?~asdf'.bytes
        String base64url = data.encodeBase64().toString().tr('+/', '-_')
        assert base64url.contains('-') == true
        assert base64url.contains('_') == true
        assert data == security.decodeBase64UrlBytes(base64url)
    }

    @Test public void test_SecurityIO_decodeBase64UrlString() {
        // this data should include both - and _ characters
        String data = '~~?~asdf~~?~asdf'
        String base64url = data.bytes.encodeBase64().toString().tr('+/', '-_')
        assert base64url.contains('-') == true
        assert base64url.contains('_') == true
        assert data == security.decodeBase64UrlString(base64url)
    }

    @Test public void test_SecurityIO_getGitHubJWT() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt_token = security.getGitHubJWT('1234')
        assert true == security.verifyJsonWebToken(jwt_token)
        String header
        Map payload
        jwt_token.tokenize('.').with {
            header = security.decodeBase64String(it[0])
            payload = YamlOperator.loadYamlFrom(security.decodeBase64Bytes(it[1]))
        }
        assert header == '{"alg":"RS256","typ":"JWT"}'
        assert 'iat' in payload
        assert 'exp' in payload
        assert 'iss' in payload
        assert payload.iss == '1234'
        // default is 10 minute token duration
        assert (payload.exp - payload.iat) == 600
    }
    @Test public void test_SecurityIO_getGitHubJWT_min_expire() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt_token = security.getGitHubJWT('1234', -1, 20)
        assert true == security.verifyGitHubJWTPayload(jwt_token)
    }

    @Test public void test_SecurityIO_getGitHubJWT_max_expire() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // Request a 20 minute token but we're only issued a 10 minute token
        // 10 minute and 1 second drift set
        String jwt_token = security.getGitHubJWT('1234', 20, 601)
        // Verify the payload is expired with no payload drift.  If 20 minutes
        // were allowed then the token would be valid but because the max is 10
        // minutes, then this should show expired.
        assert false == security.verifyGitHubJWTPayload(jwt_token, 0)
    }

    @Test public void test_SecurityIO_getGitHubJWT_expire() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt_token = security.getGitHubJWT('1234', 2, 30)
        Map payload = YamlOperator.loadYamlFrom(security.decodeBase64Bytes(jwt_token.tokenize('.')[1]))
        Long now = Instant.now().getEpochSecond()

        // validate we our JWT is not expired
        assert now > payload.iat
        assert now < payload.exp
        // 2 minute token duration
        assert (payload.exp - payload.iat) == 120
    }

    @Test public void test_SecurityIO_getGitHubJWT_expire_and_drift() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt_token = security.getGitHubJWT('1234', 1, 120)
        Map payload = YamlOperator.loadYamlFrom(security.decodeBase64Bytes(jwt_token.tokenize('.')[1]))
        Long now = Instant.now().getEpochSecond()

        // Verify due to drift and expiration our JWT is expired
        assert now > payload.iat
        assert now > payload.exp
        // 1 minute token duration
        assert (payload.exp - payload.iat) == 60
    }

    @Test public void test_SecurityIO_verifyGitHubJWTPayload_bad_signature() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // Create a valid JWT header but sign different data to fail signature verification
        String header = security.encodeBase64Url('{"alg":"RS256","typ":"JWT"}')
        String payload = security.encodeBase64Url('{"sub":"test","iat":1234567890}')
        String data = "${header}.${payload}"
        String signature = security.signRS256Base64Url(data)
        // Tamper with the payload to cause signature verification failure
        String tamperedPayload = security.encodeBase64Url('{"sub":"tampered","iat":1234567890}')
        String jwt_like = "${header}.${tamperedPayload}.${signature}"
        assert false == security.verifyGitHubJWTPayload(jwt_like)
    }

    @Test public void test_SecurityIO_verifyGitHubJWTPayload_valid() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt = security.getGitHubJWT('1234')
        assert true == security.verifyGitHubJWTPayload(jwt)
    }

    @Test public void test_SecurityIO_verifyGitHubJWTPayload_expired_with_drift() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt = security.getGitHubJWT('1234', 1, 40)
        assert false == security.verifyGitHubJWTPayload(jwt)
    }

    @Test public void test_SecurityIO_verifyGitHubJWTPayload_valid_without_drift() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt = security.getGitHubJWT('1234', 1, 40)
        assert true == security.verifyGitHubJWTPayload(jwt, 0)
    }
    @Test public void test_SecurityIO_verifyGitHubJWTPayload_invalid_with_negative_drift() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String jwt = security.getGitHubJWT('1234', 1, 40)
        assert false == security.verifyGitHubJWTPayload(jwt, -60)
    }
    @Test public void test_SecurityIO_verifyRS256Base64Url() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        String data = 'dummy data'
        String signed = security.signRS256Base64Url(data)
        assert true == security.verifyRS256Base64Url(signed, data)
        assert false == security.verifyRS256Base64Url(signed, 'corrupt')
    }
    @Test public void test_SecurityIO_avoidTimingAttack_fixed() {
        Integer mysecret = 0
        // Assert benchmark
        Long runtime = timing {
            1+1
        }
        assert runtime < 10
        runtime = timing {
            // Force code to always take 50ms
            avoidTimingAttack(50) {
                mysecret = 1+1
            }
        }
        assert mysecret == 2
        assert runtime >= 50
        runtime = timing {
            // Force code to always take 70ms
            avoidTimingAttack(70) {
                mysecret = 1+1
            }
        }
        assert mysecret == 2
        assert runtime >= 70
    }
    @Test public void test_SecurityIO_avoidTimingAttack_random() {
        Integer mysecret = 0
        // Assert benchmark
        Long runtime = timing {
            2*2
        }
        assert runtime < 10
        maxRetries(5) {
            runtime = timing {
                // Force code to randomly delay up to 200ms
                avoidTimingAttack(-50) {
                    mysecret = 2*2
                }
            }
            runtime >= 55
        }
        assert mysecret == 4
        assert runtime >= 0
        // 55 to allow a 5ms overage buffer
        assert runtime < 55
        Long runtime2
        // Loop over randomness calculation with a limit of 5 iterations to
        // avoid randomly getting the same timing twice.
        maxRetries(5) {
            runtime2 = timing {
                // Force code to randomly delay up to 200ms
                avoidTimingAttack(-50) {
                    mysecret = 2*2
                }
            }
            runtime2 == runtime
        }
        assert mysecret == 4
        assert runtime2 >= 0
        // 55 to allow a 5ms overage buffer
        assert runtime2 < 55
        assert runtime2 != runtime
    }
    @Test public void test_SecurityIO_avoidTimingAttack_fixed_min_random_max_with_implicit_return() {
        Integer mysecret = 0
        // Assert benchmark
        Long runtime = timing {
            5*5
        }
        assert runtime < 10
        maxRetries(5) {
            runtime = timing {
                // Set an implicit value.  Minimum execution time is 100ms random between 100-200ms.
                mysecret = avoidTimingAttack(30) {
                    avoidTimingAttack(-50) {
                        5*5
                    }
                }
            }
            runtime >= 55
        }
        assert mysecret == 25
        assert runtime >= 30
        // 55 to allow a 5ms overage buffer
        assert runtime < 55
    }
    @Test public void test_SecurityIO_avoidTimingAttack_time_overflow() {
        Integer mysecret = 0
        // Assert benchmark
        Long runtime = timing {
            sleep(10)
        }
        assert runtime >= 10
        // Assert benchmark
        maxRetries(5) {
            runtime = timing {
                avoidTimingAttack(5) {
                    1+1
                }
            }
            runtime > 7
        }
        assert runtime <= 7
        // Test time overflow
        runtime = timing {
            avoidTimingAttack(5) {
                sleep(10)
            }
        }
        assert runtime >= 10
    }
    @Test public void test_SecurityIO_sha256Sum() {
        String desiredValue = '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824'
        assert SecurityIO.sha256Sum('hello') == desiredValue
    }
    @Test public void test_SecurityIO_null_keySize() {
        assert security.rsa_keysize == 0
    }
    @Test public void test_SecurityIO_randomBytes_size() {
        assert SecurityIO.randomBytes(5).size() == 5
        assert SecurityIO.randomBytes(2).size() == 2
    }
    @Test public void test_SecurityIO_randomBytesBase64_size() {
        assert SecurityIO.decodeBase64Bytes(SecurityIO.randomBytesBase64(5)).size() == 5
        assert SecurityIO.decodeBase64Bytes(SecurityIO.randomBytesBase64(2)).size() == 2
    }
    @Test public void test_SecurityIO_AES_main_cipher() {
        byte[] secret = SecurityIO.randomBytes(32)
        byte[] iv = SecurityIO.randomBytes(16)
        String plaintext = 'some secret message'

        byte[] ciphertext0 = SecurityIO.encryptWithAES256(secret, iv, plaintext, 0)
        assert plaintext != ciphertext0
        assert ciphertext0 == SecurityIO.encryptWithAES256(secret, iv, plaintext, 0)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext0, 0)

        byte[] ciphertext1 = SecurityIO.encryptWithAES256(secret, iv, plaintext, 1)
        assert ciphertext0 != ciphertext1
        assert ciphertext1 == SecurityIO.encryptWithAES256(secret, iv, plaintext, 1)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext1, 1)

        byte[] ciphertext2 = SecurityIO.encryptWithAES256(secret, iv, plaintext, 2)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext2, 2)

        byte[] ciphertext3 = SecurityIO.encryptWithAES256(secret, iv, plaintext, 3)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext3, 3)
        assert plaintext != SecurityIO.decryptWithAES256(secret, iv, ciphertext3, 0)
    }
    @Test public void test_SecurityIO_AES_short_secret() {
        // test with shortened input bytes
        byte[] secret = 'short'.bytes
        byte[] iv = SecurityIO.randomBytes(16)
        String plaintext = 'some secret message'
        byte[] ciphertext = SecurityIO.encryptWithAES256(secret, iv, plaintext, 0)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext, 0)
    }
    @Test public void test_SecurityIO_AES_Base64() {
        byte[] secret = SecurityIO.randomBytes(32)
        String secretB64 = SecurityIO.encodeBase64(secret)
        byte[] iv = SecurityIO.randomBytes(16)
        String ivB64 = SecurityIO.encodeBase64(iv)
        String plaintext = 'some secret message'
        byte[] ciphertext = SecurityIO.encryptWithAES256(secret, iv, plaintext, 0)
        String ciphertextB64 = SecurityIO.encodeBase64(ciphertext)
        assert ciphertextB64 == SecurityIO.encryptWithAES256Base64(secretB64, ivB64, plaintext, 0)
        assert plaintext == SecurityIO.decryptWithAES256(secret, iv, ciphertext, 0)
        assert plaintext == SecurityIO.decryptWithAES256Base64(secretB64, ivB64, ciphertextB64, 0)
    }
    @Test public void test_SecurityIO_AES_passphrase() {
        String passphrase = 'correct horse battery staple'
        String plaintext = 'https://xkcd.com/936/'
        String ciphertext = SecurityIO.encryptWithAES256(passphrase, plaintext, 0)
        assert ciphertext == SecurityIO.encryptWithAES256(passphrase, plaintext, 1)
        assert ciphertext != SecurityIO.encryptWithAES256(passphrase, plaintext, 2)
        assert plaintext == SecurityIO.decryptWithAES256(passphrase, ciphertext, 1)
    }
    @Test public void test_SecurityIO_isBase64() {
        assert SecurityIO.isBase64('') == true
        assert SecurityIO.isBase64('YQ==') == true
        assert SecurityIO.isBase64('YQ=') == false
        assert SecurityIO.isBase64('YQ\n==\n') == true
        assert SecurityIO.isBase64('hello world') == false
    }

    @Test public void test_SecurityIO_isBase64_invalid_characters() {
        // Test invalid base64 characters (triggers regex failure)
        assert SecurityIO.isBase64('!@#$') == false
        assert SecurityIO.isBase64('YQ==!') == false
        assert SecurityIO.isBase64('data with spaces') == false
        assert SecurityIO.isBase64('====') == false
    }

    // ==========================================================================
    // Tests for new RSA OAEP encryption methods
    // ==========================================================================

    @Test public void test_SecurityIO_rsaEncryptOaep_rsaDecryptOaep() {
        String plaintext = 'secret message with OAEP'
        String ciphertext
        String decodedtext
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security.key_pair = url.content.text
        ciphertext = security.rsaEncryptOaep(plaintext)
        assert ciphertext.length() > 0
        decodedtext = security.rsaDecryptOaep(ciphertext)
        assert plaintext == decodedtext
    }

    @Test public void test_SecurityIO_rsaEncryptBytesOaep_rsaDecryptBytesOaep() {
        byte[] plainbytes = 'secret bytes with OAEP'.bytes
        byte[] cipherbytes
        byte[] decodedbytes
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security.key_pair = url.content.text
        cipherbytes = security.rsaEncryptBytesOaep(plainbytes)
        assert cipherbytes.length > 0
        decodedbytes = security.rsaDecryptBytesOaep(cipherbytes)
        assert plainbytes == decodedbytes
    }

    @Test public void test_SecurityIO_rsaEncryptOaep_with_4096_key() {
        String plaintext = 'secret message with 4096-bit key'
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_4096')
        security.key_pair = url.content.text
        String ciphertext = security.rsaEncryptOaep(plaintext)
        assert ciphertext.length() > 0
        assert plaintext == security.rsaDecryptOaep(ciphertext)
    }

    @Test public void test_SecurityIO_fail_rsaEncryptOaep_no_keypair() {
        shouldFail(EncryptException) {
            security.rsaEncryptOaep('some text')
        }
    }

    @Test public void test_SecurityIO_fail_rsaDecryptOaep_no_keypair() {
        shouldFail(DecryptException) {
            security.rsaDecryptOaep('some text')
        }
    }

    @Test public void test_SecurityIO_fail_rsaEncryptBytesOaep_no_keypair() {
        shouldFail(EncryptException) {
            security.rsaEncryptBytesOaep('some text'.bytes)
        }
    }

    @Test public void test_SecurityIO_fail_rsaDecryptBytesOaep_no_keypair() {
        shouldFail(DecryptException) {
            security.rsaDecryptBytesOaep('some text'.bytes)
        }
    }

    @Test public void test_SecurityIO_rsaOaep_different_from_pkcs1() {
        // Verify OAEP and PKCS1 produce different ciphertext
        String plaintext = 'test message'
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security.key_pair = url.content.text
        String oaepCiphertext = security.rsaEncryptOaep(plaintext)
        String pkcs1Ciphertext = security.rsaEncrypt(plaintext)
        // Ciphertexts should be different (different padding schemes)
        assert oaepCiphertext != pkcs1Ciphertext
        // Both should decrypt to the same plaintext
        assert plaintext == security.rsaDecryptOaep(oaepCiphertext)
        assert plaintext == security.rsaDecrypt(pkcs1Ciphertext)
    }

    // ==========================================================================
    // Tests for new AES-256-GCM encryption methods
    // ==========================================================================

    @Test public void test_SecurityIO_AES256GCM_encrypt_decrypt() {
        byte[] secret = SecurityIO.randomBytes(32)
        String plaintext = 'secret message for GCM'
        byte[] ciphertext = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        assert ciphertext.length > 0
        // GCM ciphertext should be: 12 bytes nonce + plaintext + 16 bytes auth tag
        assert ciphertext.length >= 12 + plaintext.bytes.length + 16
        String decrypted = SecurityIO.decryptWithAES256GCM(secret, ciphertext)
        assert plaintext == decrypted
    }

    @Test public void test_SecurityIO_AES256GCM_random_nonce() {
        // Verify each encryption produces different ciphertext (random nonce)
        byte[] secret = SecurityIO.randomBytes(32)
        String plaintext = 'same message'
        byte[] ciphertext1 = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        byte[] ciphertext2 = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        // Ciphertexts should be different due to random nonce
        assert ciphertext1 != ciphertext2
        // But both should decrypt to the same plaintext
        assert plaintext == SecurityIO.decryptWithAES256GCM(secret, ciphertext1)
        assert plaintext == SecurityIO.decryptWithAES256GCM(secret, ciphertext2)
    }

    @Test public void test_SecurityIO_AES256GCM_short_secret() {
        // Test with shortened input bytes (should be padded)
        byte[] secret = 'short'.bytes
        String plaintext = 'some secret message'
        byte[] ciphertext = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        assert plaintext == SecurityIO.decryptWithAES256GCM(secret, ciphertext)
    }

    @Test public void test_SecurityIO_AES256GCM_authentication_failure() {
        byte[] secret = SecurityIO.randomBytes(32)
        String plaintext = 'secret message'
        byte[] ciphertext = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        // Tamper with the ciphertext (modify a byte after the nonce)
        ciphertext[15] = (byte)(ciphertext[15] ^ 0xFF)
        // Decryption should fail due to authentication tag mismatch
        shouldFail(javax.crypto.AEADBadTagException) {
            SecurityIO.decryptWithAES256GCM(secret, ciphertext)
        }
    }

    @Test public void test_SecurityIO_AES256GCM_ciphertext_too_short() {
        byte[] secret = SecurityIO.randomBytes(32)
        // Ciphertext too short (less than nonce + auth tag = 28 bytes minimum)
        byte[] shortCiphertext = new byte[27]
        shouldFail(DecryptException) {
            SecurityIO.decryptWithAES256GCM(secret, shortCiphertext)
        }
    }

    @Test public void test_SecurityIO_AES256GCM_empty_plaintext() {
        byte[] secret = SecurityIO.randomBytes(32)
        String plaintext = ''
        byte[] ciphertext = SecurityIO.encryptWithAES256GCM(secret, plaintext)
        // Ciphertext should be exactly: 12 bytes nonce + 16 bytes auth tag
        assert ciphertext.length == 28
        assert plaintext == SecurityIO.decryptWithAES256GCM(secret, ciphertext)
    }

    @Test public void test_SecurityIO_AES256GCMBase64_encrypt_decrypt() {
        String secretB64 = SecurityIO.randomBytesBase64(32)
        String plaintext = 'secret message for GCM Base64'
        String ciphertextB64 = SecurityIO.encryptWithAES256GCMBase64(secretB64, plaintext)
        assert ciphertextB64.length() > 0
        String decrypted = SecurityIO.decryptWithAES256GCMBase64(secretB64, ciphertextB64)
        assert plaintext == decrypted
    }

    @Test public void test_SecurityIO_AES256GCMBase64_random_nonce() {
        String secretB64 = SecurityIO.randomBytesBase64(32)
        String plaintext = 'same message'
        String ciphertext1 = SecurityIO.encryptWithAES256GCMBase64(secretB64, plaintext)
        String ciphertext2 = SecurityIO.encryptWithAES256GCMBase64(secretB64, plaintext)
        // Ciphertexts should be different due to random nonce
        assert ciphertext1 != ciphertext2
        // But both should decrypt to the same plaintext
        assert plaintext == SecurityIO.decryptWithAES256GCMBase64(secretB64, ciphertext1)
        assert plaintext == SecurityIO.decryptWithAES256GCMBase64(secretB64, ciphertext2)
    }

    // ==========================================================================
    // Tests for new passphrase-based AES-256-GCM encryption methods
    // ==========================================================================

    @Test public void test_SecurityIO_encryptWithPassphraseGCM_decryptWithPassphraseGCM() {
        String passphrase = 'correct horse battery staple'
        String plaintext = 'https://xkcd.com/936/'
        String ciphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        assert ciphertext.length() > 0
        String decrypted = SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext)
        assert plaintext == decrypted
    }

    @Test public void test_SecurityIO_PassphraseGCM_random_salt_and_nonce() {
        // Verify each encryption produces different ciphertext (random salt and nonce)
        String passphrase = 'my secret passphrase'
        String plaintext = 'same message'
        String ciphertext1 = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        String ciphertext2 = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        // Ciphertexts should be different due to random salt and nonce
        assert ciphertext1 != ciphertext2
        // But both should decrypt to the same plaintext
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext1)
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext2)
    }

    @Test public void test_SecurityIO_PassphraseGCM_wrong_passphrase() {
        String passphrase = 'correct passphrase'
        String wrongPassphrase = 'wrong passphrase'
        String plaintext = 'secret data'
        String ciphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        // Decryption with wrong passphrase should fail
        shouldFail(javax.crypto.AEADBadTagException) {
            SecurityIO.decryptWithPassphraseGCM(wrongPassphrase, ciphertext)
        }
    }

    @Test public void test_SecurityIO_PassphraseGCM_ciphertext_too_short() {
        String passphrase = 'some passphrase'
        // Ciphertext too short (less than salt + nonce + auth tag = 60 bytes minimum)
        String shortCiphertext = SecurityIO.encodeBase64(new byte[59])
        shouldFail(DecryptException) {
            SecurityIO.decryptWithPassphraseGCM(passphrase, shortCiphertext)
        }
    }

    @Test public void test_SecurityIO_PassphraseGCM_authentication_failure() {
        String passphrase = 'my passphrase'
        String plaintext = 'secret message'
        String ciphertextB64 = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        byte[] ciphertext = SecurityIO.decodeBase64Bytes(ciphertextB64)
        // Tamper with the ciphertext (modify a byte after salt and nonce)
        ciphertext[50] = (byte)(ciphertext[50] ^ 0xFF)
        String tamperedB64 = SecurityIO.encodeBase64(ciphertext)
        // Decryption should fail due to authentication tag mismatch
        shouldFail(javax.crypto.AEADBadTagException) {
            SecurityIO.decryptWithPassphraseGCM(passphrase, tamperedB64)
        }
    }

    @Test public void test_SecurityIO_PassphraseGCM_different_from_CBC() {
        // Verify GCM and CBC passphrase encryption produce different results
        String passphrase = 'test passphrase'
        String plaintext = 'test message'
        String gcmCiphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        String cbcCiphertext = SecurityIO.encryptWithAES256(passphrase, plaintext)
        // Ciphertexts should be different (different modes and formats)
        assert gcmCiphertext != cbcCiphertext
        // Both should decrypt to the same plaintext with their respective methods
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, gcmCiphertext)
        assert plaintext == SecurityIO.decryptWithAES256(passphrase, cbcCiphertext)
    }

    @Test public void test_SecurityIO_PassphraseGCM_empty_plaintext() {
        String passphrase = 'some passphrase'
        String plaintext = ''
        String ciphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        assert ciphertext.length() > 0
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext)
    }

    @Test public void test_SecurityIO_PassphraseGCM_long_plaintext() {
        String passphrase = 'some passphrase'
        // Create a long plaintext (1000 characters)
        String plaintext = 'A' * 1000
        String ciphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext)
    }

    @Test public void test_SecurityIO_PassphraseGCM_special_characters() {
        String passphrase = 'pässwörd with spëcial çhàracters!'
        String plaintext = 'Message with émojis 🔐 and ünïcödé: 日本語'
        String ciphertext = SecurityIO.encryptWithPassphraseGCM(passphrase, plaintext)
        assert plaintext == SecurityIO.decryptWithPassphraseGCM(passphrase, ciphertext)
    }

    // ==========================================================================
    // Tests for verifyJsonWebToken edge cases (new validation)
    // ==========================================================================

    @Test public void test_SecurityIO_verifyJsonWebToken_invalid_structure_too_few_parts() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // JWT with only 2 parts should fail
        assert false == security.verifyJsonWebToken('header.payload')
    }

    @Test public void test_SecurityIO_verifyJsonWebToken_invalid_structure_too_many_parts() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // JWT with 4 parts should fail
        assert false == security.verifyJsonWebToken('a.b.c.d')
    }

    @Test public void test_SecurityIO_verifyJsonWebToken_wrong_algorithm() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // JWT with HS256 algorithm should fail (we only accept RS256)
        String header = security.encodeBase64Url('{"alg":"HS256","typ":"JWT"}')
        String payload = security.encodeBase64Url('{"sub":"test"}')
        String data = "${header}.${payload}"
        String signature = security.signRS256Base64Url(data)
        assert false == security.verifyJsonWebToken("${data}.${signature}")
    }

    @Test public void test_SecurityIO_verifyJsonWebToken_none_algorithm() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // JWT with "none" algorithm should fail (algorithm confusion attack)
        String header = security.encodeBase64Url('{"alg":"none","typ":"JWT"}')
        String payload = security.encodeBase64Url('{"sub":"test"}')
        assert false == security.verifyJsonWebToken("${header}.${payload}.")
    }

    @Test public void test_SecurityIO_verifyJsonWebToken_malformed_header() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new SecurityIO(url.content.text)
        // JWT with malformed header (not valid JSON) should fail
        String header = security.encodeBase64Url('not valid json')
        String payload = security.encodeBase64Url('{"sub":"test"}')
        String signature = 'fakesignature'
        assert false == security.verifyJsonWebToken("${header}.${payload}.${signature}")
    }
}
