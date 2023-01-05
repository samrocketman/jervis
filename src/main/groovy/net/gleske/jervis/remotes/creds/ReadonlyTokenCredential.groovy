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
package net.gleske.jervis.remotes.creds

import net.gleske.jervis.remotes.interfaces.TokenCredential

/**
  A read only credential interface which partially implements
  <tt>{@link net.gleske.jervis.remotes.interfaces.TokenCredential}</tt>.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html"
   target="_blank">Groovy Console</a> with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.remotes.creds.ReadonlyTokenCredential
import net.gleske.jervis.remotes.interfaces.TokenCredential

class MyCredential implements ReadonlyTokenCredential {
    String secret = 'super secret'
    String getToken() {
        this.secret
    }
}

// in this example creds.setToken('') is a no-op
def creds = new MyCredential()

println "TokenCredential instance? ${creds instanceof TokenCredential}"
println "Secret token: ${creds.token}"
creds.setToken( 'foo')
println creds.getToken()</code></pre><br>
  */
trait ReadonlyTokenCredential implements TokenCredential {

    /**
       When implemented, this method should return a String that is a token to
       be used in API service authentication in Jervis remotes.

       @return A token used for authentication.
      */
    abstract String getToken()

    /**
       Implemented to do nothing.  If a setToken is called then it will be
       discarded.

       @param token Does not matter because it will be discarded.
      */
    @Override
    void setToken(String token) {
        // does nothing
    }
}
