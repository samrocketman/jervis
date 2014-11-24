package jervis.remotes

class GitHubTest extends GroovyTestCase {
    protected void setUp() {
        super.setUp()
        def mygh = new GitHub()
    }
    def testsetGh_web() {
        assert mygh.gh_web == "https://github.com/"
        mygh.gh_web = "http://server"
        assert mygh.gh_web == "http://server/"
        mygh.gh_web = "http://server/"
        assert mygh.gh_web == "http://server/"
    }
}
