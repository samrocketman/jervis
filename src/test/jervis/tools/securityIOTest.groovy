package jervis.tools
//the securityIOTest() class automatically sees the securityIO() class because they're in the same package
import org.junit.*

class securityIOTest extends GroovyTestCase {
    def security
    //set up before every test
    @Before protected void setUp() {
        security = new securityIO()
    }
    //tear down after every test
    @After protected void tearDown() {
        security = null
    }
    //test securityIO().decodeBase64()
    @Test public void test_securityIO_decodeBase64String() {
        def s = "data"
        String encoded = s.bytes.encodeBase64().toString()
        assert "ZGF0YQ==" == encoded
        assert security.decodeBase64String(encoded) == s
    }
    @Test public void test_securityIO_decodeBase64Bytes() {
        def s = "data"
        String encoded = s.bytes.encodeBase64().toString()
        assert "ZGF0YQ==" == encoded
        assert security.decodeBase64Bytes(encoded) == s.bytes
    }
    @Test public void test_securityIO_encodeBase64String() {
        assert "ZGF0YQ==" == security.encodeBase64("data")
    }
    @Test public void test_securityIO_encodeBase64Bytes() {
        assert "ZGF0YQ==" == security.encodeBase64("data".bytes)
    }
}
