package jervis.tools
//the lifecycleValidatorTest() class automatically sees the lifecycleValidatorTest() class because they're in the same package
import org.junit.*

class fileIOTest extends GroovyTestCase {
    def fileio
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        fileio = new fileIO()
    }
    //tear down after every test
    @After protected void tearDown() {
        fileio = null
        super.tearDown()
    }
    @Test public void test_fileIO_isReadFile1() {
        assert true == fileio.isReadFile("readFile(/path/to/file.sh)")
    }
    @Test public void test_fileIO_isReadFile2() {
        assert false == fileio.isReadFile("readFile(path/to/file.sh)")
    }
}
