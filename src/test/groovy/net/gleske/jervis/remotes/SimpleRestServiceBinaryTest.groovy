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
//the SimpleRestServiceBinaryTest() class automatically sees the SimpleRestService() class because they're in the same package

import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl
import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.tools.GZip
import net.gleske.jervis.tools.SecurityIO


import java.util.zip.GZIPInputStream
import org.junit.After
import org.junit.Before
import org.junit.Test

class SimpleRestServiceBinaryTest extends GroovyTestCase {
    def url
    Map request_meta
    List request_history = []

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        request_meta = [:]
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256', request_history)
    }
    //tear down after every test
    @After protected void tearDown() {
        request_meta = null
        super.tearDown()
    }
    @Test public void test_SimpleRestService_apiFetch_binary_upload() {
        String username = 'admin'
        String password = 'admin123'

        // this example data will be compressed and uploaded to Nexus
        String upload_data = 'hello world\n\nMy friend\n'
        url = 'http://localhost:8081/repository/hosted-raw-repo/file.gz'
        URL api_url = new URL(url)
        Map http_headers = [
            Authorization: "Basic ${SecurityIO.encodeBase64("${username}:${password}")}",
            Accept: '*/*',
            Expect: '100-continue',
            'Binary-Data': true
        ]

        // make network call uploading or receiving binary data
        def response = SimpleRestService.apiFetch(
                api_url,
                http_headers,
                'PUT',
                'binary-data') { httpOutputStream ->

            // wrap the output stream with compression
            new GZip(httpOutputStream).withCloseable { gzip ->
                gzip << upload_data
            }
        }

        // get response message from Nexus
        assert request_history*.response_code == [201]
    }
    @Test public void test_SimpleRestService_apiFetch_binary_download() {
        URL api_url = new URL('http://localhost:8081/repository/hosted-raw-repo/file.gz')
        def response = SimpleRestService.apiFetch(api_url, ['Binary-Data': true])
        ByteArrayOutputStream plain = new ByteArrayOutputStream()

        response.getInputStream().withCloseable { is ->
            new GZIPInputStream(is).withCloseable { gunzip ->
            // decompress the downloaded text
            plain << gunzip
            }
        }
        assert plain.toString() == 'hello world\n\nMy friend\n'
        assert request_history*.response_code == [200]
    }
}
