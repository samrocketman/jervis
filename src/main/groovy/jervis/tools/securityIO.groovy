package jervis.tools

import jervis.exceptions.JervisException

/**
   A class to provide cryptographic features to Jervis such as RSA encryption and base64 encoding.

   <h2>Sample usage</h2>

<pre><tt>import jervis.tools.securityIO
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
    public String id_rsa_priv

    /**
      Path to the RSA public key.  This will be used by encryptiong and decryption.
      During key generation it is the location where the public key will be written.
      Default: <tt>/tmp/id_rsa.pub.pem</tt>
     */
    public String id_rsa_pub

    /**
      The key size in which RSA keys will be generated.  It is highly recommended this be <tt>1024</tt> bits or higher in powers of two.
      Default: value of <tt>{@link #default_key_size}</tt>.
     */
    public int id_rsa_keysize

    /**
      The default key size for <tt>{@link #id_rsa_keysize}</tt>.
      Default: <tt>1024</tt>
     */
    public static int default_key_size = 1024

    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
     */
    def securityIO() {
        set_vars('/tmp/id_rsa.pem', '/tmp/id_rsa.pub.pem', default_key_size)
    }

    /**
      Instantiates default values for <tt>{@link #id_rsa_priv}</tt> and <tt>{@link #id_rsa_pub}</tt> but
      <tt>{@link #id_rsa_keysize}</tt> is set using <tt>keysize</tt>.
     */
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
    private void set_vars(String priv_key_file_path, String pub_key_file_path, int keysize) {
        id_rsa_priv = priv_key_file_path
        id_rsa_pub = pub_key_file_path
        id_rsa_keysize = keysize
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
      It does not take values from <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, nor <tt>{@link #id_rsa_keysize}</tt>.

      For third party reference, this is essentially executing the following commands in a terminal.

<pre><tt>openssl genrsa -out /tmp/id_rsa 1024
openssl rsa -in /tmp/id_rsa -pubout -outform pem -out /tmp/id_rsa.pub</tt></pre>

      @param priv_key_file_path A file path where the private key will be written on the filesystem.
      @param pub_key_file_path  A file path where the public key will be written on the filesystem.
      @param keysize            The key size in bits of the key pair.
     */
    public void generate_rsa_pair(String priv_key_file_path, String pub_key_file_path, int keysize) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def process = ['openssl', 'genrsa', '-out', priv_key_file_path, keysize.toString()].execute()
        process.waitForProcessOutput(stdout, stderr)
        if(process.exitValue()) {
            throw new JervisException(stderr.toString())
        }
        process = ['openssl', 'rsa', '-in', priv_key_file_path, '-pubout', '-outform', 'pem', '-out', pub_key_file_path].execute()
        process.waitForProcessOutput(stdout, stderr)
        if(process.exitValue()) {
            throw new JervisException(stderr.toString())
        }
    }

    /**
      Generate an RSA key pair (private key and public key) using <tt>{@link #id_rsa_priv}</tt>, <tt>{@link #id_rsa_pub}</tt>, and <tt>{@link #id_rsa_keysize}</tt>.
     */
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
    public String rsaEncrypt(String plaintext) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc1 = ['echo', plaintext.trim()].execute()
        def proc2 = ['openssl', 'rsautl', '-encrypt', '-inkey', id_rsa_pub, '-pubin'].execute()
        def proc3 = ['base64','-w0'].execute()
        proc1 | proc2 | proc3
        proc2.waitForProcessOutput(null, stderr)
        proc3.waitForProcessOutput(stdout, null)
        if(proc2.exitValue()) {
            throw new JervisException(stderr.toString())
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
    public String rsaDecrypt(String ciphertext) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc1 = ['echo', '-n', ciphertext.trim()].execute()
        def proc2 = ['base64','-d'].execute()
        def proc3 = ['openssl', 'rsautl', '-decrypt', '-inkey', id_rsa_priv].execute()
        proc1 | proc2 | proc3
        proc3.waitForProcessOutput(stdout, stderr)
        if(proc3.exitValue()) {
            throw new JervisException(stderr.toString())
        }
        else {
            return stdout.toString().trim()
        }
    }
}
