package jervis.tools
import jervis.tools.scmGit

class securityIO {
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
}
