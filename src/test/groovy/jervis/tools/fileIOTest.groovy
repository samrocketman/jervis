/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */
package jervis.tools
//the fileIOTest() class automatically sees the fileIO() class because they're in the same package
import org.junit.After
import org.junit.Before
import org.junit.Test

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
        assert true == fileio.isReadFile('readFile(/path/to/file.sh)')
    }
    @Test public void test_fileIO_isReadFile2() {
        assert false == fileio.isReadFile('readFile(path/to/file.sh)')
    }
    @Test public void test_fileIO_fileExists1() {
        assert true == fileio.fileExists('readFile(/src/test/resources/sample_script.sh)')
    }
    @Test public void test_fileIO_fileExists2() {
        assert false == fileio.fileExists('readFile(/src/test/resources/does_not_exist.sh)')
    }
    @Test public void test_fileIO_fileExists3() {
        assert false == fileio.fileExists('readFile(/src/test/resources/sample_script.sh')
    }
    @Test public void test_fileIO_readFile1() {
        assert '#!/bin/bash\necho "this is a sample script"' == fileio.readFile('readFile(/src/test/resources/sample_script.sh)')
    }
    @Test public void test_fileIO_readFile2() {
        assert 'readFile(/src/test/resources/does_not_exist.sh)' == fileio.readFile('readFile(/src/test/resources/does_not_exist.sh)')
    }
    @Test public void test_fileIO_readFile3() {
        assert 'readFile(/src/test/resources/does_not_exist.sh' == fileio.readFile('readFile(/src/test/resources/does_not_exist.sh')
    }
}
