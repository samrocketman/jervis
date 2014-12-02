package jervis.tools

class securityIO {
    public String id_rsa_priv
    public String id_rsa_pub
    public int id_rsa_keysize
    public static int default_key_size = 1024
    def securityIO() {
        def git = new scmGit()
        set_vars(git.getRoot() + '/id_rsa.pem', git.getRoot() + '/id_rsa.pub.pem', default_key_size)
    }
    def securityIO(int keysize) {
        def git = new scmGit()
        set_vars(git.getRoot() + '/id_rsa.pem', git.getRoot() + '/id_rsa.pub.pem', keysize)
    }
    def securityIO(String priv_key_file_path, String pub_key_file_path, int keysize) {
        set_vars(priv_key_file_path, pub_key_file_path, keysize)
    }
    def securityIO(String path) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', default_key_size)
    }
    def securityIO(String path, int keysize) {
        set_vars(checkPath(path) + '/id_rsa.pem', checkPath(path) + '/id_rsa.pub.pem', keysize)
    }
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
    def set_vars(String priv_key_file_path, String pub_key_file_path, int keysize) {
        id_rsa_priv = priv_key_file_path
        id_rsa_pub = pub_key_file_path
        id_rsa_keysize = keysize
    }
    //decode base64 strings into decoded strings
    public String decodeBase64String(String content) {
        return new String(content.decodeBase64())
    }
    public byte[] decodeBase64Bytes(String content) {
        return content.decodeBase64()
    }
    public String encodeBase64(String content) {
        return content.bytes.encodeBase64().toString()
    }
    public String encodeBase64(byte[] content) {
        return content.encodeBase64().toString()
    }
    public void generate_rsa_pair(String priv_key_file_path, String pub_key_file_path, int keysize) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def process = ['openssl', 'genrsa', '-out', priv_key_file_path, keysize.toString()].execute()
        process.waitForProcessOutput(stdout, stderr)
        if(process.exitValue()) {
            throw new Exception(stderr.toString())
        }
        process = ['openssl', 'rsa', '-in', priv_key_file_path, '-pubout', '-outform', 'pem', '-out', pub_key_file_path].execute()
        process.waitForProcessOutput(stdout, stderr)
        if(process.exitValue()) {
            throw new Exception(stderr.toString())
        }
    }
    public void generate_rsa_pair() {
        generate_rsa_pair(id_rsa_priv, id_rsa_pub, id_rsa_keysize)
    }
    public String rsaEncrypt(String plaintext) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc1 = ['echo', plaintext.trim()].execute()
        def proc2 = ['openssl', 'rsautl', '-encrypt', '-inkey', id_rsa_pub, '-pubin'].execute()
        def proc3 = ['base64','-w0'].execute()
        proc1 | proc2 | proc3
        proc3.waitForProcessOutput(stdout, stderr)
        if(proc3.exitValue()) {
            throw new Exception(stderr.toString())
        }
        else {
            return stdout.toString().trim()
        }
    }
    public String rsaDecrypt(String ciphertext) {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def proc1 = ['echo', ciphertext.trim()].execute()
        def proc2 = ['base64','-d'].execute()
        def proc3 = ['openssl', 'rsautl', '-decrypt', '-inkey', id_rsa_priv].execute()
        proc1 | proc2 | proc3
        proc3.waitForProcessOutput(stdout, stderr)
        if(proc3.exitValue()) {
            throw new Exception(stderr.toString())
        }
        else {
            return stdout.toString().trim()
        }
    }
}
