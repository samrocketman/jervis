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

/**
   REST service classes can derive some default implementation from this trait.
  */
trait SimpleRestServiceSupport {

    /**
      A method for getting the API URL which will be used by apiFetch(String).
      TODO better java doc
      */
    abstract String baseUrl()

    /**
      A method for getting authentication headers and other default headers
      used by apiFetch(String).
      TODO better java doc
      */
    abstract Map header(Map original_headers)

    /**
      A method for converting a HashMap to a JSON String.
      TODO better java doc
      */
    static String objToJson(Map obj) {
        SimpleRestService.objToJson(obj)
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
        SimpleRestService.addTrailingSlash(url, suffix)
    }


    /**
      A method to simplify fetching from remote REST services.
      TODO better java doc
      */
    def apiFetch(String path = '', Map http_headers = [:], String http_method = 'GET', String data = '') {
        http_headers = header(http_headers)
        path = path ? (baseUrl() + path) : baseUrl()
        URL api_url = new URL(path)
        SimpleRestService.apiFetch(api_url, http_headers, http_method, data)
    }
}
