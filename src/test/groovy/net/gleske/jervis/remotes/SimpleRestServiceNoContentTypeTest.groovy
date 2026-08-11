/*
   Copyright 2014-2026 Sam Gleske - https://github.com/samrocketman/jervis

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

import static net.gleske.jervis.remotes.SimpleRestService.apiFetch

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
  Responses which omit the <tt>Content-Type</tt> header are served by a real
  loopback server rather than the static mocks.  The behavior under test is
  <tt>{@link java.net.URLConnection#getContent()}</tt> throwing
  <tt>UnknownServiceException</tt>, which belongs to the JDK: a mock declaring
  that it throws would only be asserting against itself.
  */
class SimpleRestServiceNoContentTypeTest extends GroovyTestCase {
    HttpServer server
    String baseUrl

    @Before protected void setUp() {
        super.setUp()
        // mockStaticUrl replaces URL.metaClass process-wide and no test restores
        // it, so whether a real connection is possible here depends on which
        // classes ran first.  Drop any installed mock; classes which want one
        // install it in their own setUp.
        GroovySystem.metaClassRegistry.removeMetaClass(URL)
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        // Zero-length chunked responses, with no Content-Type and no
        // Content-Length.  sendResponseHeaders(code, 0) means chunked here.
        server.createContext('/empty') { exchange ->
            exchange.sendResponseHeaders(200, 0)
            exchange.getResponseBody().close()
        }
        server.createContext('/body') { exchange ->
            byte[] body = '{"some":"response"}'.bytes
            exchange.sendResponseHeaders(200, 0)
            exchange.getResponseBody().withCloseable { it << body }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.getAddress().getPort()}"
    }
    @After protected void tearDown() {
        server.stop(0)
        GroovySystem.metaClassRegistry.removeMetaClass(URL)
        super.tearDown()
    }
    @Test public void test_SimpleRestService_apiFetch_empty_chunked_response_without_content_type() {
        // A bodyless chunked 200 is a success, not a transport failure.  Before
        // this was handled it threw UnknownServiceException('no content-type').
        Map response = apiFetch(new URL("${baseUrl}/empty"), ['Response-Map': true], 'POST', '{}')
        assert response.response_code == 200
        assert response.error == false
        assert response.content == null
    }
    @Test public void test_SimpleRestService_apiFetch_response_body_without_content_type() {
        // A body still parses when the server never said what it was; the
        // Content-Type default only governs the request.
        Map response = apiFetch(new URL("${baseUrl}/body"), ['Response-Map': true], 'POST', '{}')
        assert response.response_code == 200
        assert response.content == [some: 'response']
    }
}
