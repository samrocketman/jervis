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
    static def mockStaticUrl(String mockedUrl, Class<URL> clazz, Map request_meta = [:], Boolean checksumMocks = false, String checksumAlgorithm = 'SHA-256', List request_history = []) {
        def mc = clazz.metaClass
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
        mc.newReader = {
            // Create a file from the URL including the domain and path with
            // all special characters and path separators replaced with an
            // underscore.
            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_')
            try {
                return new File("src/test/resources/mocks/${file}").newReader()
            }
            catch(Exception e) {
                throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
            }
        }
        mc.newReader = { Map parameters ->
            // create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_')
            try {
                return new File("src/test/resources/mocks/${file}").newReader()
            }
            catch(Exception e) {
                throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
            }
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
                setRequestMethod: { String method ->
                    request_meta['method'] = method
                    null
                },
                setRequestProperty: { String key, String value ->
                    if(!request_meta['headers']) {
                        request_meta['headers'] = [:]
                    }
                    request_meta['headers'][key] = value
                    null
                },
                outputStream: request_meta['data'],
                getContentLengthLong: {->
                    String checksum = ''
                    if(checksumMocks) {
                        checksum = checksumString(request_meta['data'].toString(), checksumAlgorithm)
                    }
                    String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_') + ((checksum) ? '_' + checksum : '')
                    1
                    new File("src/test/resources/mocks/${file}").text.size()
                },
                getContent: { ->
                    String checksum = ''
                    if(checksumMocks) {
                        checksum = checksumString(request_meta['data'].toString(), checksumAlgorithm)
                    }
                    String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_') + ((checksum) ? '_' + checksum : '')
                    File responseFile = new File("src/test/resources/mocks/${file}")
                    if(!responseFile.exists()) {
                        throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
                    }
                    [
                        getText: { ->
                            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                            Map temp_request_meta = request_meta.clone()
                            temp_request_meta['response'] = responseFile.text
                            temp_request_meta['url'] = mockedUrl
                            request_history << temp_request_meta
                            return temp_request_meta['response']
                        }
                    ]
                }
            ]
        }
    }
    /**
      Intercepts java.lang.URL calls for opening network connections with
      <tt>{@link net.gleske.jervis.remotes.SimpleRestServiceSupport}</tt> classes.

      <p>This is very useful for recording mocks while writing mocked tests for
      API classes communicating with real services.</p>

      <p>See also:
          https://blog.mrhaki.com/2009/12/groovy-goodness-override-and-use-old_21.html

<h2>Sample Usage</h2>

<pre><tt>import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
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
request_history</tt></pre>
      */
    static def recordMockUrls(String mockedUrl, Class<URL> clazz, Map request_meta = [:], Boolean checksumMocks = false, String checksumAlgorithm = 'SHA-256', List request_history = []) {
        def mc = clazz.metaClass
        if('jervisMocked' in mc.methods*.name.sort().unique()) {
            return
        }
        def oldInvokeMethod = mc.invokeMethod
        def oldGetProperty = mc.getProperty
        // preserve old methods for calling later while overriding them
        def savedOpenConnection = mc.getMetaMethod('openConnection', [] as Class[])

        mc.jervisMocked = {->}
        mc.invokeMethod = { String name, args ->
            mc.getMetaMethod(name, args).invoke(delegate, args)
        }
        mc.getProperty = {String name  ->
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
            // return URLConnection Class-like object
            [
                setDoOutput: { Boolean val ->
                    request_meta.conn.setDoOutput(val)
                    /*
                    if(val) {
                        request_meta['data'] = new StringWriter(request_meta.conn.outputStream)
                    }*/
                },
                getContentLengthLong: {->
                    request_meta.conn.getContentLengthLong()
                },
                getDoOutput: { ->
                    request_meta.conn.getDoOutput()
                },
                setRequestMethod: { String method ->
                    request_meta.conn.setRequestMethod(method)
                    request_meta['method'] = method
                    null
                },
                setRequestProperty: { String key, String value ->
                    request_meta.conn.setRequestProperty(key, value)
                    if(!request_meta['headers']) {
                        request_meta['headers'] = [:]
                    }
                    request_meta['headers'][key] = value
                    null
                },
                outputStream: request_meta['data'],
                getContent: { ->
                    [
                        getText: { ->
                            String checksum = ''
                            if(checksumMocks) {
                                checksum = checksumString(request_meta['data'].toString(), checksumAlgorithm)
                            }
                            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_') + ((checksum) ? '_' + checksum : '')
                            println(request_meta)
                            request_meta.data = request_meta.data.toString()
                            new File("src/test/resources/mocks/${file}").withWriter('UTF-8') { Writer w ->
                                if(request_meta.method != 'GET' && request_meta.data.size()) {
                                    request_meta.conn.getOutputStream().withWriter { writer ->
                                        writer << request_meta.data
                                    }
                                }
                                println(request_meta.conn.getRequestProperty('X-Vault-Token'))
                                w << request_meta.conn.content.text
                            }
                            Map temp_request_meta = request_meta.clone()
                            temp_request_meta.remove('conn')
                            temp_request_meta['response'] = new File("src/test/resources/mocks/${file}").text
                            temp_request_meta['url'] = mockedUrl
                            request_history << temp_request_meta
                            return temp_request_meta['response']
                        }
                    ]
                }
            ]
        }
    }
}
