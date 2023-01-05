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
  Generic vault credential type that provides the minimum necessary
  implementation required by the
  <tt>{@link net.gleske.jervis.remotes.VaultService}</tt> class.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html"
   target="_blank">Groovy Console</a> with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.VaultCredential
import net.gleske.jervis.remotes.VaultService

class VaultToken implements VaultCredential {
    final String vault_url
    String token

    VaultToken(String vault_url, String token) {
        this.vault_url = vault_url
        this.token = token
    }
}

VaultToken creds = new VaultToken('http://active.vault.service.consul:8200/', 's.Aas...')
VaultService vault = new VaultService(creds)

println creds.getToken()
println vault.getSecret('path/to/secret')</code></pre><br>
  */
interface VaultCredential extends TokenCredential {
    /**
      When implemented, this method should return a String that is the URL to
      a HashiCorp Vault instance API endpoint.
      */
    String getVault_url();
}
