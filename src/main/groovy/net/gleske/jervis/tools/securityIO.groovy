/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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
import net.gleske.jervis.exceptions.SecurityException

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

if(!(new File('/tmp/id_rsa').exists())) {
    'openssl genrsa -out /tmp/id_rsa 2048'.execute().waitFor()
    'openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub'.execute().waitFor()
}
def security = new securityIO(new File("/tmp/id_rsa").text)
println 'Key size: ' + security.id_rsa_keysize.toString()
def s = security.rsaEncrypt('hello friend')
println 'Length of encrypted output: ' + s.length()
println 'Encrypted string:'
println s
println 'Decrypted string:'
println security.rsaDecrypt(s)
new File('/tmp/id_rsa').delete()
new File('/tmp/id_rsa.pub').delete()</tt></pre>
 */
class securityIO implements Serializable {

    private static String deprecation_link = 'http://sam.gleske.net/jervis-api/0.13/net/gleske/jervis/tools/securityIO.html'
    private static String release_notes = 'https://github.com/samrocketman/jervis/blob/master/CHANGELOG.md#jervis-013'

    /**
      Shortcut to getting the key size of <tt>{@link #key_pair}</tt>.

      @see #getId_rsa_keysize()
     */
    public int id_rsa_keysize = 0

    /**
      A decoded RSA key pair used for encryption and decryption.  The key size can be determined from the modulus.  For example,

<pre><tt>println key_pair.private.modulus.bitLength()
println key_pair.public.modulus.bitLength()</tt></pre>

      @see #setKey_pair(java.lang.String)

     */
    public KeyPair key_pair

    /**
      Instantiates an unconfigured instance of this class.  Call
      <tt>{@link #setKey_pair(java.lang.String)}</tt> to properly use this
      class.
     */
    def securityIO() { }

    /**
      Instantiates the class and configures a private key for decryption.
      Automatically calls <tt>{@link #setKey_pair(java.lang.String)}</tt> as
      part of instantiating.

      @param private_key_pem The contents of an X.509 PEM encoded RSA private key.
      @see #setKey_pair(java.lang.String)
     */
    def securityIO(String private_key_pem) {
        setKey_pair(private_key_pem)
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
            if(converter.getKeyPair(obj as PEMKeyPair).private.modulus.bitLength() < 2048) {
                String message = 'Private keys smaller than 2048 are not allowed.'
                message += '  Generate a new key pair 2048 bits or larger.\n\n'
                message += 'Decrypt your old values using:\n\n    '
                message += 'echo \'ciphertext\' | openssl enc -base64 -A -d | openssl rsautl -decrypt -inkey path/to/id_rsa'
                message += '\n\nSee "Enforcing stronger RSA keys" section of the wiki article.'
                throw new KeyPairDecodeException(message)
            }
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
        key_pair.private.modulus.bitLength()
    }

    /**
      A noop which does nothing.  It prevents setting the
      <tt>{@link #id_rsa_keysize}</tt> because the getter is automatically
      calculated from <tt>{@link #key_pair}</tt>.  This method throws a
      <tt>{@link net.gleske.jervis.exceptions.SecurityException}</tt> if it is
      called.

     */
    void setId_rsa_keysize(int i) throws SecurityException {
        throw new SecurityException("setId_rsa_keysize(int) is no longer allowed.  Key size is now automatically calculated when using getId_rsa_keysize()")
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
      Uses RSA asymetric encryption to encrypt a plain text <tt>String</tt> and outputs cipher text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>echo -n 'plaintext' | openssl rsautl -encrypt -inkey ./id_rsa.pub -pubin | openssl enc -base64 -A</tt></pre>

      @param  plaintext A plain text <tt>String</tt> to be encrypted.
      @return A Base64 encoded cipher text or more generically: <tt>ciphertext = base64encode(RSAPublicKeyEncrypt(plaintext))</tt>
     */
    public String rsaEncrypt(String plaintext) throws EncryptException {
        if(!key_pair) {
            throw new EncryptException('key_pair is not set.')
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
        if(!key_pair) {
            throw new DecryptException('key_pair is not set.')
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
