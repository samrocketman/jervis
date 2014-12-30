package jervis.tools
//the fileIOTest() class automatically sees the fileIO() class because they're in the same package
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
    @Test public void test_fileIO_fileExists1() {
        assert true == fileio.fileExists("readFile(/src/testResources/sample_script.sh)")
    }
    @Test public void test_fileIO_fileExists2() {
        assert false == fileio.fileExists("readFile(/src/testResources/does_not_exist.sh)")
    }
    @Test public void test_fileIO_readFile1() {
        assert '#!/bin/bash\necho "this is a sample script"' == fileio.readFile("readFile(/src/testResources/sample_script.sh)")
    }
    @Test public void test_fileIO_readFile2() {
        assert 'readFile(/src/testResources/does_not_exist.sh)' == fileio.readFile("readFile(/src/testResources/does_not_exist.sh)")
    }
}
