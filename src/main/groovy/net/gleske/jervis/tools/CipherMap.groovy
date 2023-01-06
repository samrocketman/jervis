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
import net.gleske.jervis.tools.SecurityIO
import net.gleske.jervis.tools.YamlOperator
Map m = [hello: 'world']

def co = new CipherMap(new File('jervis-jenkins-as-a-service.2023-01-04.private-key.pem').text)

co.plainMap = m

def c2 = new CipherMap(new File('jervis-jenkins-as-a-service.2023-01-04.private-key.pem').text)

c2 &lt;&lt; co.toString()

c2.plainMap
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

    CipherMap(String private_key_pem) {
        this.security = new SecurityIO(private_key_pem)
        initialize()
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
                data
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
                data
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
        Integer maxSize = [((security.key_pair.private.modulus.bitLength() / 8) - 11), 256].min()
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
        Long age = Instant.parse(decrypt(this.hidden.age)).epochSecond
        Long now = Instant.now().epochSecond
        // Number of seconds in 30 days
        Long limit = 2592000
        if((now - age) < limit) {
            return
        }
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
