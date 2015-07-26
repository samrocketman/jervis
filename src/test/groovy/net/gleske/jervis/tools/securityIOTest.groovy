/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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
import net.gleske.jervis.exceptions.JervisException
import org.junit.After
import org.junit.Before
import org.junit.Test

class securityIOTest extends GroovyTestCase {
    def security
    //set up before every test
    @Before protected void setUp() {
        security = new securityIO()
    }
    //tear down after every test
    @After protected void tearDown() {
        security = null
    }
    @Test public void test_securityIO_init_default() {
        assert '/tmp/id_rsa.pem' == security.id_rsa_priv
        assert '/tmp/id_rsa.pub.pem' == security.id_rsa_pub
        assert security.default_key_size == security.id_rsa_keysize
    }
    @Test public void test_securityIO_init_key() {
        security = new securityIO(4096)
        assert '/tmp/id_rsa.pem' == security.id_rsa_priv
        assert '/tmp/id_rsa.pub.pem' == security.id_rsa_pub
        assert 4096 == security.id_rsa_keysize
    }
    @Test public void test_securityIO_init_path() {
        security = new securityIO('/path')
        assert '/path/id_rsa.pem' == security.id_rsa_priv
        assert '/path/id_rsa.pub.pem' == security.id_rsa_pub
        assert security.default_key_size == security.id_rsa_keysize
    }
    @Test public void test_securityIO_init_path_key() {
        security = new securityIO('/path',2048)
        assert '/path/id_rsa.pem' == security.id_rsa_priv
        assert '/path/id_rsa.pub.pem' == security.id_rsa_pub
        assert 2048 == security.id_rsa_keysize
    }
    @Test public void test_securityIO_init_priv_pub_key() {
        security = new securityIO('/path/rsa.key','/path/rsa.pub', 192)
        assert '/path/rsa.key' == security.id_rsa_priv
        assert '/path/rsa.pub' == security.id_rsa_pub
        assert 192 == security.id_rsa_keysize
    }
    @Test public void test_securityIO_checkPath() {
        assert 'tmp' == security.checkPath('tmp')
        assert 'tmp' == security.checkPath('tmp/')
        assert '/tmp' == security.checkPath('/tmp')
        assert '/tmp' == security.checkPath('/tmp/')
        assert '' == security.checkPath('/')
        assert '' == security.checkPath('')
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
    @Test public void test_securityIO_generate_rsa_pair() {
        //generate keys based on a random tmp dir
        Path jervis_tmp = Files.createTempDirectory('Jervis_Testing_')
        security = new securityIO(jervis_tmp.toString())
        //test the things
        security.generate_rsa_pair()
        assert true == (new File(jervis_tmp.toString() + '/id_rsa.pem')).exists()
        assert true == (new File(jervis_tmp.toString() + '/id_rsa.pub.pem')).exists()
        shouldFail(JervisException) {
            security.generate_rsa_pair(jervis_tmp.toString(), security.id_rsa_pub, security.id_rsa_keysize)
        }
        shouldFail(JervisException) {
            security.generate_rsa_pair(security.id_rsa_priv, jervis_tmp.toString(), security.id_rsa_keysize)
        }
        //clean up the tmp dir
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc = ['rm','-rf',jervis_tmp.toString()].execute()
        proc.waitForProcessOutput(stdout, stderr)
        if(proc.exitValue()) {
            throw new IOException(stderr.toString())
        }
    }
    @Test public void test_securityIO_rsaEncrypt_rsaDecrypt() {
        //generate keys based on a random tmp dir
        Path jervis_tmp = Files.createTempDirectory('Jervis_Testing_')
        security = new securityIO(jervis_tmp.toString())
        //test the things
        String plaintext = 'secret message'
        String ciphertext
        String decodedtext
        security.generate_rsa_pair()
        assert true == (new File(jervis_tmp.toString() + '/id_rsa.pem')).exists()
        assert true == (new File(jervis_tmp.toString() + '/id_rsa.pub.pem')).exists()
        ciphertext = security.rsaEncrypt(plaintext)
        assert ciphertext.length() > 0
        decodedtext = security.rsaDecrypt(ciphertext)
        assert plaintext == decodedtext
        //clean up the tmp dir
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc = ['rm','-rf',jervis_tmp.toString()].execute()
        proc.waitForProcessOutput(stdout, stderr)
        if(proc.exitValue()) {
            throw new IOException(stderr.toString())
        }
        //we have removed the jervis_tmp directory so these should fail
        shouldFail(JervisException) {
            ciphertext = security.rsaEncrypt('some text')
        }
        shouldFail(JervisException) {
            decodedtext = security.rsaDecrypt('some text')
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
}
