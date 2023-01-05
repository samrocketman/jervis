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
  A basic credential whose only purpose is to get a Role ID and Secret ID for
  <a href="https://developer.hashicorp.com/vault/docs/auth/approle" target=_blank>HashiCorp Vault AppRole</a>
  authentication.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential


// A file-based backend where the file has 'role_id:secret_id' as its content.
VaultRoleIdCredential roleid = [
    getRole_id: {-> new File('somefile').text.trim().tokenize(':')[0]},
    getSecret_id: {-> new File('somefile').text.trim().tokenize(':')[1]}
] as VaultRoleIdCredential
</pre></code>

  <p>Let's say you have a need to not store the role ID and secret ID in Java
  memory except for when it is used.  The following is a hypothetical example.
  A third example will be provided as a real world example.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

class FileRoleId implements VaultRoleIdCredential {
    final File file
    private void validateFile() {
        if(!this.file.exists()) {
            throw new java.io.FileNotFoundException("${path} does not exist.")
        }
        this.file.text.trim().with { String contents ->
            if(!(contents.count(':') == 1)) {
                throw new IOException("${this.file} must contain role_id:secret_id as its contents.")
            }
        }
    }
    FileRoleId(String path) {
        this.file = new File(path)
        validateFile()
    }
    String getRole_id() {
        this.file.text.trim().tokenize(':')[0]
    }
    String getSecret_id() {
        this.file.text.trim().tokenize(':')[1]
    }
}

// A file-based backend where the file has 'role_id:secret_id' as its content.
VaultRoleIdCredential roleid = new FileRoleId('somefile')
</code></pre>

  <p>Secrets might be stored elsewhere like AWS secrets manager or in the case
  of Jenkins, a Jenkins credential store.  It is beneficial to rely on the
  secure store to keep the credential secured.  In this case, we only want to
  retrieve the credential when we need it and not actually store it.</p>

<pre><code>
import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import jenkins.model.Jenkins
import hudson.model.Item

class JenkinsRoleIdCredential implements VaultRoleIdCredential {
    private final Item owner
    private final String credentialsId
    private StandardUsernamePasswordCredentials getCredential() {
        CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials, this.owner, Jenkins.instance.ACL.SYSTEM).find {
            it.id == this.credentialsId
        }
    }
    private void validate() {
        if(this.getCredential() == null) {
            throw new IOException("Jenkins username and password credential ID '${credentialsId}' does not exist.")
        }
    }
    JenkinsRoleIdCredential(Item owner, String credentialsId) {
        this.owner = owner
        this.credentialsId = credentialsId
        validate()
    }
    String getRole_id() {
        this.getCredential().getUsername()
    }
    String getSecret_id() {
        this.getCredential().getPassword()?.getPlainText()
    }
}

// A Jenkins credentials backend where the credential has role ID as its user and secret ID as its password.
VaultRoleIdCredential roleid = new JenkinsRoleIdCredential(Jenkins.instance, 'some-credential-id')
</code></pre>

  <p>Given the above Jenkins credential, you can initialize <tt>{@link net.gleske.jervis.remotes.VaultService}</tt>
  communication by setting up the client with the secured <tt>{@link net.gleske.jervis.remotes.creds.VaultAppRoleCredential}</tt>
  authentication.</p>

<pre><code>
import net.gleske.jervis.remotes.VaultService
import net.gleske.jervis.remotes.creds.VaultAppRoleCredential
import jenkins.model.Jenkins
// import JenkinsRoleIdCredential

JenkinsRoleIdCredential roleid = new JenkinsRoleIdCredential(Jenkins.instance, 'some-credential-id')
VaultAppRoleCredential approle = new VaultAppRoleCredential('https://vault.example.com', roleid)
VaultService vault = new VaultService(approle)

// set secrets mount versions
vault.mountVersions = [kv: 2]

// ready to perform secrets operations
vault.getSecret('kv/path/to/secret')
</pre></code>

  <p>The above example will use approle to issue ephemeral vault tokens which
  automatically rotate as the client is making requests.</p>
  */
interface VaultRoleIdCredential extends JervisCredential {
    /**
      When implemented, this method should return a role ID used for
      authenticating with Vault.
      */
    String getRole_id()
    /**
      When implemented, this method should return a secret ID used for
      authenticating with Vault.
      */
    String getSecret_id()
}
