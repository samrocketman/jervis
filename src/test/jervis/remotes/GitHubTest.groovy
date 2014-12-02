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
        mygh = null
        super.tearDown()
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
    @Test public void test_GitHub_getGh_web() {
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
    @Test public void test_GitHub_getGh_api() {
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
    @Test public void test_GitHub_getGh_clone() {
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
    //test GitHub().getWebUrl()
    @Test public void test_GitHub_getWebUrl1() {
        assert mygh.getWebUrl() == mygh.gh_web
        assert mygh.getWebUrl() == "https://github.com/"
    }
    @Test public void test_GitHub_getWebUrl2() {
        mygh.gh_web = "http://server/"
        assert mygh.getWebUrl() == "http://server/"
    }
    //test GitHub().getCloneUrl()
    @Test public void test_GitHub_getCloneUrl1() {
        assert mygh.getCloneUrl() == mygh.gh_clone
        assert mygh.getCloneUrl() == "git://github.com/"
    }
    @Test public void test_GitHub_getCloneUrl2() {
        mygh.gh_clone = "http://server/"
        assert mygh.getCloneUrl() == "http://server/"
    }
    //test GitHub().type()
    @Test public void test_GitHub_type1() {
        assert mygh.type() == "GitHub"
    }
    @Test public void test_GitHub_type2() {
        mygh.gh_web = "http://server/"
        assert mygh.type() == "GitHub Enterprise"
    }
}
