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
import org.bouncycastle.crypto.AsymmetricBlockCipher
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

/**
   A class to provide cryptographic features to Jervis such as RSA encryption and base64 encoding.

   <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

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
println security.rsaDecrypt(s)
println "${security.id_rsa_priv} file exists? ${new File(security.id_rsa_priv).exists()}"
println "${security.id_rsa_pub} file exists? ${new File(security.id_rsa_pub).exists()}"
new File(security.id_rsa_priv).delete()
new File(security.id_rsa_pub).delete()
println "${security.id_rsa_priv} file exists? ${new File(security.id_rsa_priv).exists()}"
println "${security.id_rsa_pub} file exists? ${new File(security.id_rsa_pub).exists()}"</tt></pre>
 */
class securityIO implements Serializable {

    private static String deprecation_link = 'http://sam.gleske.net/jervis-api/0.13/net/gleske/jervis/tools/securityIO.html'
    private static String release_notes = 'https://github.com/samrocketman/jervis/blob/master/CHANGELOG.md#jervis-013'

    /**
      Path to the RSA private key.  This will be used by encryptiong and decryption.
      During key generation it is the location where the private key will be written.
      Default: <tt>/tmp/id_rsa.pem</tt>

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    public String id_rsa_priv

    /**
      Path to the RSA public key.  This will be used by encryptiong and decryption.
      During key generation it is the location where the public key will be written.
      Default: <tt>/tmp/id_rsa.pub.pem</tt>

      @Deprecated This is deprecated and will be removed in the next version.
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

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    public static int default_key_size = 2048

    /**
      A decoded RSA key pair used for encryption and decryption.  The key size can be determined from the modulus.  For example,

<pre><tt>println key_pair.private.modulus.bitLength()
println key_pair.public.modulus.bitLength()</tt></pre>

     */
    public KeyPair key_pair

    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.

      @Deprecated Setting <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>,
                  and <tt>{@link #id_rsa_keysize}</tt> is deprecated and will
                  be removed in the next version.
     */
    def securityIO() {
        //Deprecated
        set_vars('/tmp/id_rsa.pem', '/tmp/id_rsa.pub.pem', default_key_size)
    }

    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt> and <tt>{@link #id_rsa_pub}</tt> but
      <tt>{@link #id_rsa_keysize}</tt> is set using <tt>keysize</tt>.

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    def securityIO(int keysize) {
        set_vars('/tmp/id_rsa.pem', '/tmp/id_rsa.pub.pem', keysize)
    }

    /**
      Instantiates the default value for <tt>{@link #id_rsa_keysize}</tt> but sets <tt>{@link #id_rsa_priv}</tt> and <tt>{@link #id_rsa_pub}</tt> based on <tt>path</tt>.
      <tt>path</tt> sets the directory where the public and private key will be generated.
      Specifically values are set in the following manner:
<pre><tt>id_rsa_priv = {@link #checkPath()}(path) + '/id_rsa.pem'
id_rsa_pub = checkPath(path) + '/id_rsa.pub.pem'
id_rsa_keysize = {@link #default_key_size}</tt></pre>

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    def securityIO(String path) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', default_key_size)
    }

    /**
      Instantiates setting custom values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
      <tt>path</tt> sets the directory where the public and private key will be generated.
      <tt>keysize</tt> sets the value for <tt>id_rsa_keysize</tt>.
      Specifically values are set in the following manner:
<pre><tt>id_rsa_priv = {@link #checkPath()}(path) + '/id_rsa.pem'
id_rsa_pub = checkPath(path) + '/id_rsa.pub.pem'
id_rsa_keysize = keysize</tt></pre>

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    def securityIO(String path, int keysize) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', keysize)
    }

    /**
      Instantiates setting custom values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
      Specifically values are set in the following manner:
<pre><tt>id_rsa_priv = priv_key_file_path
id_rsa_pub = pub_key_file_path
id_rsa_keysize = keysize</tt></pre>

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    def securityIO(String priv_key_file_path, String pub_key_file_path, int keysize) {
        set_vars(priv_key_file_path, pub_key_file_path, keysize)
    }

    /**
      Returns the right format for <tt>path</tt>.  This is meant for properly setting the private and public key paths.

      @param  path A full path or relative path on the filesystem.  It must be a directory path.
      @return      A <tt>path</tt> in the right format.
      @see #securityIO(java.lang.String)

      @Deprecated This is deprecated and will be removed in the next version.
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

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    private void set_vars(String priv_key_file_path, String pub_key_file_path, int keysize) {
        id_rsa_priv = priv_key_file_path
        id_rsa_pub = pub_key_file_path
        id_rsa_keysize = keysize
    }

    /**
      Sets <tt>{@link #key_pair}</tt> by decoding the <tt>String</tt>.

      @param pem An X.509 PEM encoded RSA private key.
     */
    void setKey_pair(String pem) {
        PEMParser parser = new PEMParser(new StringReader(pem))
        def obj = parser.readObject()
        if(!obj) {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  readObject returned null.")
        }
        if(!Security.getProvider('BC')) {
            Security.addProvider(new BouncyCastleProvider())
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC")
        if(obj in PEMKeyPair) {
            key_pair = converter.getKeyPair(obj as PEMKeyPair)
        } else {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  Unable to handle ${obj.class}")
        }
    }

    /**
      Gets the <tt>{@link #id_rsa_keysize}</tt> from the decoded private key.

      @returns Key size of the private key if <tt>{@link #key_pair}</tt> is
               set.  Otherwise, returns the value of the variable
               <tt>{@link #id_rsa_keysize}</tt>.
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
        new String(content.trim().decodeBase64())
    }

    /**
      Decode a base64 <tt>String</tt> into <tt>{@link java.lang.Byte}s</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    public byte[] decodeBase64Bytes(String content) {
        content.trim().decodeBase64()
    }

    /**
      Encode a <tt>String</tt> into a base64 <tt>String</tt>.

      @param  content A plain <tt>String</tt>.
      @return         Base64 encoded <tt>String</tt>
     */
    public String encodeBase64(String content) {
        content.bytes.encodeBase64().toString()
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a base64 <tt>String</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    public String encodeBase64(byte[] content) {
        content.encodeBase64().toString()
    }

    /**
      Generate an RSA key pair (private key and public key).
      It does not take values from <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>,
      nor <tt>{@link #id_rsa_keysize}</tt>.

      Warning: This depends on openssl command line utility to be installed.
      Use of this method is not recommended.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>openssl genrsa -out /tmp/id_rsa 2048
openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub</tt></pre>

      @param priv_key_file_path A file path where the private key will be written on the filesystem.
      @param pub_key_file_path  A file path where the public key will be written on the filesystem.
      @param keysize            The key size in bits of the key pair.

      @Deprecated This is deprecated and will be removed in the next version.
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
      Use of this method is not recommended.

      @Deprecated This is deprecated and will be removed in the next version.
     */
    @Deprecated
    public void generate_rsa_pair() {
        generate_rsa_pair(id_rsa_priv, id_rsa_pub, id_rsa_keysize)
    }

    /**
      Uses RSA asymetric encryption to encrypt a plain text <tt>String</tt> and outputs cipher text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>echo -n 'plaintext' | openssl rsautl -encrypt -inkey ./id_rsa.pub -pubin | openssl enc -base64 -A</tt></pre>

      @param  plaintext A plain text <tt>String</tt> to be encrypted.
      @return A Base64 encoded cipher text or more generically: <tt>ciphertext = base64encode(RSAPublicKeyEncrypt(plaintext))</tt>
     */
    public String rsaEncrypt(String plaintext) throws EncryptException {
        if(!key_pair && !(new File(id_rsa_priv).exists())) {
            throw new EncryptException("Private key does not exist so can't instantiate key_pair: ${id_rsa_priv}")
        }
        if(!key_pair) {
            setKey_pair(new File(id_rsa_priv).text)
        }
        AsymmetricBlockCipher encrypt = new PKCS1Encoding(new RSAEngine())
        encrypt.init(true, PublicKeyFactory.createKey(key_pair.public.encoded) as AsymmetricKeyParameter)
        byte[] ciphertext = encrypt.processBlock(plaintext.bytes, 0, plaintext.bytes.length)
        encodeBase64(ciphertext)
    }

    /**
      Uses RSA asymetric encryption to decrypt a cipher text <tt>String</tt> and outputs plain text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>echo 'ciphertext' | openssl enc -base64 -A -d | openssl rsautl -decrypt -inkey /tmp/id_rsa</tt></pre>

      @param  ciphertext A Base64 encoded cipher text <tt>String</tt> to be decrypted.
      @return A plain text <tt>String</tt> or more generically: <tt>plaintext = RSAPrivateKeyDecrypt(base64decode(ciphertext))</tt>
     */
    public String rsaDecrypt(String ciphertext) throws DecryptException {
        if(!key_pair && !(new File(id_rsa_priv).exists())) {
            throw new DecryptException("Private key does not exist so can't instantiate key_pair: ${id_rsa_priv}")
        }
        if(!key_pair) {
            setKey_pair(new File(id_rsa_priv).text)
        }
        AsymmetricBlockCipher decrypt = new PKCS1Encoding(new RSAEngine())
        decrypt.init(false, PrivateKeyFactory.createKey(key_pair.private.encoded) as AsymmetricKeyParameter)
        byte[] messageBytes = decodeBase64Bytes(ciphertext)
        (new String(decrypt.processBlock(messageBytes, 0, messageBytes.length))).trim()
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
