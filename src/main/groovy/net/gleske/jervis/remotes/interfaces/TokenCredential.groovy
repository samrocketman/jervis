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
package net.gleske.jervis.remotes.interfaces

/**
   Abstracted token credential allows retrieving a token from an external
   credential store.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html"
   target="_blank">Groovy Console</a> with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.TokenCredential

class MyCredential implements TokenCredential {
    String secret = 'super secret'
    String getToken() {
        this.secret
    }
    void setToken(String s) {
        this.secret = s
    }
}

def creds = new MyCredential()

println "TokenCredential instance? ${creds instanceof TokenCredential}"
println "Secret token: ${creds.token}"
creds.setToken('foo')
println creds.getToken()</code></pre><br>
 */
interface TokenCredential extends JervisCredential {

    /**
       When implemented, this method should return a String that is a token to
       be used in API service authentication in Jervis remotes.

       @return A token used for authentication.
      */
    String getToken();

    /**
       When implemented, this method should set the value of the token in a
       credential service.  Note, this method can be implemented as a no-op.
      */
    void setToken(String token);
}
