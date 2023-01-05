/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

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

import static net.gleske.jervis.remotes.SimpleRestService.addTrailingSlash
import static net.gleske.jervis.remotes.SimpleRestService.apiFetch
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl
import net.gleske.jervis.exceptions.JervisException

import org.junit.After
import org.junit.Before
import org.junit.Test

class SimpleRestServiceTest extends GroovyTestCase {
    def url
    Map request_meta
    List request_history = []

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        request_meta = [:]
        mockStaticUrl(url, URL, request_meta, false, '', request_history)
    }
    //tear down after every test
    @After protected void tearDown() {
        request_meta = null
        super.tearDown()
    }
    @Test public void test_SimpleRestService_fail_instantiation() {
        shouldFail(IllegalStateException) {
            new SimpleRestService()
        }
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
        shouldFail(IOException) {
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
        def response = apiFetch(new URL('https://example.com/post/endpoint'), http_headers, 'DELETE')
        assert response == 200
        assert request_meta['method'] == 'DELETE'
    }
    @Test public void test_SimpleRestService_apiFetch_no_json_parse() {
        Map http_headers = ['Content-Type': 'text/plain']
        assert '[]' == apiFetch(new URL('https://api.github.com/repos/samrocketman/emptyList/contents'), http_headers)
    }
    @Test public void test_SimpleRestService_apiFetch_no_empty_response_default_to_string() {
        Map http_headers = ['Content-Type': 'text/plain']
        assert '' == apiFetch(new URL('https://api.github.com/repos/samrocketman/empty/contents'))
        assert '' == apiFetch(new URL('https://api.github.com/repos/samrocketman/empty/contents'), http_headers)
    }
    @Test public void test_SimpleRestService_apiFetch_get_response_json_parse() {
        Map parse_http_headers = ['Parse-JSON': true]
        def response = apiFetch(new URL('https://api.github.com/users/samrocketman'), parse_http_headers)
        assert response in Map
        assert response['login'] == 'samrocketman'
        assert !('Parse-JSON' in request_meta.headers.keySet())
        parse_http_headers = ['Parse-JSON': 'true']
        response = apiFetch(new URL('https://api.github.com/users/samrocketman'), parse_http_headers)
        assert response in Map
        assert response['login'] == 'samrocketman'
        assert !('Parse-JSON' in request_meta.headers.keySet())
    }
    @Test public void test_SimpleRestService_apiFetch_get_response_json_no_parse() {
        Map parse_http_headers = ['Parse-JSON': false]
        def response = apiFetch(new URL('https://api.github.com/users/samrocketman'), parse_http_headers)
        assert response in String
        assert !('Parse-JSON' in request_meta.headers.keySet())
        parse_http_headers = ['Parse-JSON': 'false']
        response = apiFetch(new URL('https://api.github.com/users/samrocketman'), parse_http_headers)
        assert response in String
        assert !('Parse-JSON' in request_meta.headers.keySet())
    }
    @Test public void test_SimpleRestService_apiFetch_response_headers() {
        Map headers = apiFetch(new URL('https://www.example.com/doesnotexist'), ['Content-Type': 'text/html'], 'HEAD')
        assert headers['Content-Length'].toList().first()
        assert request_history*.url == ['https://www.example.com/doesnotexist']
        assert request_history*.method == ['HEAD']
    }
    @Test public void test_SimpleRestService_apiFetch_response_code() {
        // get HTTP response code
        assert 404 == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Content-Type': 'text/html', 'Response-Code': true], 'HEAD')
        assert 404 == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Content-Type': 'text/html', 'Response-Code': 'true'], 'HEAD')
    }
    @Test public void test_SimpleRestService_apiFetch_parsed_content() {
        Map response = apiFetch(new URL('https://www.example.com/doesnotexist'), [:], 'POST')
        assert [some: 'response'] == response
        assert [some: 'response'] == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Parse-JSON': true], 'POST')
        assert [some: 'response'] == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Parse-JSON': 'true'], 'POST')
    }
    @Test public void test_SimpleRestService_apiFetch_string_content() {
        assert '{"some":"response"}' == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Parse-JSON': false], 'POST')
        assert '{"some":"response"}' == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Parse-JSON': 'false'], 'POST')
    }
    @Test public void test_SimpleRestService_apiFetch_no_content() {
        request_meta.response_headers = Collections.unmodifiableMap([(null): Collections.unmodifiableList(['HTTP/1.1 204 No Content'])])
        def response = apiFetch(new URL('https://www.example.com/doesnotexist'), [:], 'POST')
        assert response == ''
        // get HTTP response code
        assert 204 == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Response-Code': true], 'POST')
    }
    @Test public void test_SimpleRestService_apiFetch_delete_example() {
        request_meta.response_headers = Collections.unmodifiableMap([(null): Collections.unmodifiableList(['HTTP/1.1 204 No Content'])])
        def response = apiFetch(new URL('https://www.example.com/doesnotexist'), [:], 'DELETE')
        assert response == 204
        assert request_history*.url == ['https://www.example.com/doesnotexist']
        assert request_history*.method == ['DELETE']
        // get HTTP response code
        assert '' == apiFetch(new URL('https://www.example.com/doesnotexist'), ['Response-Code': false], 'DELETE')
    }
    @Test public void test_SimpleRestService_addTrailingSlash() {
        String result = 'https://example.com/'
        assert result == addTrailingSlash('https://example.com')
        assert result == addTrailingSlash('https://example.com/')
    }
    @Test public void test_SimpleRestService_addTrailingSlash_suffix() {
        String result = 'https://example.com/v1/'
        assert result == addTrailingSlash('https://example.com', 'v1')
        assert result == addTrailingSlash('https://example.com', 'v1/')
        assert result == addTrailingSlash('https://example.com/', 'v1')
        assert result == addTrailingSlash('https://example.com/', 'v1/')
    }
}
