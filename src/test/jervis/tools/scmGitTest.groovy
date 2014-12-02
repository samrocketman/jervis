package jervis.tools
//the scmGitTest() class automatically sees the scmGit() class because they're in the same package
import org.junit.*

class scmGitTest extends GroovyTestCase {
    def git
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        git = new scmGit("echo")
    }
    //tear down after every test
    @After protected void tearDown() {
        git = null
        super.tearDown()
    }
    //test getRoot()
    @Test public void test_scmGit_getRoot1() {
        assert "rev-parse --show-toplevel" == git.getRoot()
    }
    @Test public void test_scmGit_getRoot2() {
        git.git_root = "some git root"
        assert "some git root" == git.getRoot()
    }
    @Test public void test_scmGit_get_mygit() {
        assert "echo" == git.mygit
    }
}
