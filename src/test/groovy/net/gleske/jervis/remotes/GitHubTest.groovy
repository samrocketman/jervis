/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.remotes
//the GitHubTest() class automatically sees the GitHub() class because they're in the same package
import org.junit.After
import org.junit.Before
import org.junit.Test
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

class GitHubTest extends GroovyTestCase {
    def mygh
    def url

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        mockStaticUrl(url, URL)
        mygh = new GitHub()
    }
    //tear down after every test
    @After protected void tearDown() {
        mygh = null
        super.tearDown()
    }
    //test GitHub().gh_web
    @Test public void test_GitHub_set1Gh_web() {
        mygh.gh_web = 'http://server'
        assert mygh.gh_web == 'http://server/'
        assert mygh.gh_api == 'http://server/api/v3/'
    }
    @Test public void test_GitHub_set2Gh_web() {
        mygh.gh_web = 'http://server/'
        assert mygh.gh_web == 'http://server/'
        assert mygh.gh_api == 'http://server/api/v3/'
    }
    @Test public void test_GitHub_set3Gh_web() {
        mygh.gh_web = 'https://github.com/'
        assert mygh.gh_web == 'https://github.com/'
        assert mygh.gh_api == 'https://api.github.com/'
    }
    @Test public void test_GitHub_getGh_web() {
        assert mygh.gh_web == 'https://github.com/'
        assert mygh.gh_api == 'https://api.github.com/'
    }
    //test GitHub().gh_api
    @Test public void test_GitHub_set1Gh_api() {
        mygh.gh_api = 'http://server'
        assert mygh.gh_api == 'http://server/'
    }
    @Test public void test_GitHub_set2Gh_api() {
        mygh.gh_api = 'http://server/'
        assert mygh.gh_api == 'http://server/'
    }
    @Test public void test_GitHub_set3Gh_api() {
        mygh.gh_api = 'https://github.com/api/v3/'
        assert mygh.gh_api == 'https://api.github.com/'
    }
    @Test public void test_GitHub_getGh_api() {
        assert mygh.gh_api == 'https://api.github.com/'
    }
    //test GitHub().gh_clone
    @Test public void test_GitHub_set1Gh_clone() {
        mygh.gh_clone = 'http://server'
        assert mygh.gh_clone == 'http://server/'
    }
    @Test public void test_GitHub_set2Gh_clone() {
        mygh.gh_clone = 'http://server/'
        assert mygh.gh_clone == 'http://server/'
    }
    @Test public void test_GitHub_getGh_clone() {
        assert mygh.gh_clone == 'https://github.com/'
    }
    //test GitHub().gh_token
    @Test public void test_GitHub_set1Gh_token() {
        mygh.gh_token = 'a'
        assert mygh.gh_token == 'a'
    }
    @Test public void test_GitHub_set2Gh_token() {
        mygh.gh_token = ''
        assert mygh.gh_token == null
    }
    @Test public void test_GitHub_set3Gh_token() {
        mygh.gh_token = 'a'
        assert 'language: groovy\n' == mygh.getFile('samrocketman/jervis', '.travis.yml', 'master')
    }
    //test GitHub().getWebUrl()
    @Test public void test_GitHub_getWebUrl1() {
        assert mygh.getWebUrl() == mygh.gh_web
        assert mygh.getWebUrl() == 'https://github.com/'
    }
    @Test public void test_GitHub_getWebUrl2() {
        mygh.gh_web = 'http://server/'
        assert mygh.getWebUrl() == 'http://server/'
    }
    //test GitHub().getCloneUrl()
    @Test public void test_GitHub_getCloneUrl1() {
        assert mygh.getCloneUrl() == mygh.gh_clone
        assert mygh.getCloneUrl() == 'https://github.com/'
    }
    @Test public void test_GitHub_getCloneUrl2() {
        mygh.gh_clone = 'http://server/'
        assert mygh.getCloneUrl() == 'http://server/'
    }
    //test GitHub().toString()
    @Test public void test_GitHub_toString1() {
        assert mygh.toString() == 'GitHub'
    }
    @Test public void test_GitHub_toString2() {
        mygh.gh_web = 'http://server/'
        assert mygh.toString() == 'GitHub Enterprise'
    }
    @Test public void test_GitHub_branches() {
        assert ['gh-pages', 'master'] == mygh.branches('samrocketman/jervis')
    }
    @Test public void test_GitHub_getFile() {
        assert 'language: groovy\n' == mygh.getFile('samrocketman/jervis', '.travis.yml', 'master')
    }
    @Test public void test_GitHub_getFile_default() {
        assert 'language: groovy\n' == mygh.getFile('samrocketman/jervis', '.travis.yml')
    }
    @Test public void test_GitHub_getFolderListing() {
        assert ['.gitignore', '.travis.yml', 'LICENSE', 'README.md', 'build.gradle', 'src'] == mygh.getFolderListing('samrocketman/jervis', '/', 'master')
        assert ['main', 'resources', 'test'] == mygh.getFolderListing('samrocketman/jervis', 'src', 'master')
    }
    @Test public void test_GitHub_getFolderListing_default() {
        assert ['.gitignore', '.travis.yml', 'LICENSE', 'README.md', 'build.gradle', 'src'] == mygh.getFolderListing('samrocketman/jervis', '/')
        assert ['.gitignore', '.travis.yml', 'LICENSE', 'README.md', 'build.gradle', 'src'] == mygh.getFolderListing('samrocketman/jervis')
        assert ['main', 'resources', 'test'] == mygh.getFolderListing('samrocketman/jervis', 'src')
    }
    @Test public void test_GitHub_isUser() {
        assert true == mygh.isUser("samrocketman")
        assert false == mygh.isUser("jenkinsci")
    }
}
