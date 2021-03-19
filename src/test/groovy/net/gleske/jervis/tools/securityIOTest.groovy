/*
   Copyright 2014-2021 Sam Gleske - https://github.com/samrocketman/jervis

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
//the securityIOTest() class automatically sees the securityIO() class because they're in the same package
import java.nio.file.Files
import java.nio.file.Path
import net.gleske.jervis.exceptions.DecryptException
import net.gleske.jervis.exceptions.EncryptException
import net.gleske.jervis.exceptions.KeyPairDecodeException
import net.gleske.jervis.exceptions.SecurityException
import org.junit.After
import org.junit.Before
import org.junit.Test

class securityIOTest extends GroovyTestCase {
    def jervis_tmp
    def security
    //set up before every test
    @Before protected void setUp() {
        security = new securityIO()
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
    @Test public void test_securityIO_init_default() {
        security = new securityIO()
        assert !security.key_pair
    }
    @Test public void test_securityIO_init_private_pem() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        security = new securityIO(url.content.text)
        assert security.id_rsa_keysize == 2048
    }
    //test securityIO().decodeBase64()
    @Test public void test_securityIO_decodeBase64String() {
        def s = 'data'
        String encoded = s.bytes.encodeBase64().toString()
        assert 'ZGF0YQ==' == encoded
        assert security.decodeBase64String(encoded) == s
    }
    @Test public void test_securityIO_decodeBase64Bytes() {
        def s = 'data'
        String encoded = s.bytes.encodeBase64().toString()
        assert 'ZGF0YQ==' == encoded
        assert security.decodeBase64Bytes(encoded) == s.bytes
    }
    @Test public void test_securityIO_encodeBase64String() {
        assert 'ZGF0YQ==' == security.encodeBase64('data')
    }
    @Test public void test_securityIO_encodeBase64Bytes() {
        assert 'ZGF0YQ==' == security.encodeBase64('data'.bytes)
    }
    @Test public void test_securityIO_rsaEncrypt_rsaDecrypt() {
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
    @Test public void test_securityIO_fail_rsaEncrypt() {
        shouldFail(EncryptException) {
            def ciphertext = security.rsaEncrypt('some text')
        }
    }
    @Test public void test_securityIO_fail_rsaDecrypt() {
        shouldFail(DecryptException) {
            def decodedtext = security.rsaDecrypt('some text')
        }
    }
    @Test public void test_securityIO_isSecureField_map_nonsecure() {
        Map myobj = new HashMap()
        myobj.put('someprop', 'somevalue')
        assert false == security.isSecureField(myobj)
    }
    @Test public void test_securityIO_isSecureField_nonmap() {
        assert false == security.isSecureField([])
    }
    @Test public void test_securityIO_isSecureField_map_secure() {
        Map myobj = new HashMap()
        myobj.put('secure', 'somevalue')
        assert true == security.isSecureField(myobj)
    }
    @Test public void test_securityIO_load_key_pair() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        assert !security.key_pair
        security.key_pair = url.content.text
        assert security.key_pair
        assert security.id_rsa_keysize == 2048
        url = this.getClass().getResource('/rsa_keys/good_id_rsa_4096')
        security.key_pair = url.content.text
        assert security.id_rsa_keysize == 4096
    }
    @Test public void test_securityIO_bad_key_pair() {
        shouldFail(KeyPairDecodeException) {
            security.key_pair = "bad RSA key"
        }
    }
    @Test public void test_securityIO_unsupported_key_pair() {
        shouldFail(KeyPairDecodeException) {
            URL url = this.getClass().getResource('/rsa_keys/unsupported_crt_pair');
            security.key_pair = url.content.text
        }
    }
    @Test public void test_securityIO_serialization() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
        assert !security.key_pair
        security.key_pair = url.content.text
        assert security.key_pair
        def ciphertext = security.rsaEncrypt('some text')
        def plaintext = security.rsaDecrypt(ciphertext)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(security)
    }
    @Test public void test_securityIO_setId_rsa_keysize() {
        shouldFail(SecurityException) {
            security.id_rsa_keysize = 1024
        }
    }
    @Test public void test_securityIO_fail_on_weak_keys() {
        URL url = this.getClass().getResource('/rsa_keys/bad_id_rsa_1024')
        assert !security.key_pair
        shouldFail(SecurityException) {
            security.key_pair = url.content.text
        }
    }
}
