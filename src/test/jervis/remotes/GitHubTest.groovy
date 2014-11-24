package jervis.remotes
//the GitHubTest() class automatically sees the GitHub() class because they're in the same package
import org.junit.*

class GitHubTest extends GroovyTestCase {
    def mygh

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        mygh = new GitHub()
    }
    //tear down after every test
    @After protected void tearDown() {
        super.tearDown()
        mygh = null
    }
    //test GitHub().gh_web
    @Test public void test_GitHub_set1Gh_web() {
        mygh.gh_web = "http://server"
        assert mygh.gh_web == "http://server/"
    }
    @Test public void test_GitHub_set2Gh_web() {
        mygh.gh_web = "http://server/"
        assert mygh.gh_web == "http://server/"
    }
    @Test public void test_GitHub_readGh_web() {
        assert mygh.gh_web == "https://github.com/"
    }
    //test GitHub().gh_api
    @Test public void test_GitHub_set1Gh_api() {
        mygh.gh_api = "http://server"
        assert mygh.gh_api == "http://server/"
    }
    @Test public void test_GitHub_set2Gh_api() {
        mygh.gh_api = "http://server/"
        assert mygh.gh_api == "http://server/"
    }
    @Test public void test_GitHub_readGh_api() {
        assert mygh.gh_api == "https://api.github.com/"
    }
    //test GitHub().gh_clone
    @Test public void test_GitHub_set1Gh_clone() {
        mygh.gh_clone = "http://server"
        assert mygh.gh_clone == "http://server/"
    }
    @Test public void test_GitHub_set2Gh_clone() {
        mygh.gh_clone = "http://server/"
        assert mygh.gh_clone == "http://server/"
    }
    @Test public void test_GitHub_readGh_clone() {
        assert mygh.gh_clone == "git://github.com/"
    }
    //test GitHub().gh_token
    @Test public void test_GitHub_set1Gh_token() {
        mygh.gh_token = "a"
        assert mygh.gh_token == "a"
    }
    @Test public void test_GitHub_set2Gh_token() {
        mygh.gh_token = ""
        assert mygh.gh_token == null
    }
    @Test public void test_GitHub_decodeBase64() {
        def s = "Without tests, you're going to have a bad time!"
        String encoded = s.bytes.encodeBase64().toString()
        assert "V2l0aG91dCB0ZXN0cywgeW91J3JlIGdvaW5nIHRvIGhhdmUgYSBiYWQgdGltZSE=" == encoded
        assert mygh.decodeBase64(encoded) == s
    }
    @Test public void test_GitHub_getWebEndpoint() {
        assert mygh.getWebEndpoint() == mygh.gh_web
        assert mygh.getWebEndpoint() == "https://github.com/"
    }
}
