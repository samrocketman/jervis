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

trait SimpleRestServiceSupport {

    /**
      A method for getting the API URL which will be used by apiFetch(String).
      */
    abstract String baseUrl()

    /**
      A method for getting authentication headers and other default headers
      used by apiFetch(String).
      */
    abstract Map header(Map original_headers)

    /**
      A method to simplify fetching from remote REST services.
      */
    def apiFetch(String path, Map http_headers = [:], String http_method = 'GET', String data = '') {
        http_headers = header(http_headers)
        URL api_url = new URL(baseUrl() + path)
        SimpleRestService.apiFetch(api_url, http_headers, http_method, data)
    }
}
