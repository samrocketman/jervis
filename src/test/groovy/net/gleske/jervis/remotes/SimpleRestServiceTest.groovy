/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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
//the SimpleRestServiceTest() class automatically sees the SimpleRestService() class because they're in the same package

import org.junit.After
import org.junit.Before
import org.junit.Test

import net.gleske.jervis.exceptions.JervisException
import static net.gleske.jervis.remotes.SimpleRestService.apiFetch
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

class SimpleRestServiceTest extends GroovyTestCase {
    def url
    Map request_meta

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        request_meta = [:]
        mockStaticUrl(url, URL, request_meta)
    }
    //tear down after every test
    @After protected void tearDown() {
        request_meta = null
        super.tearDown()
    }
    @Test public void test_SimpleRestService_apiFetch_get() {
        Map response = apiFetch(new URL('https://api.github.com/users/samrocketman'))
        assert response['login'] == 'samrocketman'
    }
    @Test public void test_SimpleRestService_apiFetch_get_plain() {
        String response = apiFetch(new URL('https://example.com/post/endpoint')).trim()
        assert response == 'this is mock POST response data'
    }
    @Test public void test_SimpleRestService_apiFetch_get_fail() {
        shouldFail(RuntimeException) {
            apiFetch(new URL('https://example.com/does/not/exist'))
        }
    }
    @Test public void test_SimpleRestService_apiFetch_post() {
        Map response = apiFetch(new URL('https://api.github.com/users/samrocketman'), [:], 'POST', 'this is some test data')
        assert response['login'] == 'samrocketman'
        assert request_meta['method'] == 'POST'
        assert request_meta['data'].toString().trim() == 'this is some test data'
        assert request_meta['headers'] == ['Content-Type': 'application/json']
    }
    @Test public void test_SimpleRestService_apiFetch_post_plain() {
        Map http_headers = ['Content-Type': 'text/plain']
        String response = apiFetch(new URL('https://example.com/post/endpoint'), http_headers, 'POST', 'this is some test data').trim()
        assert response == 'this is mock POST response data'
        assert request_meta['method'] == 'POST'
        assert request_meta['data'].toString().trim() == 'this is some test data'
        assert request_meta['headers'] == http_headers
    }
    @Test public void test_SimpleRestService_apiFetch_post_fail() {
        shouldFail(RuntimeException) {
            apiFetch(new URL('https://example.com/does/not/exist'), http_headers, 'POST', 'this is some test data')
        }
    }
    @Test public void test_SimpleRestService_apiFetch_put() {
        Map http_headers = ['Content-Type': 'text/plain']
        String response = apiFetch(new URL('https://example.com/post/endpoint'), http_headers, 'PUT').trim()
        assert response == 'this is mock POST response data'
        assert request_meta['method'] == 'PUT'
    }
    @Test public void test_SimpleRestService_apiFetch_patch() {
        Map http_headers = ['Content-Type': 'text/plain']
        String response = apiFetch(new URL('https://example.com/post/endpoint'), http_headers, 'PATCH').trim()
        assert response == 'this is mock POST response data'
        assert request_meta['method'] == 'PATCH'
    }
    @Test public void test_SimpleRestService_apiFetch_delete() {
        Map http_headers = ['Content-Type': 'text/plain']
        String response = apiFetch(new URL('https://example.com/post/endpoint'), http_headers, 'DELETE').trim()
        assert response == 'this is mock POST response data'
        assert request_meta['method'] == 'DELETE'
    }
}
