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

import net.gleske.jervis.exceptions.DecryptException
import net.gleske.jervis.exceptions.EncryptException
import net.gleske.jervis.exceptions.KeyPairDecodeException

import groovy.json.JsonBuilder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
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
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemObjectGenerator

/**
  A class to provide cryptographic features to Jervis such as RSA encryption and base64 encoding.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.tools.SecurityIO

if(!(new File('/tmp/id_rsa').exists())) {
    'openssl genrsa -out /tmp/id_rsa 4096'.execute().waitFor()
    'openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub'.execute().waitFor()
}
def security = new SecurityIO(new File("/tmp/id_rsa").text)
println "Key size: ${security.rsa_keysize}"
def s = security.rsaEncrypt('hello friend')
println 'Length of encrypted output: ' + s.length()
println 'Encrypted string:'
println s
println 'Decrypted string:'
println security.rsaDecrypt(s)
new File('/tmp/id_rsa').delete()
new File('/tmp/id_rsa.pub').delete()
</code></pre>
 */
class SecurityIO implements Serializable {

    /**
      The default number of iterations of SHA-256 hashing of the AES IV when AES
      encryption or decryption is performed.  Default: <tt>5000</tt> iterations.

      @see #decryptWithAES256(byte[], byte[], byte[], java.lang.Integer)
      @see #encryptWithAES256(byte[], byte[], java.lang.String, java.lang.Integer)
      */
    static Integer DEFAULT_AES_ITERATIONS = 5000

    /**
      A decoded RSA key pair used for encryption and decryption, and signing.

      @see #setKey_pair(java.lang.String)
      @see #getRsa_keysize()

     */
    transient KeyPair key_pair

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
        publicSignature.initVerify(key_pair.public)
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

<pre><code>
import net.gleske.jervis.tools.SecurityIO

if(!(new File('/tmp/id_rsa').exists())) {
    'openssl genrsa -out /tmp/id_rsa 4096'.execute().waitFor()
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
}
</code></pre>

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
    void setKey_pair(String pem) throws KeyPairDecodeException {
        PEMParser parser = new PEMParser(new StringReader(pem))
        def obj = parser.readObject()
        parser.close()
        if(!obj) {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  readObject returned null.")
        }
        if(obj in PrivateKeyInfo) {
            obj = getKeypairFromPkcs8(obj)
        }
        if(obj in PEMKeyPair) {
            setPemKeyPair(obj)
        } else {
            throw new KeyPairDecodeException("Could not decode KeyPair from pem String.  Unable to handle ${obj.class}")
        }
    }

    /**
      Converts from PKCS8 private to PKCS1 pair.
      */
    private PEMKeyPair getKeypairFromPkcs8(PrivateKeyInfo pkInfo) {
        ASN1Encodable pkcs1ASN1Encodable = pkInfo.parsePrivateKey()
        ASN1Primitive privateKeyPkcs1ASN1 = pkcs1ASN1Encodable.toASN1Primitive()
        StringWriter stringWriter = new StringWriter()
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter)
        jcaPEMWriter.writeObject((PemObjectGenerator)new PemObject("RSA PRIVATE KEY", privateKeyPkcs1ASN1.getEncoded()))
        jcaPEMWriter.close()
        String pkcs1pem = stringWriter.toString() // -----BEGIN RSA PRIVATE KEY-----...
        PEMParser parser = new PEMParser(new StringReader(pkcs1pem))
        def obj = parser.readObject()
        parser.close()
        obj
    }

    /**
      Creates a <tt>{@link java.security.KeyPair}</tt> from  <tt>PEMKeyPair</tt>
      and checks minimum key strength.  Sets <tt>{@link #key_pair}</tt> which is
      used for encryption, decryption, and signing.
      */
    private void setPemKeyPair(PEMKeyPair obj) {
        if(!Security.getProvider('BC')) {
            Security.addProvider(new BouncyCastleProvider())
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC")
        KeyPair parsedKeyPair = converter.getKeyPair(obj)
        if(parsedKeyPair.private.modulus.bitLength() < 2048) {
            String message = 'Private keys smaller than 2048 are not allowed.'
            message += '  Generate a new key pair 2048 bits or larger.\n\n'
            message += 'Decrypt your old values using:\n\n    '
            message += 'echo \'ciphertext\' | openssl enc -base64 -A -d | openssl rsautl -decrypt -inkey path/to/id_rsa'
            message += '\n\nSee "Enforcing stronger RSA keys" section of the wiki article.'
            throw new KeyPairDecodeException(message)
        }
        key_pair = parsedKeyPair
    }

    /**
      Returns the calculated RSA private key size in bits calculated from the
      <tt>{@link #key_pair}</tt>.  e.g. a 4096-bit RSA key will return <tt>4096</tt>.

      <h2>Sample usage</h2>
<pre><code>
import net.gleske.jervis.tools.SecurityIO
def security = new SecurityIO(new File("/tmp/id_rsa").text)
println "Key size: ${security.rsa_keysize}"
</code></pre>

      @returns Key size of the private key if <tt>{@link #key_pair}</tt> is
               set.  If no private key, then returns <tt>0</tt>.
     */
    Integer getRsa_keysize() {
        key_pair?.private?.modulus?.bitLength() ?: 0
    }

    /**
      Decode a base64 <tt>String</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         A decoded <tt>String</tt>.
     */
    static String decodeBase64String(String content) {
        new String(content.trim().decodeBase64())
    }

    /**
      Decode a base64 <tt>String</tt> into <tt>{@link java.lang.Byte}s</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    static byte[] decodeBase64Bytes(String content) {
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
    static byte[] decodeBase64UrlBytes(String content) {
        decodeBase64Bytes(content.tr('-_', '+/'))
    }

    /**
      Encode a <tt>String</tt> into a base64 <tt>String</tt>.

      @param  content A plain <tt>String</tt>.
      @return         Base64 encoded <tt>String</tt>
     */
    static String encodeBase64(String content) {
        content.bytes.encodeBase64().toString()
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a base64 <tt>String</tt>.

      @param  content Base64 encoded <tt>String</tt>.
      @return         Decoded raw <tt>Bytes</tt>.
     */
    static String encodeBase64(byte[] content) {
        content.encodeBase64().toString()
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a URL safe base64 <tt>String</tt>.

      @param  content A plain <tt>String</tt>.
      @return         A URL safe Base64 encoded <tt>String</tt>.
     */
    static String encodeBase64Url(String content) {
        encodeBase64(content).tr('+/', '-_')
    }

    /**
      Encode raw <tt>{@link java.lang.Byte}s</tt> into a URL safe base64 <tt>String</tt>.

      @param  content Raw <tt>Bytes</tt>.
      @return         A URL safe Base64 encoded <tt>String</tt>.
     */
    static String encodeBase64Url(byte[] content) {
        encodeBase64(content).tr('+/', '-_')
    }

    /**
      Uses RSA asymmetric encryption to encrypt a plain text <tt>String</tt> and outputs ciphertext.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><code>
echo -n 'plaintext' | openssl rsautl -encrypt -inkey ./id_rsa.pub -pubin | openssl enc -base64 -A
</code></pre>

      @param  plaintext A plain text <tt>String</tt> to be encrypted.
      @return A Base64 encoded ciphertext or more generically: <tt>ciphertext = base64encode(RSAPublicKeyEncrypt(plaintext))</tt>
     */
    String rsaEncrypt(String plaintext) throws EncryptException {
        byte[] ciphertext = rsaEncryptBytes(plaintext.bytes)
        encodeBase64(ciphertext)
    }

    /**
      Uses RSA asymmetric encryption to encrypt a plain bytes and outputs enciphered bytes.
      @param plainbytes Plain bytes to be encrypted.
      @return Enciphered bytes are returned.
      */
    byte[] rsaEncryptBytes(byte[] plainbytes) throws EncryptException {
        if(!key_pair) {
            throw new EncryptException('key_pair is not set.')
        }
        AsymmetricBlockCipher encrypt = new PKCS1Encoding(new RSAEngine())
        encrypt.init(true, PublicKeyFactory.createKey(key_pair.public.encoded) as AsymmetricKeyParameter)
        byte[] enciphered = encrypt.processBlock(plainbytes, 0, plainbytes.length)
        enciphered
    }

    /**
      Uses RSA asymmetric encryption to decrypt a ciphertext <tt>String</tt> and outputs plain text.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><code>
echo 'ciphertext' | openssl enc -base64 -A -d | openssl rsautl -decrypt -inkey /tmp/id_rsa
</code></pre>

      @param  ciphertext A Base64 encoded ciphertext <tt>String</tt> to be decrypted.
      @return A plain text <tt>String</tt> or more generically: <tt>plaintext = RSAPrivateKeyDecrypt(base64decode(ciphertext))</tt>
     */
    String rsaDecrypt(String ciphertext) throws DecryptException {
        byte[] messageBytes = decodeBase64Bytes(ciphertext)
        (new String(rsaDecryptBytes(messageBytes)))
    }

    /**
      Uses RSA asymmetric encryption to decrypt enciphered bytes and returns plain bytes.
      @param cipherbytes Encrypted bytes.
      @return Returns decrypted bytes.
      */
    byte[] rsaDecryptBytes(byte[] cipherbytes) throws DecryptException {
        if(!key_pair) {
            throw new DecryptException('key_pair is not set.')
        }
        AsymmetricBlockCipher decrypt = new PKCS1Encoding(new RSAEngine())
        decrypt.init(false, PrivateKeyFactory.createKey(key_pair.private.encoded) as AsymmetricKeyParameter)
        decrypt.processBlock(cipherbytes, 0, cipherbytes.length)
    }

    /**
      Checks to see if a field in the Jervis YAML is a
      <a href="https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories" target="_blank">secure field</a>.
      If it is then decryption should be attempted.  This only detects of decryption
      is plausible.

      @param  property A simple object that can take multiple types to check against.
      @return Returns <tt>true</tt> if the field can potentially be decrypted.
     */
    static Boolean isSecureField(def field) {
        (field instanceof Map) && ('secure' in field.keySet())
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

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import static net.gleske.jervis.tools.SecurityIO.avoidTimingAttack
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
</code></pre>

      @param milliseconds The number of milliseconds a section of code must
                          minimally take.  If <tt>milliseconds</tt> is negative,
                          then randomly delay up to the value.  In both cases,
                          the maximum delay is fixed at the absolute value of
                          <tt>milliseconds</tt>.
      @param body A closure of code to execute.  The code will execute as fast
                  as it can and this function will enforce a constant time.
      @return Returns the result from the executed closure.
      */
    static def avoidTimingAttack(Integer milliseconds, Closure body) {
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

    /**
      Calculates SHA-256 sum from a String.

      @param input A <tt>String</tt> to calculate a SHA-256 digest.
      @return A SHA-256 hex <tt>String</tt>.
      */
    static String sha256Sum(String input) {
        sha256Sum(input.bytes)
    }

    /**
      Calculates SHA-256 sum from a byte-array.

      @param input A <tt>String</tt> to calculate a SHA-256 digest.
      @return A SHA-256 hex <tt>String</tt>.
      */
    static String sha256Sum(byte[] input) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        digest.update(input)
        new BigInteger(1,digest.digest()).toString(16).padLeft(32, '0')
    }

    /**
      Returns a random of bytes.
      @param size Number of random bytes to generate.
      @return Random bytes provided by <tt>NativePRNGNonBlocking</tt>.
      */
    static byte[] randomBytes(int size) {
        SecureRandom rand = SecureRandom.getInstance('NativePRNGNonBlocking')
        byte[] random = new byte[size]
        rand.nextBytes(random)
        random
    }

    /**
      Base64 encoded random bytes.
      @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html" target=_blank>Java Security Standard Algorithm Names</a>.
      @param size Number of random bytes to generate.
      @return Random bytes wrapped with Base64 encoding.
      */
    static String randomBytesBase64(int size) {
        encodeBase64(randomBytes(size))
    }

    /**
      Encrypt plaintext using AES-256 CBC mode with PKCS5 padding.  The IV is
      hashed with multiple iterations of SHA-256.

      @see #DEFAULT_AES_ITERATIONS
      @see <a href="https://docs.oracle.com/en/java/javase/11/security/java-cryptography-architecture-jca-reference-guide.html#GUID-94225C88-F2F1-44D1-A781-1DD9D5094566" target=_blank>Java Cryptography Architecture (JCA) Reference Guide for <tt>Cipher</tt> class</a>
      @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html" target=_blank>Java Security Standard Algorithm Names</a>.
      @param secret Secret key for decrypting.  If byte-count is less than 32
                    (256-bits), then bytes are repeated until 256 bits are available.
      @param iv Initialization vector (IV) used to initialize the cipher.
      @param data Data to be encrypted with AES-256.
      @param hash_iterations The IV is hashed with SHA-256.  For each iteration
                             the <tt>iv</tt> is combined with the previous
                             iteration result of the hashing.  The resulting
                             bytes are fed into <tt>PBKDF2WithHmacSHA256</tt>
                             resulting in a new initialization vector.  If set
                             to <tt>0</tt>, then hashing is skipped and the
                             original <tt>iv</tt> is used for AES cipher
                             initialization.
      */
    static byte[] encryptWithAES256(byte[] secret, byte[] iv, String data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        // Calculate IV with 5k iterations of SHA-256 sum
        String checksum
        if(hash_iterations > 0) {
            Integer iterations = hash_iterations - 1
            checksum = sha256Sum(iv)
            iterations.times {
                checksum = sha256Sum([iv, checksum.bytes].flatten() as byte[])
            }
        }
        byte[] b_iv = (checksum) ? passwordKeyDerivation(checksum, checksum)[0..15] : iv
        // 32 comes from 256 / 8 in AES-256
        SecretKey key = new SecretKeySpec(padForAES256(secret), 0, 32, 'AES')
        Cipher cipher = Cipher.getInstance('AES/CBC/PKCS5Padding')
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(b_iv))
        cipher.doFinal(data.getBytes('UTF-8'))
    }

    /**
      Decrypt ciphertext using AES-256 CBC mode with PKCS5 padding.  The IV is
      hashed with multiple iterations of SHA-256.

      @see #DEFAULT_AES_ITERATIONS
      @see <a href="https://docs.oracle.com/en/java/javase/11/security/java-cryptography-architecture-jca-reference-guide.html#GUID-94225C88-F2F1-44D1-A781-1DD9D5094566" target=_blank>Java Cryptography Architecture (JCA) Reference Guide for <tt>Cipher</tt> class</a>
      @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html" target=_blank>Java Security Standard Algorithm Names</a>.
      @param secret Secret key for decrypting.  If byte-count is less than 32
                    (256-bits), then bytes are repeated until 256 bits are available.
      @param iv Initialization vector (IV) used to initialize the cipher.
      @param data Data to be encrypted with AES-256.
      @param hash_iterations The IV is hashed with SHA-256.  For each iteration
                             the <tt>iv</tt> is combined with the previous
                             iteration result of the hashing.  The resulting
                             bytes are fed into <tt>PBKDF2WithHmacSHA256</tt>
                             resulting in a new initialization vector.  If set
                             to <tt>0</tt>, then hashing is skipped and the
                             original <tt>iv</tt> is used for AES cipher
                             initialization.
      */
    static String decryptWithAES256(byte[] secret, byte[] iv, byte[] data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        // Calculate IV with 5k iterations of SHA-256 sum
        String checksum
        if(hash_iterations > 0) {
            Integer iterations = hash_iterations - 1
            checksum = sha256Sum(iv)
            iterations.times {
                checksum = sha256Sum([iv, checksum.bytes].flatten() as byte[])
            }
        }
        byte[] b_iv = (checksum) ? passwordKeyDerivation(checksum, checksum)[0..15] : iv
        // 32 comes from 256 / 8 in AES-256
        SecretKey key = new SecretKeySpec(padForAES256(secret), 0, 32, 'AES')
        Cipher cipher = Cipher.getInstance('AES/CBC/PKCS5Padding')
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(b_iv))
        new String(cipher.doFinal(data), 'UTF-8')
    }

    /**
      Takes Base64 encoded secret and IV and encrypts the provided
      <tt>String</tt> data.

      @see #encryptWithAES256(byte[], byte[], java.lang.String, java.lang.Integer)
      @param secret Base64 encoded binary secret.
      @param iv Base64 encoded binary initialization vector (IV).
      @param data A plain <tt>String</tt> to be encrypted (not Base64 encoded).
      */
    static String encryptWithAES256Base64(String secret, String iv, String data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        byte[] b_secret = decodeBase64Bytes(secret)
        byte[] b_iv = decodeBase64Bytes(iv)
        encodeBase64(encryptWithAES256(b_secret, b_iv, data, hash_iterations))
    }

    /**
      Takes Base64 encoded secret and IV and decrypts the provided Base64
      encoded <tt>String</tt> data.

      @see #encryptWithAES256(byte[], byte[], java.lang.String, java.lang.Integer)
      @param secret Base64 encoded binary secret.
      @param iv Base64 encoded binary initialization vector (IV).
      @param data Base64 encoded encrypted bytes to decrypt.
      @return A <tt>String</tt> which is plaintext.
      */
    static String decryptWithAES256Base64(String secret, String iv, String data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        byte[] b_secret = decodeBase64Bytes(secret)
        byte[] b_iv = decodeBase64Bytes(iv)
        byte[] b_data = decodeBase64Bytes(data)
        decryptWithAES256(b_secret, b_iv, b_data, hash_iterations)
    }

    /**
      Repeats the input bytes until enough bytes are provided for AES-256.

      @param input Bytes to be repeated.
      */
    static byte[] padForAES256(byte[] input) {
        if(input.size() >= 32) {
            return input
        }
        Integer n = (32 / (input.size() + 1)) + 1
        List b_list = [input]
        n.times {
            b_list << input
        }
        b_list.flatten()[0..31]
    }

    /**
      Converts to an integer of iterations based on SHA-256 sum input.  The same
      hash will always result in the same number of iterations.
      @param shasum A SHA hash which will return an integer.
      @return Returns an integer with a value between 100100 and 960000 for
              SHA-256.  Larger hashes will return a larger maximum.
      */
    private static Integer iterationDerivation(String shasum) {
        // An all f SHA-256 sum (64 chars) is 960.  This maxMultiplier sets the maximum
        // value of the number of iterations.  e.g. 1000 means the max will be
        // 960000
        Integer maxMultiplier = 1000
        Integer minIterations = 100100
        String keyIndex = "0123456789abcdef"
        Map subtract = [:]
        Integer iterations =  shasum.toList().each { String c ->
            Set keyset = subtract.keySet()
            if(keyset.size() == 4 || c in keyset) {
                return
            }
            Integer negative = 1 + (keyset.size() * 10 / 2)
            subtract[c] = -1 * negative
        }.collect {
            (it in subtract.keySet()) ? subtract[it] : (keyIndex.indexOf(it) * maxMultiplier)
        }.sum().abs()
        [minIterations, iterations].max()
    }

    /**
      Takes a passphrase and a SHA-256 checksum and returns a PBKDF2 with HMAC
      SHA-256 secret key intended for AES encryption and decryption.

      @param passphrase A passphrase which would typically come from human input.
      @param shasum A SHA-256 a derived from the passphrase.
      */
    private static byte[] passwordKeyDerivation(String passphrase, String shasum) {
        Integer iterations = iterationDerivation(shasum)
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), shasum.getBytes(), iterations, 256)
        factory.generateSecret(spec).getEncoded()
    }

    /**
      Encrypts String data with AES-256 using a passphrase.

      <h2>Sample usage</h2>
<pre><code>
import net.gleske.jervis.tools.SecurityIO
import java.time.Instant

Long time(Closure c) {
    Long before = Instant.now().epochSecond
    c()
    Long after = Instant.now().epochSecond
    after - before
}

String encrypted
Long timing1 = time {
    encrypted = SecurityIO.encryptWithAES256('correct horse battery staple', 'My secret data')
}


println("Encrypted text: ${encrypted}")
Long timing2 = time {
    println("Decrypted text: ${SecurityIO.decryptWithAES256('correct horse battery staple', encrypted)}")
}

println "Encrypt time: ${timing1} second(s)\nDecrypt time: ${timing2} second(s)"
</code></pre>

      @see #DEFAULT_AES_ITERATIONS
      @see #encryptWithAES256(byte[], byte[], java.lang.String, java.lang.Integer) encryptWithAES256 AES enciphering details
      @see <a href="https://docs.oracle.com/en/java/javase/11/security/java-cryptography-architecture-jca-reference-guide.html#GUID-5E8F4099-779F-4484-9A95-F1CEA167601A" target=_blank>Java Cryptography Architecture (JCA) Reference Guide for <tt>SecretKeyFactory</tt> class</a>
      @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#secretkeyfactory-algorithms" target=_blank><tt>SecretKeyFactory</tt> algorithm names</a>
      @see <a href="https://www.nist.gov/publications/recommendation-password-based-key-derivation-part-1-storage-applications" target=_blank>Recommendation for Password-Based Key Derivation Part 1: Storage Applications</a>;
           Published: December 22, 2010; Citation: Special Publication (NIST SP) - 800-132


      @param passphrase A passphrase used to generate AES keys for encryption.
                        The passphrase creates an AES key using
                        <tt>PBKDF2WithHmacSHA256</tt>, a salt created from
                        passphrase checksum, and variable PBKDF2 iterations
                        based on passphrase checksum.  The PBKDF2 iterations
                        are between <tt>100100</tt> and <tt>960000</tt>.
      @param data Data to be encrypted.
      @param hash_iterations The number of iterations the AES-256 initialization
                             vector (IV) is hashed with SHA-256.  The minimum
                             iterations allowed is 1 for password-based
                             encryption.
      */
    static String encryptWithAES256(String passphrase, String data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        // sha256Sum should always return lower case but forcing toLowerCase
        // since this is used as an input for encryption and decryption.
        String salt = sha256Sum(passphrase).toLowerCase()
        byte[] b_secret = passwordKeyDerivation(passphrase, salt)
        byte[] b_iv = salt.substring(0, 16).getBytes('UTF-8')
        Integer iterations = (hash_iterations > 0) ? hash_iterations : 1
        encodeBase64(encryptWithAES256(b_secret, b_iv, data, iterations))
    }

    /**
      Decrypts ciphertext with AES-256 using a passphrase.  See the encrypt
      method for more details on both algorithms and usage.

      @see #DEFAULT_AES_ITERATIONS
      @see #encryptWithAES256(java.lang.String, java.lang.String, java.lang.Integer) encryptWithAES256 using passphrase method
      @see #decryptWithAES256(byte[], byte[], byte[], java.lang.Integer) decryptWithAES256 AES deciphering details
      @param passphrase A passphrase used to decrypt AES-256 ciphertext.  See
                        encrypt method for more details.
      @param data Base64 encoded ciphertext to be decrypted.
      @param hash_iterations The number of iterations the AES-256 initialization
                             vector (IV) is hashed with SHA-256.  The minimum
                             iterations allowed is 1 for password-based
                             encryption.
      */
    static String decryptWithAES256(String passphrase, String data, Integer hash_iterations = DEFAULT_AES_ITERATIONS) {
        // sha256Sum should always return lower case but forcing toLowerCase
        // since this is used as an input for encryption and decryption.
        String salt = sha256Sum(passphrase).toLowerCase()
        byte[] b_secret = passwordKeyDerivation(passphrase, salt)
        byte[] b_iv = salt.substring(0, 16).getBytes('UTF-8')
        byte[] b_data = decodeBase64Bytes(data)
        Integer iterations = (hash_iterations > 0) ? hash_iterations : 1
        decryptWithAES256(b_secret, b_iv, b_data, iterations)
    }
}
