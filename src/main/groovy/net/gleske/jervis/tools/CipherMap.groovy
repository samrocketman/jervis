/*
   Copyright 2014-2026 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.exceptions.JervisException

import java.security.SignatureException
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.crypto.BadPaddingException

/**
  Strong encrypted storage backend used for encrypting a <tt>Map</tt> at rest.
  The must consist only of standard java class types.  Serialization and
  deseralization provided by
  <tt>{@link net.gleske.jervis.tools.YamlOperator}</tt>.

  <h2>Encryption details</h2>

  <p>In general, this class uses algorithms processes recommended by
  <a href="https://csrc.nist.gov/publications/fips" target=_blank>NIST FIPS</a>
  in FIPS 180-4, FIPS 186-4, FIPS 197, and FIPS 198-1.  FIPS 202 was considered
  but not currently used.  The author of this class made a best-effort towards
  understanding these standards and implementing them in code.  However,
  there's no warranty on correctness or 3rd party certification.  As a
  reminder, please reference the license of this project before use.</p>

  <p>Some other high level details include.</p>

  <ul>
  <li>
    Upon instantiation the AES cipher secret is randomly generated (32 bytes).
    A random 12-byte nonce is generated for each encryption operation.
  </li>
  <li>
    The cipher secret is asymmetrically encrypted with RSA using OAEP padding.
    OAEP padding prevents Bleichenbacher padding oracle attacks. The stronger
    the RSA key provided the more secure the encryption at rest.
    Keys below 2048-bits will throw an exception for being too weak.
    Recommended RSA private key size is 4096-bit.
  </li>
  <li>
    The data is encrypted with AES-256-GCM authenticated encryption.
    GCM mode provides both confidentiality and integrity protection,
    preventing padding oracle attacks and detecting tampering.
  </li>
  <li>
    After encryption, the encrypted value is signed with an RS256 signature
    (which is RSA-SHA-256 Base64Url encoded).  This signature is later used
    before decryption.  Decryption will only occur if the signature is valid.
    An empty map is the default if the signature is invalid.
  </li>
  <li>
    The cipher secret infrequently changes to protect against RSA attack
    utilizing Chinese Remainder Theorem.  The cipher secret is automatically
    rotated if the secret is older than 30 days when data is encrypted.
  </li>
  </ul>

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.tools.CipherMap
import java.time.Instant

Map plainTextMap = [hello: 'world']

Long time(Closure c) {
    Instant before = Instant.now()
    c()
    Instant after = Instant.now()
    after.epochSecond - before.epochSecond
}

def cmap1 = new CipherMap(new File('src/test/resources/rsa_keys/good_id_rsa_4096'))

Long timing = time {
    cmap1.plainMap = plainTextMap
}
println("Time encrypting: ${timing} second(s)")

def cmap2 = new CipherMap(new File('src/test/resources/rsa_keys/good_id_rsa_4096'))
timing = time {
    cmap2 << cmap1.toString()
    cmap2.plainMap
}

println("Time to load from String and decrypt: ${timing} second(s)")

println(['\n', '='*80, 'Encrypted contents with CipherMap toString()'.with { ' '*(40 - it.size()/2) + it }, '='*80, "\n${cmap1}"].join('\n'))
</code></pre>
  */
class CipherMap implements Serializable {
    /**
      The object responsible for encryption and decryption.
      */
    private transient SecurityIO security

    /**
      The hidden map containing encrypted data.
      */
    private transient Map hidden

    /**
      Deprecated: This field is no longer used. AES-256-GCM mode uses random
      nonces generated for each encryption operation instead of derived IVs.

      @deprecated No longer used with AES-256-GCM authenticated encryption.
      */
    @Deprecated
    Integer hash_iterations

    /**
      The time limit in seconds before AES secret needs to be rotated.
      Once it reaches this age then a new secret will be generated.  Default:
      <tt>2592000</tt> seconds (number of seconds in 30 days).

      @see #setPlainMap(java.util.Map)
      */
    Long rotate_time_limit = 2592000

    /**
      Instantiates a new CipherMap object with the given private key.  This is
      used for asymmetric encryption wrapping symmetric encryption.

      @param privateKey A PKCS1 or PKCS8 private key PEM.
      */
    CipherMap(String privateKey) {
        this.security = new SecurityIO(privateKey)
    }

    /**
      Instantiates a new CipherMap object with the given private key.  This is
      used for asymmetric encryption wrapping symmetric encryption.

      @deprecated The hash_iterations parameter is no longer used with AES-256-GCM.
      @param privateKey A PKCS1 or PKCS8 private key PEM.
      @param hash_iterations Deprecated, ignored. AES-GCM uses random nonces.
      */
    @Deprecated
    CipherMap(String privateKey, Integer hash_iterations) {
        this.hash_iterations = hash_iterations
        this.security = new SecurityIO(privateKey)
    }

    /**
      Instantiates a new CipherMap object with the given private key.  This is
      used for asymmetric encryption wrapping symmetric encryption.

      @param privateKey A PKCS1 or PKCS8 private key.
      */
    CipherMap(File privateKey) {
        this(privateKey.text)
    }

    /**
      Instantiates a new CipherMap object with the given private key.  This is
      used for asymmetric encryption wrapping symmetric encryption.

      @deprecated The hash_iterations parameter is no longer used with AES-256-GCM.
      @param privateKey A PKCS1 or PKCS8 private key.
      @param hash_iterations Deprecated, ignored. AES-GCM uses random nonces.
      */
    @Deprecated
    CipherMap(File privateKey, Integer hash_iterations) {
        this(privateKey.text, hash_iterations)
    }

    /**
      Encrypts the data with AES-256-GCM authenticated encryption.

      @param data To be encrypted.
      @return Returns encrypted String.
      */
    private String encrypt(String data) {
        String encryptedSecret = this.hidden.cipher
        // Decrypt the RSA-wrapped AES secret using OAEP padding
        byte[] aesSecret = security.rsaDecryptBytesOaep(security.decodeBase64Bytes(encryptedSecret))
        // Encrypt data with AES-256-GCM (nonce is generated automatically and prepended)
        SecurityIO.encryptWithAES256GCMBase64(security.encodeBase64(aesSecret), data)
    }

    /**
      Decrypts the data with AES-256-GCM authenticated decryption.
      @param data To be decrypted.
      @return Returns the plaintext data.
      */
    private String decrypt(String data) {
        String encryptedSecret = this.hidden.cipher
        // Decrypt the RSA-wrapped AES secret using OAEP padding
        byte[] aesSecret = security.rsaDecryptBytesOaep(security.decodeBase64Bytes(encryptedSecret))
        // Decrypt data with AES-256-GCM
        SecurityIO.decryptWithAES256GCMBase64(security.encodeBase64(aesSecret), data)
    }

    /**
      Returns a string meant for signing and verifying signed data.
      */
    private String signedData(Map obj) {
        [obj.age, obj.cipher, obj.data].join('\n')
    }

    private Boolean verifyCipherObj(def obj) {
        // Strict type checking
        if(!(obj in Map)) {
            return false
        }
        if(!(['age', 'cipher', 'data', 'signature'] == obj.keySet().toList())) {
            return false
        }
        // cipher is now a single String (RSA-OAEP encrypted AES secret)
        Boolean stringCheck = [
            obj.age,
            obj.cipher,
            obj.data,
            obj.signature
        ].every { it in String }
        if(!(stringCheck)) {
            return false
        }
        // Data integrity check
        try {
            security.verifyRS256Base64Url(obj.signature, signedData(obj))
        }
        catch(SignatureException ignored) {
            return false
        }
    }

    /**
      Creates a new cipher secret using RSA-OAEP encryption.
      @return Returns a new RSA-OAEP encrypted random AES-256 secret.
      */
    private String newCipher() {
        // Generate a 32-byte (256-bit) random AES secret
        // Use OAEP padding for RSA encryption to prevent Bleichenbacher attacks
        security.encodeBase64(security.rsaEncryptBytesOaep(SecurityIO.randomBytes(32)))
    }

    private void initialize() {
        this.hidden = [
            age: '',
            cipher: '',
            data: '',
            signature: ''
        ]
    }

    /**
      Load or append to this object enciphered text.  There are three behaviors
      depending on the object type passed.

      <ol>
      <li>
        An encrypted <tt>String</tt> created from another <tt>CipherMap</tt>
        will load the secret into this <tt>CipherMap</tt> replacing the current
        <tt>CipherMap</tt>.
      </li>
      <li>
        If another <tt>CipherMap</tt> object is shifted then both this and the other
        object are decrypted.  The two maps are appended together and the
        combined result is encrypted.
      </li>
      <li>
        Throws an exception for any other type.
      </li>
      </ol>

      @see #toString() toString for an example of the encrypted CipherMap String.
      @param input A <tt>String</tt> to load or a <tt>CipherMap</tt> to append.
      */
    void leftShift(def input) throws JervisException {
        if(![String, CipherMap].any { input in it }) {
            throw new JervisException("Cannot leftShift type ${input.getClass()}")
        }
        if(input in CipherMap) {
            setPlainMap(getPlainMap() + input.plainMap)
            return
        }
        def parsedObj = YamlOperator.loadYamlFrom(input)
        if(!verifyCipherObj(parsedObj)) {
            // wipe the data since leftShift should overwrite
            this.hidden = null
            return
        }
        this.hidden = parsedObj
    }

    private void rotateSecrets() {
        if(!this.hidden) {
            initialize()
        }
        Long age
        try {
            age = (this.hidden.age) ? Instant.parse(decrypt(this.hidden.age)).epochSecond : 0
        }
        catch(BadPaddingException|DateTimeParseException ignored) {
            age = 0
        }
        if(age) {
            Long now = Instant.now().epochSecond
            if((now - age) < this.rotate_time_limit) {
                return
            }
        }
        this.hidden.cipher = newCipher()
        this.hidden.age = encrypt(Instant.now().toString())
    }

    /**
      Encrypts the object and stores it for later retrieval as enciphered text.
      Before encryption occurs, the age of the AES-256 secret and IV is checked
      and rotated if beyond a certain age.
      @see #rotate_time_limit
      @see #toString()
      @see net.gleske.jervis.tools.YamlOperator
      @param obj The plain java object to be encrypted.  The object is
                 serialized by YamlOperator and must only consist of standard
                 java classes.
      */
    void setPlainMap(Map obj) {
        rotateSecrets()
        this.hidden.data = encrypt(YamlOperator.writeObjToYaml([secure_field: obj]))
        this.hidden.signature = security.signRS256Base64Url(signedData(this.hidden))
    }

    /**
      Decrypts the encrypted map and returns the object.

      @returns A map consisting of standard java class objects.
      */
    Map getPlainMap() {
        if(!hidden?.data) {
            return [:]
        }
        YamlOperator.loadYamlFrom(decrypt(this.hidden.data)).secure_field
    }

    /**
      Returns an encrypted object as text meant for storing at rest.

<pre><code>
age: AES-GCM encrypted timestamp
cipher: RSA-OAEP encrypted AES-256 secret
data: AES-GCM encrypted data (with nonce prepended)
signature: RS256 Base64URL signature.
</code></pre>

      @see #getPlainMap()
      @see #leftShift(def) leftShift is used to load the ciphertext.
      @return Returns encrypted ciphertext.
      */
    String toString() {
        YamlOperator.writeObjToYaml(this.hidden)
    }
}
