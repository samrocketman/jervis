/*
   Copyright 2014-2020 Sam Gleske - https://github.com/samrocketman/jervis

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


import net.gleske.jervis.exceptions.JervisException
import org.yaml.snakeyaml.Yaml

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
        def yaml = new Yaml()

        //data_response could be either a List or Map depending on the JSON
        def data_response
        String response = api_url.openConnection().with { conn ->
            conn.doOutput = true
            conn.setRequestMethod(http_method.toUpperCase())
            http_headers.each { k, v ->
                conn.setRequestProperty(k, v)
            }
            if(http_method.toUpperCase() != 'GET') {
                conn.outputStream.withWriter { writer ->
                    writer << data
                }
            }
            conn.getContent().getText()
        }
        data_response = (parse_json)? yaml.load(response ?: '{}') : response
        data_response
    }
}
