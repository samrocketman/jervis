/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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

/**
  A simple class which makes using REST services like the GitHub API really
  easy.  This utilizes helpful features of Groovy while not relying on any
  heavier external libraries for HTTP communication.
  */
class SimpleRestService {
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
      </dl>

      @param api_url A URL of a REST endpoint in which to make an HTTP call.
      @param http_headers HTTP headers to pass as part of the HTTP request.  By
                          default only <tt>Content-Type: application/json</tt>
                          HTTP header will be set.
      @param http_method The HTTP method or action to request from the server.
                         Currently supported methods include: GET, POST, PUT,
                         DELETE, and PATCH.

      @return If <tt>Content-Type</tt> HTTP header is
              <tt>application/json</tt>, then one of two types will be
              returned: <tt>HashMap</tt> or <tt>ArrayList</tt> (for JSON Object
              and JSON Array respectively).  If any other <tt>Content-Type</tt>
              HTTP header used then a <tt>String</tt> will be returned and it
              will be left up to the caller to parse the result.
      */
    static def apiFetch(URL api_url, Map http_headers = [:], String http_method = 'GET', String data = '') {
        http_headers['Content-Type'] = http_headers['Content-Type'] ?: 'application/json'
        Boolean parse_json = http_headers['Content-Type'] == 'application/json'
        parse_json = net.gleske.jervis.lang.LifecycleGenerator.getObjectValue(http_headers, 'Parse-JSON', parse_json)
        Boolean response_code = (http_method == 'DELETE')
        response_code = net.gleske.jervis.lang.LifecycleGenerator.getObjectValue(http_headers, 'Response-Code', response_code)
        Boolean only_response_headers = net.gleske.jervis.lang.LifecycleGenerator.getObjectValue(http_headers, 'Response-Headers', response_code)
        if(http_method == 'HEAD') {
            only_response_headers = true
        }

        Map response_headers = [:]
        //data_response could be either a List or Map depending on the JSON
        String response = api_url.openConnection().with { conn ->
            if(http_method.toUpperCase() != 'GET' && data.size()) {
                conn.setDoOutput(true)
            }
            http_headers.each { k, v ->
                if(k in ['Parse-JSON', 'Response-Code', 'Response-Headers']) {
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
            conn.@method = http_method.toUpperCase()
            if(conn.getDoOutput()) {
                conn.getOutputStream().withWriter { writer ->
                    writer << data
                }
            }
            response_headers = conn.getHeaderFields()
            if(only_response_headers) {
                return
            }
            if(response_headers[null].toList().first().toLowerCase().contains('no content')) {
                return
            }
            conn.getContent().with {
                if(conn.getContentLengthLong()) {
                    return it.getText()
                }
                ''
            }
        }
        if(response_code) {
            return Integer.parseInt(response_headers[null].toList().first().tokenize(' ')[1])
        }
        if(only_response_headers) {
            return response_headers
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
