/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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
import net.gleske.jervis.exceptions.KeyPairDecodeException
import net.gleske.jervis.exceptions.SecurityException

import groovy.json.JsonBuilder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
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

<pre><tt>import net.gleske.jervis.tools.SecurityIO

if(!(new File('/tmp/id_rsa').exists())) {
    'openssl genrsa -out /tmp/id_rsa 2048'.execute().waitFor()
    'openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub'.execute().waitFor()
}
def security = new SecurityIO(new File("/tmp/id_rsa").text)
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
class SecurityIO implements Serializable {

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
    def SecurityIO() { }

    /**
      Instantiates the class and configures a private key for decryption.
      Automatically calls <tt>{@link #setKey_pair(java.lang.String)}</tt> as
      part of instantiating.

      @param private_key_pem The contents of an X.509 PEM encoded RSA private key.
      @see #setKey_pair(java.lang.String)
     */
    def SecurityIO(String private_key_pem) {
        setKey_pair(private_key_pem)
    }

    /**
      Creates a URL safe base64 string of a signature using algorithm RS256.
      This data signature is meant for use in <a href="https://jwt.io/">JSON
      Web Tokens</a>.

      @param data Any data meant to be signed with RS256 algorithm.
      @return     Returns a URL safe base64 encoded <tt>String</tt> of the
                  signature.
      */
    String signRS256Base64Url(String data) {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key_pair.private.encoded)
        KeyFactory kf = KeyFactory.getInstance("RSA")
        Signature privateSignature = Signature.getInstance("SHA256withRSA")
        privateSignature.initSign(kf.generatePrivate(spec))
        privateSignature.update(data.getBytes("UTF-8"))
        byte[] signedData = privateSignature.sign()
        encodeBase64Url(signedData)
    }

    /**
      Verify data signed by RS256 Base64 URL encoded signature.

      @param signature A Base64 URL encoded signature of RS256 algorithm.
      @param data      The data in which the signature should verify.
      @return          Returns <tt>true</tt> if the signature was successfully
                       verified or <tt>false</tt> if signature verification
                       failed.
      */
    Boolean verifyRS256Base64Url(String signature, String data) {
        Signature publicSignature = Signature.getInstance("SHA256withRSA")
        publicSignature.initVerify(key_pair.public);
        publicSignature.update(data.bytes)
        publicSignature.verify(decodeBase64UrlBytes(signature))
    }

    /**
      Get a <a href="https://jwt.io/">JSON Web Token</a> (JWT) meant for use with
      <a href="https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps#authenticating-as-a-github-app">GitHub App Authentication</a>.
      This assumes the <tt>SecurityIO</tt> class was loaded with an RSA private
      key provided by GitHub App Authentication setup.

      <h2>Sample usage</h2>

      The following code will generate a JWT, verify it, and extract its
      payload contents.

<pre><tt>import net.gleske.jervis.tools.SecurityIO

if(!(new File('/tmp/id_rsa').exists())) {
    'openssl genrsa -out /tmp/id_rsa 2048'.execute().waitFor()
    'openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub'.execute().waitFor()
}
def security = new SecurityIO(new File("/tmp/id_rsa").text)
// use a fake App ID of 1234
String jwt = security.getGitHubJWT('1234')
if(security.verifyGitHubJWTPayload(jwt)) {
    println('JWT is currently valid.')
} else {
    println('JWT is invalid or expired.')
}

// Issue a 1 minute duration JWT, using clock drift 2 minutes in the past so that it is expired
jwt = security.getGitHubJWT('1234', 1, 120)
if(security.verifyGitHubJWTPayload(jwt)) {
    println('JWT is currently valid.')
} else {
    println('JWT is invalid or expired.')
}</tt></pre>

      @param github_app_id The GitHub App ID available when generating a GitHub App.
      @param expiration    The duration of the JWT in minutes before the JWT
                           expires.  The maximum value supported by GitHub is
                           10 minutes.  Minimum expiration is <tt>1</tt>
                           minute.  Default: <tt>10</tt> minutes.
      @param drift         Number of seconds in the past used as the starting
                           point of the JWT.  This is to account for clock
                           drift between two remote systems as a conservative
                           measure.  If the drift is set for 30 seconds that
                           means the issues JWT will expire in 9 minutes, 30
                           seconds in the future (10 minutes total if you
                           include 30 seconds for clock drift).
                           Default: <tt>30</tt> seconds.
      @return              Returns a signed JWT.  This does not guarantee a
                           valid token just that a token is issued and signed.
                           Use <tt>{@link #verifyGitHubJWTPayload(java.lang.String, java.lang.Integer)}</tt>
                           method to verify the token is valid given the
                           current time.
      */
    String getGitHubJWT(String github_app_id, Integer expiration = 10, Integer drift = 30) {
        if(expiration < 1) {
            expiration = 1
        }
        else if(expiration > 10) {
            expiration = 10
        }
        // 30 seconds in the past for clock drift
        Instant issuedAt = Instant.now().minus(Duration.ofSeconds(drift))
        // 10 minutes is the max limited duration for a GitHub JWT
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(expiration))

        // https://jwt.io/
        String header = '{"alg":"RS256","typ":"JWT"}'
        // https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps#authenticating-as-a-github-app
        String payload = ([
            iat: issuedAt.getEpochSecond(),
            exp: expiresAt.getEpochSecond(),
            iss: github_app_id
        ] as JsonBuilder).toString()
        "${encodeBase64Url(header)}.${encodeBase64Url(payload)}".with { String data ->
            "${data}.${signRS256Base64Url(data)}"
        }
    }

    /**
      Use the loaded public key to verify the provided JSON web token (JWT).

      @param jwt A JSON Web Token meant for authentication.
      @return    Returns <tt>true</tt> if the signature was successfully
                 verified or <tt>false</tt> if signature verification failed.
      */
    Boolean verifyJsonWebToken(String github_jwt) {
        List jwt = github_jwt.tokenize('.')
        String data = jwt[0..1].join('.')
        String signature = jwt[-1]
        verifyRS256Base64Url(signature, data)
    }

    /**
      Verify a GitHub JWT is not expired by checking the signature and
      ensuring current time falls within issued at and expiration.  This does
      both signature checking and decoding the payload to check for validity.
      See also
      <a href="https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps#authenticating-as-a-github-app">GitHub App Authentication</a>.

      @param github_jwt A JWT meant for use in GitHub App Auth.
      @param drift      Add seconds into the future in order to account for
                        clock drift.  If <tt>30</tt> seconds are added that
                        means this will assume a token expires 30 seconds
                        before it really expires.  If you issue a token with a
                        30 second drift and verify the token with a 30 second
                        drift then the maximum duration for a token is
                        <tt>9</tt> minutes.
                        Default: <tt>30</tt> seconds.
      */
    Boolean verifyGitHubJWTPayload(String github_jwt, Integer drift = 30) {
        if(!verifyJsonWebToken(github_jwt)) {
            return false
        }
        Map payload = YamlOperator.loadYamlFrom(decodeBase64UrlBytes(github_jwt.tokenize('.')[1]))
        // add seconds into the future to account for clock drift
        Integer time_since_epoch = Instant.now().getEpochSecond() + drift
        payload.iat < time_since_epoch && payload.exp > time_since_epoch
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
      Decode a URL safe base64 <tt>String</tt> into a <tt>String</tt>.

      @param  content A URL safe base64 encoded <tt>String</tt>.
      @return         A decoded <tt>String</tt>.
     */
    public String decodeBase64UrlString(String content) {
        decodeBase64String(content.tr('-_', '+/'))
    }

    /**
      Decode a URL safe base64 <tt>String</tt> into <tt>{@link java.lang.Byte}s</tt>.

      @param  content URL safe base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    public byte[] decodeBase64UrlBytes(String content) {
        decodeBase64Bytes(content.tr('-_', '+/'))
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
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a URL safe base64 <tt>String</tt>.

      @param  content A plain <tt>String</tt>.
      @return         A URL safe Base64 encoded <tt>String</tt>.
     */
    public String encodeBase64Url(String content) {
        encodeBase64(content).tr('+/', '-_')
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a URL safe base64 <tt>String</tt>.

      @param  content Raw <tt>Bytes</tt>.
      @return         A URL safe Base64 encoded <tt>String</tt>.
     */
    public String encodeBase64Url(byte[] content) {
        encodeBase64(content).tr('+/', '-_')
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

    /**
      This is a constant-time function intended to wrap security-sensitive code.
      This forces code to always take a certain amount of time regardless of
      input in order to have a constant-time result to avoid timing based
      attacks.

      <p>If <tt>milliseconds</tt> is a negative number, e.g. <tt>-200</tt>, then
      the delay will be randomized between 0 and <tt>milliseconds</tt> but not
      more than <tt>milliseconds</tt>.</p>

      <h2>Security Notes</h2>

      <p><b>Please Note:</b> due to how time works on computers sometimes the
      time can be 1 or 2 ms over the time you specify.  If you have an absolute
      maximum, then you should account for this in the value you pass.  This
      function can't guarantee absolute maximum.</p>

      <p><b>Please Note:</b> this function provides fixed maximum delays.  If
      code being called goes over this fixed delay, then timing attacks are
      still possible on your code.  You should account for this with defensive
      programming such as validating inputs on security sensitive code before
      calling algorithms.</p>

      <p><b>Please note:</b> randomized delay can have a limit.  If an attacker
      has infinite time and unlimited samples, then statistically randomness
      goes away.  This function makes it harder to calculate timing attacks;
      other protections should be in place on your code endpoints such as
      request limits within a given time frame.</p>

      <p><b>Please note:</b> These notes are known as defense in depth.  Have
      multiple controls around securing your code in case one control fails.
      Knowing the weakness of different controls enables you to better secure
      it with additional layers of security.</p>

      <h2>Sample Usage</h2>
<pre><tt>import static net.gleske.jervis.tools.SecurityIO.avoidTimingAttack
import java.time.Instant

Integer mysecret = 0
Long before = Instant.now().toEpochMilli()
// Force code to always take 200ms
avoidTimingAttack(200) {
    mysecret = 1+1
}
assert mysecret == 2
println("Time taken (milliseconds): ${Instant.now().toEpochMilli() - before}ms")

before = Instant.now().toEpochMilli()
// Force code to randomly delay up to 200ms
avoidTimingAttack(-200) {
    mysecret = mysecret + 2
}
assert mysecret == 4
println("Time taken (milliseconds): ${Instant.now().toEpochMilli() - before}ms")

before = Instant.now().toEpochMilli()
// Set an implicit value.  Minimum execution time is 100ms random between 100-200ms.
mysecret = avoidTimingAttack(100) {
    avoidTimingAttack(-200) {
        mysecret + mysecret
    }
}
assert mysecret == 8
println("Time taken (milliseconds): ${Instant.now().toEpochMilli() - before}ms")
</tt></pre>

      @param milliseconds The number of milliseconds a section of code must
                          minimally take.  If <tt>milliseconds</tt> is negative,
                          then randomly delay up to the value.  In both cases,
                          the maximum delay is fixed at the absolute value of
                          <tt>milliseconds</tt>.
      @param body A closure of code to execute.  The code will execute as fast
                  as it can and this function will enforce a constant time.
      @return Returns the result from the executed closure.
      */
    public static def avoidTimingAttack(Integer milliseconds, Closure body) {
        Long desiredTime = (milliseconds < 0) ? (new Random().nextInt(milliseconds * -1)) : milliseconds
        Long before = Instant.now().toEpochMilli()
        // execute code
        def result = body()
        // milliseconds - (after - before)
        Long remainingTime = desiredTime - (Instant.now().toEpochMilli() - before)
        if(remainingTime > 0) {
            sleep(remainingTime)
        }
        result
    }
}
