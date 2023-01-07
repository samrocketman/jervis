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
package net.gleske.jervis.tools

import net.gleske.jervis.tools.YamlOperator
import java.security.SignatureException

import java.time.Instant

/**

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

// re-encrypt with stronger security
def cmap3 = new CipherMap(new File('src/test/resources/rsa_keys/good_id_rsa_4096').text)
cmap3.hash_iterations = 100100

timing = time {
    cmap3.plainMap = cmap1.plainMap
}
println("Time migrating to stronger encryption with 100100 hash iterations: ${timing} second(s)")
println(['\n', '='*80, 'Encrypted contents with CipherMap toString()'.with { ' '*(40 - it.size()/2) + it }, '='*80, "\n${cmap3}"].join('\n'))
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
      Customize the number of SHA-256 hash iterations performed during AES
      encryption operations.
      */
    Integer hash_iterations = 5000

    CipherMap(String private_key_pem) {
        this.security = new SecurityIO(private_key_pem)
        initialize()
    }

    CipherMap(File privateKey) {
        this(privateKey.text)
    }

    /**
      Encrypts the data with AES.

      @param data To be encrypted.
      @return Returns encrypted String.
      */
    private String encrypt(String data) {
        this.hidden.cipher.with { secret ->
            return security.encryptWithAES256Base64(
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[0]))),
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[1]))),
                data,
                this.hash_iterations
            )
        }
    }

    /**
      Decrypts the data with AES.
      @param data To be decrypted.
      @return Returns the plaintext data.
      */
    private String decrypt(String data) {
        this.hidden.cipher.with { secret ->
            return security.decryptWithAES256Base64(
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[0]))),
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[1]))),
                data,
                this.hash_iterations
            )
        }
    }

    /**
      Returns a string meant for signing and verifying signed data.
      */
    private String signedData(Map obj) {
        ([obj.age] + obj.cipher + [obj.data]).join('\n')
    }

    private Boolean verifyCipherObj(def obj) {
        // Strict type checking
        if(!(obj in Map)) {
            return false
        }
        if(!(['age', 'cipher', 'data', 'signature'] == obj.keySet().toList())) {
            return false
        }
        if(!((obj.cipher in List) && obj.cipher.size() == 2)) {
            return false
        }
        Boolean stringCheck = [
            obj.age,
            obj.cipher[0],
            obj.cipher[1],
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
      Creates a new cipher either for the first time or as part of rotation.
      @return Returns a new random cipher secret and IV.
      */
    private List newCipher() {
        // Get the max size an RSA key can encrypt.  Sometimes this is less
        // than 256 bytes which means padding will be required.
        Integer maxSize = [((security.getRsa_keysize() / 8) - 11), 256].min()
        [
            security.encodeBase64(security.rsaEncryptBytes(security.randomBytes(maxSize))),
            security.encodeBase64(security.rsaEncryptBytes(security.randomBytes(16)))
        ]
    }

    private void initialize() {
        this.hidden = [
            age: Instant.now().toString(),
            cipher: newCipher(),
            data: '',
            signature: ''
        ]
        // Encrypt the cipher age now that the secrets are available.
        this.hidden.age = encrypt(this.hidden.age)
    }

    void leftShift(def input) {
        YamlOperator.catchErrors {
            def parsedObj = YamlOperator.loadYamlFrom(input)
            if(!verifyCipherObj(parsedObj)) {
                initialize()
                return
            }
            this.hidden = parsedObj
        }
    }

    private void rotateSecrets() {
        Long age
        try {
            age = Instant.parse(decrypt(this.hidden.age)).epochSecond
        }
        catch(java.time.format.DateTimeParseException ignored) { }
        Long now = Instant.now().epochSecond
        // Number of seconds in 30 days
        Long limit = 2592000
        if(age && ((now - age) < limit)) {
            return
        }
        this.hidden.age = encrypt(Instant.now().toString())
        this.hidden.cipher = newCipher()
    }

    void setPlainMap(Map obj) {
        rotateSecrets()
        this.hidden.data = encrypt(YamlOperator.writeObjToYaml([secure_field: obj]))
        this.hidden.signature = security.signRS256Base64Url(signedData(this.hidden))
    }

    Map getPlainMap() {
        if(!hidden.data) {
            return [:]
        }
        YamlOperator.loadYamlFrom(decrypt(this.hidden.data)).secure_field
    }

    /**
      Returns an encrypted object as text meant for storing at rest.
<pre><code>
age: AES encrypted timestamp
cipher:
  - asymmetrically encrypted AES secret
  - asymmetrically encrypted AES IV
data: AES encrypted data
signature: RS256 Base64URL signature.
</code></pre>
      */
    String toString() {
        YamlOperator.writeObjToYaml(this.hidden)
    }
}
