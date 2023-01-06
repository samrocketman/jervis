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

    private Boolean verifyCipherObj(def obj) {
        // Strict type checking
        if(!(obj in Map)) {
            return false
        }
        if(!(['cipher', 'data', 'signature'] == obj.keySet().toList())) {
            return false
        }
        if(!((obj.cipher in List) && obj.cipher.size() == 2)) {
            return false
        }
        Boolean stringCheck = [
            obj.cipher[0],
            obj.cipher[1],
            obj.data,
            obj.signature
        ].every { it in String }
        if(!(stringCheck)) {
            return false
        }
        // Asymmetric decryption check
        obj.cipher.each {
            this.security.rsaDecrypt(it)
        }
        // Data integrity check
        security.verifyRS256Base64Url(obj.signature, obj.data)
    }

    void initialize() {
        // Get the max size an RSA key can encrypt.  Sometimes this is less
        // than 256 bytes which means padding will be required.
        Integer maxSize = [((security.key_pair.private.modulus.bitLength() / 8) - 11), 256].min()
        this.hidden = [
            cipher: [
                security.encodeBase64(security.rsaEncryptBytes(security.randomBytes(maxSize))),
                security.encodeBase64(security.rsaEncryptBytes(security.randomBytes(16)))
            ],
            data: '',
            signature: ''
        ]
    }

    void leftShift(def input) {
        def parsedObj = YamlOperator.loadYamlFrom(input)
        if(!verifyCipherObj(parsedObj)) {
            initialize()
            return
        }
        this.hidden = parsedObj
    }

    void setPlainMap(Map obj) {
        this.hidden.cipher.with { secret ->
            this.hidden.data = security.encryptWithAES256Base64(
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[0]))),
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[1]))),
                YamlOperator.writeObjToYaml(obj)
            )
        }
        this.hidden.signature = security.signRS256Base64Url(this.hidden.data)
    }

    Map getPlainMap() {
        if(!hidden.data) {
            return null
        }
        String plaintext
        this.hidden.cipher.with { secret ->
            plaintext = security.decryptWithAES256Base64(
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[0]))),
                security.encodeBase64(security.rsaDecryptBytes(security.decodeBase64Bytes(secret[1]))),
                this.hidden.data
            )
        }
        YamlOperator.loadYamlFrom(plaintext)
    }

    /**
      Returns an encrypted object as text meant for storing at rest.
<pre><code>
cipher:
  - asymmetrically encrypted AES secret
  - asymmetrically encrypted AES IV
data: AES encrypted data
signature: RS256 Base64URL signature of data
</code></pre>
      */
    String toString() {
        YamlOperator.writeObjToYaml(this.hidden)
    }
}
