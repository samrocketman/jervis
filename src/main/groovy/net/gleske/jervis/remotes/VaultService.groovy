/*
   Copyright 2014-2020 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.interfaces.VaultCredential

// TODO: java doc
    // TODO document recommended setup and usage (setting up recommended
    //   policy, AppId, use batch token, manually set mountVersions to cut down
    //   on API calls).  Add a bullet point list of useful methods.  Document
    //   usage of the method within the method itself.
    // TODO document minimum required role for full functionality
    // TODO document reducing the role and code changes required (e.g. using
    //   mountVersions or a batch token instead of a service token)
/**
  Provides easy access to HashiCorp Vault Key-Value secrets engine.  Both KV v1
  and KV v2 secrets engines are supported.

  <h2>Recommended setup and usage</h2>
  <ul>
    <li>
      Use <a href="https://www.vaultproject.io/docs/auth/approle" target="_blank">AppRole</a>
      for authenticating. This library provides easy integration via automatic
      token renewal and token management transparent to the user.  This is done
      when using the <tt>{@link net.gleske.jervis.remotes.creds.VaultAppRoleCredential}</tt>
      for authenticating.
    </li>
    <li>
      Use short lived <a href="https://learn.hashicorp.com/tutorials/vault/tokens" target="_blank"><tt>batch</tt> tokens</a>
      with a 60s TTL.  When following recommended classes token re-issue and
      TTL monitoring will be handled automatically.
    </li>
    <li>
        Manually set <tt>{@link #mountVersions}</tt> so that API calls are
        reduced when copying secrets between Key-Value mounts.  Otherwise, your
        role will need to grant read permissions to the mount tune in order to
        detect if it is KV v1 or KV v2 secrets engine.
    </li>
    <li>
      Limit the role granted to the AppRole <tt>role_id</tt> to only the permissions necessary.
    </li>
  </ul>

  <h2>Authenticating with Vault</h2>
  <p>The recommended way to authenticate with Vault is to use AppRole
  authentication.  Token-based authentication is possible but not recommended.
  This section will discuss both AppRole and Token-based authentication.</p>

  <h4>AppRole Authentication</h4>
<pre><tt>import net.gleske.jervis.remotes.creds.VaultAppRoleCredential
import net.gleske.jervis.remotes.VaultService

VaultAppRoleCredential creds = new VaultAppRoleCredential('http://active.vault.service.consul:8200/', 'app-roll-id', 'secret-id')
VaultService vault = new VaultService(creds)

// read a secret
vault.getSecret('path/to/secret')
</tt></pre>

  <h4>Token Authentication</h4>
  <p>Authenticating with Vault using a Token is pretty basic.  There's no
  built-in support in this library because in general there's no need for it.
  Instead, the recommended method for authentication is AppRole.   However, if
  you must use a Vault Token, then this example describes a basic method.  This
  example uses a basic static token and will not automatically renew the token
  like AppRole support.</p>
<pre><tt>import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

// 's.fuFc...' is a vault token in this example
TokenCredential creds = [getToken: {-> 's.fuFc...' }] as TokenCredential
VaultService vault = new VaultService('http://active.vault.service.consul:8200/', creds)

// get a secret using the basic Vault Token
vault.getSecret('path/to/secret')</tt></pre>
  */
class VaultService implements SimpleRestServiceSupport {
    private final String vault_url
    private final TokenCredential credential

    // TODO: java doc
    /**
      This property tracks whether a mount is KV v1 or KV v2 secrets engine.
      This only gets discovered once by API and does not change during the
      lifetime of the object.  If a version is not manually set, then the mount
      version is detected by calling
      <a href="https://www.vaultproject.io/api-docs/system/mounts#read-mount-configuration" target="_blank"><tt>/sys/mounts/:path/tune</tt> API</a>
      where <tt>:path</tt> is the name of the mount used for the KV secrets
      engine.

      <p>If you choose not to manually set the mount version, then you'll need
      the following
      <a href="https://www.vaultproject.io/docs/concepts/policies#policy-syntax" target="_blank">Vault policy ACL</a>.</p>
<pre><tt># Read mount config to detect KV secrets engine version
path "sys/mounts/+/tune" {
    capabilities = ["read"]
}</tt></pre>

      <h5>Manually setting mount version</h5>

      <p>Recommendation: manually setting the mount version avoids making an
      API call to read the mount configuration.  If the application using this
      library creates several instances of <tt>VaultService</tt>, then manually
      setting the KV engine version will significantly save on API calls to
      Vault.</p>

      <p>The default mount points for KV secrets engines are the following.</p>

      <ul>
        <li style="list-style: circle outside none"><tt>secret/</tt> for KV v1.</li>
        <li style="list-style: circle outside none"><tt>kv/</tt> for KV v2.</li>
      </ul>

      <h5>Code Example</h5>
<pre><tt>import net.gleske.jervis.remotes.VaultService
import net.gleske.jervis.remotes.creds.VaultAppRoleCredential

VaultAppRoleCredential cred = new VaultAppRoleCredential('http://active.vault.service.consul:8200/', 'app-id', 'secret-id')
VaultService vault = new VaultService(cred)

// add a single entry to mountVersions for secret being KV v1
vault.setMountVersions('secret', '1')
vault.setMountVersions('kv', '2')

// alternately
Map versions = [secret: '1', kv: '2']
vault.setMountVersions(versions)
vault.mountVersions = versions</tt></pre>
      */
    Map mountVersions = [:]

    VaultService(String vault_url, TokenCredential credential) {
        this.vault_url = addTrailingSlash(vault_url)
        if(!this.vault_url.endsWith('v1/')) {
            this.vault_url += 'v1/'
        }
        this.credential = credential
    }
    VaultService(VaultCredential credential) {
        this(credential.vault_url, credential)
    }

    // TODO: java doc
    String baseUrl() {
        this.vault_url
    }

    // TODO: java doc
    Map header(Map headers = [:]) {
        headers['X-Vault-Token'] = credential.getToken()
        headers
    }

    /**
       Get secret from a KV v1 or v2 secret engine.

       TODO better java doc
      */
    Map getSecret(String path, Integer version = 0) {
        String mount = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        if(isKeyValueV2(mount)) {
            apiFetch("${mount}/data/${subpath}?version=${version}")?.data?.data
        }
        else {
            apiFetch(path)?.data
        }
    }

    // TODO: java doc
    void setSecret(String path, Map secret, Boolean enableCas = false) {
        String mount = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        if(isKeyValueV2(mount)) {
            Map secretMeta = [:]
            try {
                secretMeta = apiFetch("${mount}/metadata/${subpath}")
            } catch(IOException e) {}
            if(secretMeta?.data?.cas_required) {
                enableCas = true
            }
            Map data = [data: secret]
            if(enableCas) {
                data['options'] = [cas: (secretMeta?.data?.current_version ?: 0)]
            }
            apiFetch("${mount}/data/${subpath}", [:], 'POST', objToJson(data))
        }
        else {
            apiFetch(path, [:], 'POST', objToJson(secret))
        }
    }

    // TODO: java doc
    void setMountVersions(String mount, def version) {
        if(!(version in ['1', '2'])) {
            throw new JervisException('Error: Vault key-value mounts can only be version "1" or "2" (String).')
        }
        this.mountVersions[mount] = version
    }

    // TODO: java doc
    void setMountVersions(Map mountVersions) {
        mountVersions.each { k, v ->
            this.setMountVersions(k, v)
        }
    }

    /**
      Checks version of a Key-Value engine mount.  Returns true if Key-Value v2
      or false if Key-Value v2.
       TODO better java doc
      */
    private Boolean isKeyValueV2(String mount) {
        discoverMountVersion(mount)
        this.mountVersions[mount] == '2'
    }

    /**
      If the mount is not already in mountVersions, then this will inspect the
      mount and set whether the mount is a Key-Value secret engine v1 or v2.

      TODO better java doc
      */
    private void discoverMountVersion(String mount) {
        if(mount in this.mountVersions) {
            return
        }
        setMountVersions(mount, apiFetch("sys/mounts/${mount}/tune")?.options?.version)
    }

    // TODO: java doc
    List listPath(String path) {
        String mount = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        subpath = addTrailingSlash(subpath)

        if(isKeyValueV2(mount)) {
            apiFetch("${mount}/metadata/${subpath}?list=true")?.data?.keys
        }
        else {
            // KV v1 API call here
            apiFetch("${mount}/${subpath}?list=true")?.data?.keys
        }
    }

    /**
      Internal method used by findAllKeys() which tracks recursion level in
      addition to the user-passed settings.

      TODO better java doc
      */
    private List recursiveFindAllKeys(String path, Integer desiredLevel, Integer level) {
        if(desiredLevel > 0 && level > desiredLevel) {
            return []
        }
        List entries = listPath(path).collect { String key ->
            if(key.endsWith('/')) {
                recursiveFindAllKeys(path + key, desiredLevel, level + 1)
            }
            else {
                [path + key]
            }
        }

        if(entries) {
            entries.sum()
        }
        else {
            []
        }
    }

    /**
      Recursively traverses the path for subkeys.  If level is 0 then there's
      no depth limit.  When level = n, keys are traversed up to the limit.

      TODO better java doc
      */
    List findAllKeys(String path, Integer level = 0) {
        recursiveFindAllKeys(path, level, 1)
    }

    /**
      Copies the contents of secret from a source key, srcKey, to the
      destination key, destKey.  Optional argument srcVersion allows you to
      select a specific version from a Key-Value v2 engine (default is get
      latest version of secret).

      TODO better java doc
      */
    void copySecret(String srcKey, String destKey, Integer srcVersion = 0) {
        setSecret(destKey, getSecret(srcKey, srcVersion), true)
    }

    /**
      Recursively copies all secrets from the source path, srcPath, to the
      destination path, destPath.

      TODO better java doc
      */
    void copyAllKeys(String srcPath, destPath, Integer level = 0) {
        findAllKeys(srcPath, level).each { String srcKey ->
            String destKey = destPath + (srcKey -~ "^\\Q${srcPath}\\E")
            copySecret(srcKey, destKey)
        }
    }

    /**
      Returns a Map of key-value pairs compatible with bash environment
      variables.  The key and value returned in the Map will always be type String.

      TODO better java doc
      */
    Map getEnvironmentSecret(String path, Integer version = 0, Boolean allowInvalidKeys = false) {
        getSecret(path, version).findAll { k, v ->
            k in String &&
            (k ==~ '^[a-zA-Z0-9_]+$' || allowInvalidKeys) && (
                v in String ||
                v in Boolean ||
                v in Number
            )
        }.collect { k, v ->
            [(k): v.toString()]
        }.sum() ?: [:]
    }

    /**
      Returns a Map of key-value pairs compatible with bash environment
      variables.  Given a list of paths, they'll be combined with the end of
      the list taking precedence over the beginning of  the list.  When
      combining the Maps, the last Key-Value pair wins when key names conflict.

      @param paths            A List of paths to search Vault for Maps.  Maps
                              are eventually combined.
      @param allowInvalidKeys Includes keys which have invalid bash variable
                              names.  This might be useful for additional logic
                              processing.
      @return Returns a Map which is the sum of all of the keys provided.

      TODO better java doc
      */
    Map getEnvironmentSecrets(List paths, Boolean allowInvalidKeys = false) {
        paths.collect { String path ->
            try {
                getEnvironmentSecret(path, 0, allowInvalidKeys)
            } catch(FileNotFoundException e) {
                [:]
            }
        }.sum()
    }

    // TODO implement DELETE key
    // TODO implement recursive DELETE path
    // TODO implement combining path of keys into a List of Maps?  This might not be useful.
    // TODO organize private methods to bottom of class
    // TODO add Vault exception higherarchy?
    // TODO getSecretsAsList should return a combined list matching the order of the input keys.
    // TODO getSecretsAsMap should return a Key-Value Map where the key is the secret key and value is contents of the secret.
}
