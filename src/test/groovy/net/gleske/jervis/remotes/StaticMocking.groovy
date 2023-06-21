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

import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest

/**
  A helper class for tests which makes it easy to statically mock other
  internal classes.
 */
class StaticMocking {

    /**
      Throw an error if a user attempts to instantiate an instance of <tt>StaticMocking</tt>.
      */
    private StaticMocking() {
        throw new IllegalStateException("This utility class is only meant for referencing static methods.  This is not meant to be instantiated.")
    }

    /**
      Checksum String data for use in static mocking of GraphQL payloads.
      Valid checksum algorithms:
        MD2
        MD5
        SHA-1
        SHA-256
        SHA-384
        SHA-512
      */
    static String checksumString(String contents, String type = 'SHA-256') {
        def buffer = new byte[16384]
        def len
        def digest = MessageDigest.getInstance(type)
        InputStream targetStream = new ByteArrayInputStream(contents.getBytes())
        while((len=targetStream.read(buffer)) > 0) {
            digest.update(buffer, 0, len)
        }
        digest.digest().encodeHex().toString()
    }

    static String urlToMockFileName(String mockedUrl, String data = '', Boolean checksumMocks = false, String checksumAlgorithm = 'SHA-256') {
        String checksum
        if(checksumMocks) {
            checksum = checksumString(data, checksumAlgorithm)
        }
        mockedUrl.toString().replaceAll(/[:&?=]/,'_').split('/')[2..-1].join('_') + ((checksum) ? '_' + checksum : '')
    }

    /**
      Mock the HTTP calls to any API and use resources files, instead.  Under
      the hood, URL will utilize a file reader rather than attempting to
      connect to the internet.  This utilizes a Groovy feature known as meta
      class hacking.  This is similar to monkey patching as known in other
      languages.

      See also:
        http://flyingtomoon.com/tag/mocking/
        http://groovy.329449.n5.nabble.com/Groovy-metaclass-invokeConstructor-td5716360.html
     */
    static def mockStaticUrl(String mockedUrl, Class<URL> clazz,
            Map request_meta = [:], Boolean checksumMocks = false,
            String checksumAlgorithm = 'SHA-256', List request_history = [],
            Map custom_responses = [:]) {
        def mc = clazz.getMetaClass()
        mc.invokeMethod = { String name, args ->
            // choose mock or fall back to class default
            if(name != 'getTheClass' && delegate.getTheClass() in clazz) {
                mc.getMetaMethod(name, args)?.invoke(delegate, args)
            }
            else {
                // call the real method instead of the mocked meta method
                mc.getMethods().find {
                    it.name == name && it.isValidMethod(args.toList()*.class as Class[])
                }?.invoke(delegate, args)
            }
        }
        mc.getProperty = { String name  ->
            mc.getMetaProperty(name)?.getProperty(delegate) ?:
                mc.getProperty(delegate, name)
        }
        mc.constructor = { String url ->
            request_meta['url'] = url
            mockedUrl = url
            def constructor = delegate.getConstructor([String] as Class[])
            constructor.newInstance(url)
        }
        mc.openConnection = { ->
            request_meta['data'] = new StringWriter()
            [
                setDoOutput: { Boolean val ->
                    request_meta['doOutput'] = val
                },
                getDoOutput: { ->
                    request_meta['doOutput']
                },
                getHeaderFields: { ->
                    request_meta.data = request_meta.data.toString() ?: ''
                    Map header_fields = [(null): Collections.unmodifiableList(['HTTP/1.1 200 OK'])]
                    String file = urlToMockFileName(mockedUrl, [request_meta.method, request_meta.data].join(' '), checksumMocks, checksumAlgorithm)
                    if(file in custom_responses.keySet()) {
                        //throw new Exception( custom_responses.get(file) )
                        file = custom_responses.get(file)
                    }
                    File headersFile = new File("src/test/resources/mocks/${file}_headers")
                    if(!headersFile.exists()) {
                        file = urlToMockFileName(mockedUrl, request_meta.data, checksumMocks, checksumAlgorithm)
                        headersFile = new File("src/test/resources/mocks/${file}_headers")
                    }
                    if(headersFile.exists() && !request_meta.response_headers) {
                        request_meta.response_headers = net.gleske.jervis.tools.YamlOperator.loadYamlFrom(headersFile)
                    }
                    if(request_meta.response_headers in Map) {
                        header_fields = [:]
                        request_meta.response_headers.each { k, v ->
                            header_fields.put(k, (v in List) ? Collections.unmodifiableList(v) : v)
                        }
                    }
                    Map response_headers = Collections.unmodifiableMap(header_fields)
                    Map temp_request_meta = request_meta.clone()
                    temp_request_meta['response'] = ''
                    temp_request_meta['url'] = mockedUrl
                    temp_request_meta['response_headers'] = response_headers
                    temp_request_meta['response_code'] = Integer.parseInt(response_headers[null].toList().first().tokenize(' ')[1])
                    temp_request_meta['mock_file'] = "src/test/resources/mocks/${file}".toString()
                    temp_request_meta['mock_header_file'] = "src/test/resources/mocks/${file}_headers".toString()
                    request_history << temp_request_meta
                    response_headers
                },
                setRequestMethod: { String method ->
                    request_meta['method'] = method
                    null
                },
                setRequestProperty: { String key, def value ->
                    if(key == 'X-Mock-Throw-Exception') {
                        throw value
                    }
                    if(key == 'X-HTTP-Method-Override') {
                        if(value) {
                            request_meta['method'] = value
                        }
                        return
                    }
                    if(!request_meta['headers']) {
                        request_meta['headers'] = [:]
                    }
                    request_meta['headers'][key] = value
                    null
                },
                getOutputStream: {->
                    request_meta.data
                },
                getContentLengthLong: {->
                    request_meta.data = request_meta.data.toString() ?: ''
                    String file = urlToMockFileName(mockedUrl, [request_meta.method, request_meta.data].join(' '), checksumMocks, checksumAlgorithm)
                    if(file in custom_responses.keySet()) {
                        file = custom_responses.get(file)
                    }
                    File responseFile = new File("src/test/resources/mocks/${file}")
                    if(!responseFile.exists()) {
                        file = urlToMockFileName(mockedUrl, request_meta.data, checksumMocks, checksumAlgorithm)
                        responseFile = new File("src/test/resources/mocks/${file}")
                    }
                    responseFile.text.trim().size()
                },
                getContent: { ->
                    request_meta.data = request_meta.data.toString() ?: ''
                    // Create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                    String file = urlToMockFileName(mockedUrl, [request_meta.method, request_meta.data].join(' '), checksumMocks, checksumAlgorithm)
                    if(file in custom_responses.keySet()) {
                        file = custom_responses.get(file)
                    }
                    File responseFile = new File("src/test/resources/mocks/${file}")
                    if(!responseFile.exists()) {
                        file = urlToMockFileName(mockedUrl, request_meta.data, checksumMocks, checksumAlgorithm)
                        responseFile = new File("src/test/resources/mocks/${file}")
                    }
                    if(!responseFile.exists()) {
                        throw new IOException("[404] Not Found - src/test/resources/mocks/${file}")
                    }
                    // return content like object
                    [
                        getText: { ->
                            // Load cached response from YAML to a minified JSON
                            // String as an API response.
                            request_history[-1].response = net.gleske.jervis.remotes.SimpleRestService.objToJson(net.gleske.jervis.tools.YamlOperator.loadYamlFrom(responseFile))
                            return request_history[-1].response
                        }
                    ]
                }
            ] as MockURLConnection
        }
    }
    /**
      Intercepts java.lang.URL calls for opening network connections with
      <tt>{@link net.gleske.jervis.remotes.SimpleRestServiceSupport}</tt> classes.

      <p>This is very useful for recording mocks while writing mocked tests for
      API classes communicating with real services.</p>

      <p>See also:
          https://blog.mrhaki.com/2009/12/groovy-goodness-override-and-use-old_21.html

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
import net.gleske.jervis.remotes.SimpleRestServiceSupport

if(!binding.hasVariable('url')) {
    String persistStr
    url = persistStr
}
if(binding.hasVariable('request_meta')) {
    request_meta.clear()
} else {
    request_meta = [:]
}

if(binding.hasVariable('request_history')) {
    request_history.clear()
} else {
    request_history = []
}
//if(URL.class.metaClass.metaMethods*.name.sort().unique().join('\n')
recordMockUrls(url, URL, request_meta, true, 'SHA-256', request_history)

//return URL.metaClass.methods*.name.sort().unique().join('\n')
class MyApi implements SimpleRestServiceSupport {
    String baseUrl() {
        'https://www.example.com'
    }
    Map header(Map http_headers = [:]) {
        http_headers['Content-Type'] = 'text/html'
        http_headers
    }
}

new MyApi().apiFetch('')

// return full request history of the request_meta
request_history
</code></pre>
      */
    static def recordMockUrls(String mockedUrl, Class<URL> clazz, Map request_meta = [:], Boolean checksumMocks = false, String checksumAlgorithm = 'SHA-256', List request_history = []) {
        def mc = clazz.metaClass
        if('jervisMocked' in mc.methods*.name.sort().unique()) {
            return
        }
        // preserve old methods for calling later while overriding them
        def savedOpenConnection = mc.getMetaMethod('openConnection', [] as Class[])

        mc.jervisMocked = {->}
        mc.invokeMethod = { String name, args ->
            mc.getMetaMethod(name, args).invoke(delegate, args)
        }
        mc.getProperty = { String name  ->
            mc.getMetaProperty(name).getProperty(delegate)
        }
        mc.constructor = { String url ->
            request_meta['url'] = url
            mockedUrl = url
            def constructor = delegate.getConstructor([String] as Class[])
            constructor.newInstance(url)
        }
        mc.openConnection = { ->
            request_meta['conn'] = savedOpenConnection.invoke(delegate)
            request_meta['data'] = new StringWriter()
            request_meta['id'] = request_history.size() + 1
            // return URLConnection Class-like object
            [
                setDoOutput: { Boolean val ->
                    request_meta.conn.setDoOutput(val)
                },
                getDoOutput: { ->
                    request_meta.conn.getDoOutput()
                },
                getHeaderFields: { ->
                    request_meta.data = request_meta.data.toString() ?: ''
                    // write output to connection request
                    if(request_meta.conn.getDoOutput()) {
                        request_meta.conn.getOutputStream().withWriter { writer ->
                            writer << request_meta.data
                        }
                    }
                    // Complete the request by getting header fields
                    Map response_headers = request_meta.conn.getHeaderFields()
                    String file = urlToMockFileName(mockedUrl, [request_meta.method, request_meta.data].join(' '), checksumMocks, checksumAlgorithm)
                    File headersFile = new File("src/test/resources/mocks/${file}_headers")
                    net.gleske.jervis.tools.YamlOperator.writeObjToYaml(headersFile, response_headers)
                    Integer response_code = Integer.parseInt(response_headers[null].toList().first().tokenize(' ')[1])
                    Map temp_request_meta = request_meta.clone()
                    temp_request_meta.remove('conn')
                    temp_request_meta['response'] = ''
                    temp_request_meta['url'] = mockedUrl
                    temp_request_meta['response_headers'] = response_headers
                    temp_request_meta['response_code'] = response_code
                    temp_request_meta['mock_file'] = "src/test/resources/mocks/${file}".toString()
                    temp_request_meta['mock_header_file'] = "src/test/resources/mocks/${file}_headers".toString()
                    request_history << temp_request_meta
                    // create an empty file for any API query
                    File responseFile = new File("src/test/resources/mocks/${file}")
                    if(!responseFile.exists()) {
                        responseFile.createNewFile()
                    }
                    response_headers
                },
                setRequestMethod: { String method ->
                    request_meta.conn.setRequestMethod(method)
                    request_meta['method'] = method
                    null
                },
                setRequestProperty: { String key, def value ->
                    if(key == 'X-HTTP-Method-Override') {
                        if(value) {
                            request_meta.conn.@method = value
                            request_meta['method'] = value
                        }
                        return
                    }
                    request_meta.conn.setRequestProperty(key, value)
                    if(!request_meta['headers']) {
                        request_meta['headers'] = [:]
                    }
                    request_meta['headers'][key] = value
                    null
                },
                getOutputStream: {->
                    request_meta.data
                },
                getContentLengthLong: {->
                    request_meta.conn.getContentLengthLong()
                },
                getContent: { ->
                    // finalize writer
                    request_meta.data = request_meta.data.toString()
                    // call for real network content
                    request_meta.conn.content
                    // return content like object
                    [
                        getText: { ->
                            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                            String file = urlToMockFileName(mockedUrl, [request_meta.method, request_meta.data].join(' '), checksumMocks, checksumAlgorithm)
                            File responseFile = new File("src/test/resources/mocks/${file}")
                            StringWriter responseBuffer = new StringWriter()
                            responseBuffer << request_meta.conn.content.text
                            // serialize the response to disk as YAML for a readable version of the response
                            net.gleske.jervis.tools.YamlOperator.writeObjToYaml(
                                responseFile,
                                net.gleske.jervis.tools.YamlOperator.loadYamlFrom(responseBuffer)
                            )
                            request_history[-1].response = responseFile.text
                            return request_history[-1].response
                        }
                    ]
                }
            ] as MockURLConnection
        }
    }
}
