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

/**
   REST service classes can derive some default implementation from this trait.
  */
trait SimpleRestServiceSupport {

    /**
      A method for getting the API URL which will be used by
      <tt>{@link #apiFetch(java.lang.String, java.util.Map, java.lang.String, java.lang.String)}</tt>.
      When developing an API client extending <tt>SimpleRestServiceSupport</tt>
      you can call
      <tt>{@link #addTrailingSlash(java.lang.String, java.lang.String)}</tt>
      for convenience.
      */
    abstract String baseUrl()

    /**
      A method for getting authentication headers and other default headers
      used by <tt>{@link #apiFetch(java.lang.String, java.util.Map, java.lang.String, java.lang.String)}</tt>.
      @param original_headers A Map of user-provided headers when making an API
                              call.
      @return A Map of headers added by the class to perform authentication.
      */
    abstract Map header(Map original_headers)

    /**
      A method for converting a HashMap of standard Java types to a JSON String.

      @param obj A Map of standard Java types like Boolean, List, Map, Number,
                 String, and null.
      @return Returns a <tt>String</tt> of JSON content created from the
              <tt>obj</tt> parameter.
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
      This is a convenient method for making API calls to remote REST services.
      It automatically builds out authentication headers by calling other
      methods defined in this trait.

      @param path A path appended to the <tt>{@link #baseUrl()}</tt> for making
                  an API call.
      @param http_headers A list of user or client-provided headers.  These
                          provided headers get passed to
                          <tt>{@link #header(java.util.Map)}</tt>.
      @param http_method The HTTP method to call when making an API request.
                         Non-standard HTTP verbs can also be provided for REST
                         services that support custom verbs.
      @param data Data to be written to the remote service upon API request.
                  This is typically done via HTTP <tt>POST</tt> method but other
                  methods can also accept data written.
      */
    def apiFetch(String path = '', Map http_headers = [:], String http_method = 'GET', String data = '') {
        http_headers = header(http_headers)
        path = path ? (baseUrl() + path) : baseUrl()
        URL api_url = new URL(path)
        SimpleRestService.apiFetch(api_url, http_headers, http_method, data)
    }

    /**
      This method is provided for REST client development convenience.  This
      calls
      <tt>{@link #apiFetch(java.lang.String, java.util.Map, java.lang.String, java.lang.String)}</tt>
      for JSON-based APIs.

      @param path A path appended to the <tt>{@link #baseUrl()}</tt> for making
                  an API call.
      @param http_headers A list of user or client-provided headers.  These
                          provided headers get passed to
                          <tt>{@link #header(java.util.Map)}</tt>.
      @param http_method The HTTP method to call when making an API request.
                         Non-standard HTTP verbs can also be provided for REST
                         services that support custom verbs.
      @param data Assuming a Map consisting of standard Java types, performs
                  <tt>{@link #objToJson(java.util.Map)}</tt> on the
                  <tt>data</tt> parameter.  Submits JSON data to the remote API.
      */
    def apiFetch(String path, Map http_headers, String http_method, Map data) {
        apiFetch(path, http_headers, http_method, SimpleRestService.objToJson(data))
    }
}
