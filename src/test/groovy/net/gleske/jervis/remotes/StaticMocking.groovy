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

import java.net.URL
import java.net.URLConnection

/**
  A helper class for tests which makes it easy to statically mock other
  internal classes.
 */
class StaticMocking {

    /*
      Mock the HTTP calls to any API and use resources files, instead.  Under
      the hood, URL will utilize a file reader rather than attempting to
      connect to the internet.  This utilizes a Groovy feature known as meta
      class hacking.  This is similar to monkey patching as known in other
      languages.

      See also:
        http://flyingtomoon.com/tag/mocking/
        http://groovy.329449.n5.nabble.com/Groovy-metaclass-invokeConstructor-td5716360.html
     */
    static def mockStaticUrl(String mockedUrl, Class<URL> clazz, Map request_meta = [:]) {
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
            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_')
            try {
                URL resource_url = this.getClass().getResource("/mocks/${file}");
                def resource = new File(resource_url.getFile())
                if(resource.isFile()) {
                    return resource.newReader()
                }
                else {
                    throw new RuntimeException("[404] Not Found - test/resources/mocks/${file}")
                }
            }
            catch(Exception e) {
                throw new RuntimeException("[404] Not Found - test/resources/mocks/${file}")
            }
        }
        mc.newReader = { Map parameters ->
            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_')
            try {
                URL resource_url = this.getClass().getResource("/mocks/${file}");
                def resource = new File(resource_url.getFile())
                if(resource.isFile()) {
                    return resource.newReader()
                }
                else {
                    throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
                }
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
                            //create a file from the URL including the domain and path with all special characters and path separators replaced with an underscore
                            String file = mockedUrl.toString().replaceAll(/[:?=]/,'_').split('/')[2..-1].join('_')
                            try {
                                URL resource_url = this.getClass().getResource("/mocks/${file}");
                                def resource = new File(resource_url.getFile())
                                if(resource.isFile()) {
                                    return resource.text
                                }
                                else {
                                    throw new RuntimeException("[404] Not Found - src/test/resources/mocks/${file}")
                                }
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
