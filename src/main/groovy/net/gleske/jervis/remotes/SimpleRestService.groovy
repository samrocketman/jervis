/*
   Copyright 2014-2024 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.tools.YamlOperator

import groovy.json.JsonBuilder

/**
  A simple class which makes using REST services like the GitHub API really
  easy.  This utilizes helpful features of Groovy while not relying on any
  heavier external libraries for HTTP communication.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a
  <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

  <h4>A basic get request</h4>
  <p>By default, JSON-based REST APIs are assumed.  The response will
  automatically be parsed into a plain Java Object.  In this example, a HashMap
  is returned.</p>

<pre><code>
import net.gleske.jervis.remotes.SimpleRestService

SimpleRestService.apiFetch(new URL('https://api.github.com/meta'))
</code></pre>

  <h4>A basic get request with no response processing</h4>
  <p>  So if you do not set the
  content type, then an <tt>application/json</tt> response will be assumed.  If
  you do not want any response processing you can force no content type by
  passing a <tt>null</tt> HTTP request header value for
  <tt>Content-Type</tt>.</p>

<pre><code>
import net.gleske.jervis.remotes.SimpleRestService

SimpleRestService.apiFetch(new URL('http://www.example.com/'), ['Content-Type': null])
</code></pre>

  <h4>Handling binary data</h4>
  <p>This client can send or receive binary data by using a special header
  <tt>Binary-Data</tt>.  Compare this example with
  <tt>{@link net.gleske.jervis.tools.GZip}</tt></p>

<pre><code>
import java.net.HttpURLConnection
import net.gleske.jervis.remotes.SimpleRestService
import net.gleske.jervis.tools.GZip
import net.gleske.jervis.tools.SecurityIO

String username = 'some user'
String password = 'some pass'

// this example data will be compressed and uploaded to Nexus
String upload_data = 'hello world\n\nMy friend\n'

URL api_url = new URL('http://localhost:8081/repository/hosted-raw-repo/file.gz')
Map http_headers = [
    Authorization: "Basic ${SecurityIO.encodeBase64("${username}:${password}")}",
    Accept: '*&sol;*',
    Expect: '100-continue',
    'Binary-Data': true
]

// make network call uploading or receiving binary data
HttpURLConnection response = SimpleRestService.apiFetch(
        api_url,
        http_headers,
        'PUT',
        'binary-data') { httpOutputStream -&gt;

    // wrap the output stream with compression
    new GZip(httpOutputStream).withCloseable { gzip -&gt;
        gzip &lt;&lt; upload_data
    }
}

// get response message from Nexus
response.getHeaderFields()[null][0]
</code></pre>

  <p>You can also use the <tt>Binary-Data</tt> special header to get back the raw HTTP request response so you can do your own custom processing.</p>

<pre><code>
import net.gleske.jervis.remotes.SimpleRestService

SimpleRestService.apiFetch(new URL('http://www.example.com/'), ['Binary-Data': true])
</code></pre>

  <p>You would also do a similar request to download compressed data you uploaded to Nexus.</p>

<pre><code>
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream
import net.gleske.jervis.remotes.SimpleRestService

URL api_url = new URL('http://localhost:8081/repository/hosted-raw-repo/file.gz')
HttpURLConnection response = SimpleRestService.apiFetch(api_url, ['Binary-Data': true])
ByteArrayOutputStream plain = new ByteArrayOutputStream()

response.inputStream.withCloseable { is -&gt;
    new GZIPInputStream(is).withCloseable { gunzip -&gt;
        // decompress the downloaded text
        plain &lt;&lt; gunzip
    }
}
assert plain.toString() == 'hello world\n\nMy friend\n'
</code></pre>

  */
class SimpleRestService {

    private SimpleRestService() {
        throw new IllegalStateException('ERROR: This utility class only provides static methods and is not meant for instantiation.  See Java doc for this class for examples.')
    }

    /**
      Meant for flexibly setting API URLs, this will enforce a trailing slash
      and optionaly a suffix such as an API version applied to the URL.

      @param url    Typically an API URL where a trailing slash will be added if
                    missing.
      @param suffix Typically an API version path where a trailing slash will be
                    added if missing.  If this option is provided, then it will
                    ensure <tt>url</tt> ends with the <tt>suffix</tt> and add it
                    if missing.
      */
    static String addTrailingSlash(String url, String suffix = '') {
        String result = url
        String end = suffix
        if(end && !end.endsWith('/')) {
            end += '/'
        }
        if(!result.endsWith('/')) {
            result += '/'
        }
        if(suffix && !result.endsWith(end)) {
            result += end
        }
        result
    }

    /**
      A method for converting an object comprising of standard Java classes
      <tt>Map</tt> or <tt>List</tt> to a JSON String.

      @param obj Any object.
      @return A <tt>String</tt> of JSON text which could be later parsed if the
              <tt>obj</tt> is a <tt>Map</tt> or <tt>List</tt>.  Otherwise, the
              original object is returned if serialization is not possible.
      */
    static String objToJson(def obj) {
        if([Map, List].any { obj in it}) {
            (obj as JsonBuilder).toString()
        }
        else {
            obj
        }
    }

    /**
      <tt>apiFetch()</tt> can be used to submit HTTP commands common to REST
      APIs.  If <tt>http_headers</tt> does not contain a <tt>Content-Type</tt>
      header, then <tt>application/json</tt> is assumed.

      <p>Extra behaviors are provided by special <tt>http_headers</tt>.  These
      headers are not sent over HTTP but instead change the behavior of the
      response.</p>
      <dl>
      <dt><b>Special HTTP Headers:</b></dt>
      <dd>
        <tt>Binary-Data</tt> - Enable send and receive of binary data.  No
        <tt>Content-Type</tt> is set and no automatic response processing is
        performed.  An
        <tt>{@link java.net.HttpURLConnection}</tt> will
        always be returned.  All other special HTTP headers will be ignored.
      </dd>
      <dd>
        <tt>Parse-JSON</tt> - For JSON-based APIs responses are automatically
        parsed.  This setting can disable automatic parsing if set to
        <tt>false</tt>.
      </dd>
      <dd>
        <tt>Response-Code</tt> - Return the HTTP response code.
      </dd>
      <dd>
        <tt>Response-Headers</tt> - Return the HTTP response headers as an
        unmodifiable Map.  The <tt>DELETE</tt> HTTP method requires setting
        <tt>Response-Code</tt> to <tt>false</tt>.  The <tt>HEAD</tt> HTTP
        method implies returning response headers.
      </dd>
      <dd>
        <tt>Response-Map</tt> - Return a HashMap containing
        <tt>response_code</tt>, <tt>response_headers</tt>,
        <tt>response_content</tt>, and <tt>error</tt> keys.  <tt>error</tt>
        will return <tt>true</tt> if <tt>response_code</tt> is a 4XX or 5XX
        HTTP code.
      </dd>
      </dl>

      @param api_url A URL of a REST endpoint in which to make an HTTP call.
      @param http_headers HTTP headers to pass as part of the HTTP request.  By
                  default only <tt>Content-Type: application/json</tt> HTTP
                  header will be set.
      @param http_method The HTTP method or action to request from the server.
                  Currently supported methods include: GET, POST, PUT, DELETE,
                  and PATCH.
      @param data Send <tt>String</tt> data to the remote connection via
                  <tt>{@link java.io.OutputStream}</tt>
                  <tt>{@link java.io.Writer}</tt>.
      @param httpOutputStream A <tt>Closure</tt> in which an
                  <tt>{@link java.io.OutputStream}</tt> is passed as an
                  argument.  This allows caller to write binary data to the
                  remote as part of the HTTP connection.  The
                  <tt>httpOutputStream</tt> will only be called if
                  <tt>data</tt> is a non-zero length String.

      @return If <tt>Content-Type</tt> HTTP header is
              <tt>application/json</tt>, then one of two types will be
              returned: <tt>HashMap</tt> or <tt>ArrayList</tt> (for JSON Object
              and JSON Array respectively).  If any other <tt>Content-Type</tt>
              HTTP header used then a <tt>String</tt> will be returned and it
              will be left up to the caller to parse the result.  If
              <tt>Binary-Data</tt> request header was passed, then an
              <tt>{@link java.net.HttpURLConnection}</tt> will
              be returned and all response processing will be left to the
              caller.
      */
    static def apiFetch(URL api_url, Map http_headers = [:], String http_method = 'GET', def data = '', Closure httpOutputStream = null) {
        // case insensitive HashMap with default value fallback to looking up case insensitive values
        Map tmp_http_headers = [:].withDefault { key ->
            if(!(key in String)) {
                return null
            }
            for(Map.Entry entry : http_headers.entrySet()) {
                if(key.equalsIgnoreCase(entry.key)) {
                    return entry.value;
                }
            }
            null
        }
        // copy valid fields only
        http_headers.each { k, v ->
            if(v in Boolean) {
                tmp_http_headers[k] = v
            }
            else {
                tmp_http_headers[k] = v.toString()
            }
        }
        Boolean binary_data = YamlOperator.getObjectValue(tmp_http_headers, 'Binary-Data', false)
        Boolean user_specified_content_type = tmp_http_headers.keySet().toList()*.equalsIgnoreCase('Content-Type').any { it }
        if(!user_specified_content_type && !binary_data) {
            tmp_http_headers['Content-Type'] = 'application/json'
        }
        Boolean parse_json = tmp_http_headers['Content-Type'] == 'application/json'
        parse_json = YamlOperator.getObjectValue(tmp_http_headers, 'Parse-JSON', parse_json)
        Boolean return_response_code = (http_method == 'DELETE')
        return_response_code = YamlOperator.getObjectValue(tmp_http_headers, 'Response-Code', return_response_code)
        Boolean only_response_headers = YamlOperator.getObjectValue(tmp_http_headers, 'Response-Headers', false)
        Boolean get_response_map = YamlOperator.getObjectValue(tmp_http_headers, 'Response-Map', false)
        if(!return_response_code && http_method == 'HEAD') {
            only_response_headers = true
        }

        Map response_headers = [:]
        Integer response_code = 0
        Boolean response_failure = false
        String response_content = ''
        //data_response could be either a List or Map depending on the JSON
        def response = api_url.openConnection().with { conn ->
            if(http_method.toUpperCase() != 'GET' && data.size()) {
                conn.setDoOutput(true)
            }
            // Set connection timeout - JVM property takes priority over header
            Integer timeout = 30000 // default 30 seconds
            String timeoutProperty = System.getProperty('net.gleske.jervis.remotes.SimpleRestService.timeoutMillis')
            if(timeoutProperty) {
                timeout = Integer.parseInt(timeoutProperty)
            } else if(tmp_http_headers.find { it.key.toLowerCase() == 'x-http-timeout-millis' }) {
                timeout = Integer.parseInt(tmp_http_headers.find { it.key.toLowerCase() == 'x-http-timeout-millis' }.value.toString())
            }
            conn.setConnectTimeout(timeout)

            tmp_http_headers.each { k, v ->
                // ignored headers are skipped
                if(k.toLowerCase() in ['binary-data', 'parse-json', 'response-code', 'response-headers', 'response-map', 'x-http-binary-data', 'x-http-method-override', 'x-http-timeout-millis']) {
                    return
                }
                conn.setRequestProperty(k, v)
            }
            // conn.setRequestMethod(request_method) but instead bypass internal
            // Java error checking by setting private variable directly.  This
            // is necessary to enable services which have custom HTTP verbs.
            // Error checking the verb is not necessary.  For example, HashiCorp
            // Vault has custom HTTP verbs such as LIST.
            // source: https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.base/share/classes/java/net/HttpURLConnection.java
            List java_supported_methods = ['GET', 'POST', 'HEAD', 'OPTIONS', 'PUT', 'DELETE', 'TRACE']
            if(http_method.toUpperCase() in java_supported_methods) {
                conn.setRequestMethod(http_method.toUpperCase())
            }
            else {
                // else a non-standard HTTP verb is desirable
                conn.@method = http_method.toUpperCase()
            }
            // START: Necessary for mock interception
            if(System.getProperty('net.gleske.jervis.SimpleRestService.AddMockHeader') == 'true') {
                conn.setRequestProperty('X-HTTP-Method-Override', http_method)
                conn.setRequestProperty('X-HTTP-Method-Override', null)
                if(binary_data) {
                    conn.setRequestProperty('X-HTTP-Binary-Data', null)
                }
            }
            // END: Necessary for mock interception

            if(conn.getDoOutput()) {
                if(binary_data && httpOutputStream) {
                    conn.getOutputStream().withCloseable { os ->
                        httpOutputStream(os)
                    }
                } else {
                    conn.getOutputStream().withWriter { writer ->
                        writer << data
                    }
                }
            }
            //getHeaderFields will make a network request
            response_headers = conn.getHeaderFields()
            response_code = Integer.parseInt(response_headers[null].toList().first().tokenize(' ')[1])
            // 4xx and 5xx are errors
            response_failure = response_code.toString()[0] in ['4', '5']
            // user requested Binary-Data processing so return binary data
            if(binary_data) {
                return conn
            }
            if(only_response_headers) {
                return
            }
            if(conn.getContentLengthLong() == 0) {
                return
            }
            if(response_failure) {
                ByteArrayOutputStream errorResponse = new ByteArrayOutputStream()
                errorResponse << conn.getErrorStream()
                response_content = errorResponse.toString()
            } else {
                response_content = conn.getContent().getText()
            }
            response_content
        }
        if(binary_data) {
            return response
        }
        if(only_response_headers) {
            return response_headers
        }
        if(return_response_code) {
            return response_code
        }
        Map response_map = [response_code: response_code]
        response_map.content = (parse_json) ? YamlOperator.loadYamlFrom(response_content) : response_content
        response_map.response_headers = response_headers
        if(get_response_map) {
            response_map.error = response_failure
            return response_map
        }
        if(response_failure) {
            throw new FileNotFoundException('\n\n' + YamlOperator.writeObjToYaml(response_map) + '\n\n')
        }
        if(!response) {
            return ''
        }
        if(parse_json) {
            YamlOperator.loadYamlFrom(response)
        }
        else {
            response
        }
    }
}
