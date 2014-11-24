package jervis.remotes
import org.junit.*

class GitHubTest extends GroovyTestCase {
    def mygh

    @Before protected void setUp() {
        super.setUp()
        mygh = new GitHub()
    }
    @After protected void tearDown() {
        super.tearDown()
        mygh = null
    }
    @Test public void testset1Gh_web() {
        mygh.gh_web = "http://server"
        assert mygh.gh_web == "http://server/"
    }
    @Test public void testset2Gh_web() {
        mygh.gh_web = "http://server/"
        assert mygh.gh_web == "http://server/"
    }
    @Test public void testreadGh_web() {
        assert mygh.gh_web == "https://github.com/"
    }
    @Test public void testset1Gh_api() {
        mygh.gh_api = "http://server"
        assert mygh.gh_api == "http://server/"
    }
    @Test public void testset2Gh_api() {
        mygh.gh_api = "http://server/"
        assert mygh.gh_api == "http://server/"
    }
    @Test public void testreadGh_api() {
        assert mygh.gh_api == "https://api.github.com/"
    }
    @Test public void testset1Gh_clone() {
        mygh.gh_clone = "http://server"
        assert mygh.gh_clone == "http://server/"
    }
    @Test public void testset2Gh_clone() {
        mygh.gh_clone = "http://server/"
        assert mygh.gh_clone == "http://server/"
    }
    @Test public void testreadGh_clone() {
        assert mygh.gh_clone == "git://github.com/"
    }
}
