/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.exceptions.DecryptException
import net.gleske.jervis.exceptions.EncryptException
import net.gleske.jervis.exceptions.KeyGenerationException
import net.gleske.jervis.exceptions.KeyPairDecodeException

import java.security.KeyPair
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

/**
   A class to provide cryptographic features to Jervis such as RSA encryption and base64 encoding.

   <h2>Sample usage</h2>

<pre><tt>import net.gleske.jervis.tools.securityIO
def security = new securityIO('/tmp',2048)
security.generate_rsa_pair()
println 'Private key path: ' + security.id_rsa_priv
println 'Public key path: ' + security.id_rsa_pub
println 'Key size: ' + security.id_rsa_keysize.toString()
def s = security.rsaEncrypt('hello friend')
println 'Length of encrypted output: ' + s.length()
println 'Encrypted string:'
println s
println 'Decrypted string:'
println security.rsaDecrypt(s)</tt></pre>
 */
class securityIO {

    /**
      Path to the RSA private key.  This will be used by encryptiong and decryption.
      During key generation it is the location where the private key will be written.
      Default: <tt>/tmp/id_rsa.pem</tt>
     */
    @Deprecated
    public String id_rsa_priv

    /**
      Path to the RSA public key.  This will be used by encryptiong and decryption.
      During key generation it is the location where the public key will be written.
      Default: <tt>/tmp/id_rsa.pub.pem</tt>
     */
    @Deprecated
    public String id_rsa_pub

    /**
      The key size in which RSA keys will be generated.  It is highly recommended this be <tt>1024</tt> bits or higher in powers of two.
      Default: value of <tt>{@link #default_key_size}</tt>.
     */
    public int id_rsa_keysize

    /**
      The default key size for <tt>{@link #id_rsa_keysize}</tt>.
      Default: <tt>2048</tt>
     */
    @Deprecated
    public static int default_key_size = 2048

    /**
      A decoded private public key pair used for encryption and decryption.  The key size can be determined from the modulus.  For example,

<pre><tt>println key_pair.private.modulus.bitLength()
println key_pair.public.modulus.bitLength()</tt></pre>

     */
    public KeyPair key_pair


    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
     */
    def securityIO() {
        //Deprecated
        set_vars('/tmp/id_rsa.pem', '/tmp/id_rsa.pub.pem', default_key_size)
    }

    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt> and <tt>{@link #id_rsa_pub}</tt> but
      <tt>{@link #id_rsa_keysize}</tt> is set using <tt>keysize</tt>.
     */
    @Deprecated
    def securityIO(int keysize) {
        set_vars('/tmp/id_rsa.pem', '/tmp/id_rsa.pub.pem', keysize)
    }

    /**
      Instantiates the default value for <tt>{@link #id_rsa_keysize}</tt> but sets <tt>{@link #id_rsa_priv}</tt> and <tt>{@link #id_rsa_pub}</tt> based on <tt>path</tt>.
      <tt>path</tt> sets the directory where the public and private key will be generated.
      Specifically values are set in the following manner:
<pre><tt>
id_rsa_priv = {@link #checkPath()}(path) + '/id_rsa.pem'
id_rsa_pub = checkPath(path) + '/id_rsa.pub.pem'
id_rsa_keysize = {@link #default_key_size}</tt></pre>
     */
    def securityIO(String path) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', default_key_size)
    }

    /**
      Instantiates setting custom values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
      <tt>path</tt> sets the directory where the public and private key will be generated.
      <tt>keysize</tt> sets the value for <tt>id_rsa_keysize</tt>.
      Specifically values are set in the following manner:
<pre><tt>
id_rsa_priv = {@link #checkPath()}(path) + '/id_rsa.pem'
id_rsa_pub = checkPath(path) + '/id_rsa.pub.pem'
id_rsa_keysize = keysize</tt></pre>
     */
    @Deprecated
    def securityIO(String path, int keysize) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', keysize)
    }

    /**
      Instantiates setting custom values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
      Specifically values are set in the following manner:
<pre><tt>
id_rsa_priv = priv_key_file_path
id_rsa_pub = pub_key_file_path
id_rsa_keysize = keysize</tt></pre>
    @Deprecated
     */
    def securityIO(String priv_key_file_path, String pub_key_file_path, int keysize) {
        set_vars(priv_key_file_path, pub_key_file_path, keysize)
    }

    /**
      Returns the right format for <tt>path</tt>.  This is meant for properly setting the private and public key paths.

      @param  path A full path or relative path on the filesystem.  It must be a directory path.
      @return      A <tt>path</tt> in the right format.
      @see #securityIO(java.lang.String)
     */
    @Deprecated
    public String checkPath(String path) {
        if(path.length() > 0 && path[-1] == '/') {
            if(path == '/') {
                path = ''
            }
            else {
                path = path[0..-2]
            }
        }
        return path
    }

    /**
      A generic setter function that allows contstructor overloading with minimal code duplication.

      @param priv_key_file_path A file path where the private key will be written on the filesystem.
      @param pub_key_file_path  A file path where the public key will be written on the filesystem.
      @param keysize            The key size in bits of the key pair.
     */
    @Deprecated
    private void set_vars(String priv_key_file_path, String pub_key_file_path, int keysize) {
        id_rsa_priv = priv_key_file_path
        id_rsa_pub = pub_key_file_path
        id_rsa_keysize = keysize
    }

    /**
      Sets <tt>{@link #key_pair}</tt> by decoding the <tt>String</tt>.

      @param pem An X.509 PEM encoded private key.
     */
    void setKey_pair(String pem) {
        PEMParser parser = new PEMParser(new StringReader(pem))
        def obj = parser.readObject()
        if(!obj) {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  readObject returned null.")
        }
        Security.addProvider(new BouncyCastleProvider())
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC")
        if(obj in PEMKeyPair) {
            key_pair = converter.getKeyPair(obj as PEMKeyPair)
        } else {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  Unable to handle ${obj.class}")
        }
    }

    /**
      Gets they <tt>{@link #id_rsa_keysize}</tt> from the decoded private key.
     */
    int getId_rsa_keysize() {
        if(key_pair) {
            key_pair.private.modulus.bitLength()
        }
        else {
            id_rsa_keysize
        }
    }

    /**
      Decode a base64 <tt>String</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         A decoded <tt>String</tt>.
     */
    public String decodeBase64String(String content) {
        return new String(content.decodeBase64())
    }

    /**
      Decode a base64 <tt>String</tt> into <tt>{@link java.lang.Byte}s</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    public byte[] decodeBase64Bytes(String content) {
        return content.decodeBase64()
    }

    /**
      Encode a <tt>String</tt> into a base64 <tt>String</tt>.

      @param  content A plain <tt>String</tt>.
      @return         Base64 encoded <tt>String</tt>
     */
    public String encodeBase64(String content) {
        return content.bytes.encodeBase64().toString()
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a base64 <tt>String</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    public String encodeBase64(byte[] content) {
        return content.encodeBase64().toString()
    }

    /**
      Generate an RSA key pair (private key and public key).
      It does not take values from <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>,
      nor <tt>{@link #id_rsa_keysize}</tt>.

      Warning: This depends on openssl command line utility to be installed.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>openssl genrsa -out /tmp/id_rsa 2048
openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub</tt></pre>

      @param priv_key_file_path A file path where the private key will be written on the filesystem.
      @param pub_key_file_path  A file path where the public key will be written on the filesystem.
      @param keysize            The key size in bits of the key pair.
     */
    @Deprecated
    public void generate_rsa_pair(String priv_key_file_path, String pub_key_file_path, int keysize) throws KeyGenerationException {
        StringBuilder stderr = new StringBuilder()
        Process process = ['openssl', 'genrsa', '-out', priv_key_file_path, keysize.toString()].execute()
        process.waitFor()
        process.waitForProcessOutput(null, stderr)
        if(process.exitValue()) {
            throw new KeyGenerationException(stderr.toString())
        }
        process = ['openssl', 'rsa', '-in', priv_key_file_path, '-pubout', '-outform', 'pem', '-out', pub_key_file_path].execute()
        process.waitFor()
        process.waitForProcessOutput(null, stderr)
        if(process.exitValue()) {
            throw new KeyGenerationException(stderr.toString())
        }
    }

    /**
      Generate an RSA key pair (private key and public key) using <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.

      Warning: This depends on openssl command line utility to be installed.
     */
    @Deprecated
    public void generate_rsa_pair() {
        generate_rsa_pair(id_rsa_priv, id_rsa_pub, id_rsa_keysize)
    }

    /**
      Uses RSA asymetric encryption to encrypt a plain text <tt>String</tt> and outputs cipher text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>echo -n 'plaintext' | openssl rsautl -encrypt -inkey /tmp/id_rsa.pub -pubin | base64 -w0</tt></pre>

      @param  plaintext A plain text <tt>String</tt> to be encrypted.
      @return A Base64 encoded cipher text or more generically: <tt>ciphertext = base64encode(RSAPublicKeyEncrypt(plaintext))</tt>
     */
    public String rsaEncrypt(String plaintext) throws EncryptException {
        //build a list of processes to pipe
        def stdout = new StringBuffer()
        def stderr = new StringBuffer()
        Process proc1 = ['echo', plaintext.trim()].execute()
        Process proc2 = ['openssl', 'rsautl', '-encrypt', '-inkey', id_rsa_pub, '-pubin'].execute()
        Process proc3 = ['openssl','enc', '-base64', '-A'].execute()
        proc1 | proc2 | proc3
        proc2.waitFor()
        proc3.waitFor()
        proc2.waitForProcessOutput(null, stderr)
        proc3.waitForProcessOutput(stdout, null)
        if(proc2.exitValue()) {
            throw new EncryptException(stderr.toString())
        }
        else {
            return stdout.toString().trim()
        }
    }

    /**
      Uses RSA asymetric encryption to decrypt a cipher text <tt>String</tt> and outputs plain text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>echo 'ciphertext' | base64 -d | openssl rsautl -decrypt -inkey /tmp/id_rsa</tt></pre>

      @param  ciphertext A Base64 encoded cipher text <tt>String</tt> to be decrypted.
      @return A plain text <tt>String</tt> or more generically: <tt>plaintext = RSAPrivateKeyDecrypt(base64decode(ciphertext))</tt>
     */
    public String rsaDecrypt(String ciphertext) throws DecryptException {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        Process proc1 = ['echo', '-n', ciphertext.trim()].execute()
        Process proc2 = ['openssl', 'enc', '-base64', '-A', '-d'].execute()
        Process proc3 = ['openssl', 'rsautl', '-decrypt', '-inkey', id_rsa_priv].execute()
        proc1 | proc2 | proc3
        proc3.waitFor()
        proc3.waitForProcessOutput(stdout, stderr)
        if(proc3.exitValue()) {
            throw new DecryptException(stderr.toString())
        }
        else {
            return stdout.toString().trim()
        }
    }

    /**
      Checks to see if a field in the Jervis YAML is a
      <a href="https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories" target="_blank">secure field</a>.
      If it is then decryption should be attempted.  This only detects of decryption
      is plausible.

      @param  property A simple object that can take multiple types to check against.
      @return Returns <tt>true</tt> if the field can potentially be decrypted.
     */
    public Boolean isSecureField(def field) {
        if(field instanceof Map) {
            String[] field_keys = field.keySet() as String[]
            if('secure' in field_keys) {
                return true
            }
        }
        return false
    }
}
