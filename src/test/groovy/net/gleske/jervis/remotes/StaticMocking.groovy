/*
   Copyright 2014-2021 Sam Gleske - https://github.com/samrocketman/jervis

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
            mockedUrl = url
            def constructor = delegate.getConstructor([String] as Class[])
            constructor.newInstance(url)
        }
        mc.newReader = {
            // create a file from the URL including the domain and path with
            // all special characters and path separators replaced with an
            // underscore
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
                getContent: { ->
                    [
                        getText: { ->
                            String checksum = ''
                            if(checksumMocks) {
                                checksum = checksumString(request_meta['data'].toString(), checksumAlgorithm)
                            }
                            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_') + ((checksum) ? '_' + checksum : '')
                            try {
                                Map temp_request_meta = request_meta.clone()
                                temp_request_meta['response'] = new File("src/test/resources/mocks/${file}").text
                                temp_request_meta['url'] = mockedUrl
                                request_history << temp_request_meta
                                return temp_request_meta['response']
                            }
                            catch(Exception e) {
                                throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
                            }
                        }
                    ]
                }
            ]
        }
    }
}
