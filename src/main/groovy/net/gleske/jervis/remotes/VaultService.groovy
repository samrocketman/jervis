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

import net.gleske.jervis.exceptions.VaultException
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
  Provides easy access to
  <a href="https://www.vaultproject.io/" target="_blank">HashiCorp Vault</a>
  Key-Value secrets engine.  Both KV v1 and KV v2 secrets engines are
  supported.

  <h2>Discovering key-value mounts</h2>

  <p>There are two ways to set up instantiation of this class.  Manually specifying
  key-value mounts or auto-discovering mounts.  In general, auto-discovery is
  more reliable and recommended.  Manually specifying mounts is available to
  reduce API usage.</p>

  <h4>Automatic discover mounts</h4>

<pre><tt>
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

VaultService vault = new VaultService('http://active.vault.service.consul:8200/', creds)

// auto-discover mounts
vault.discoverKVMounts()
</tt></pre>

  <h4>Manuaully declare mounts</h4>

<pre><tt>
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

VaultService vault = new VaultService('http://active.vault.service.consul:8200/', creds)

// specify mounts
vault.mountVersions = [kv: '2', secret: '1']
</tt></pre>

  <h2>Sample usage</h2>
  <p>To run examples, clone Jervis and execute <tt>./gradlew console</tt> to
  bring up a
  <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.  Additionally, you'll need to clone and setup a
  <a href="https://github.com/samrocketman/docker-compose-ha-consul-vault-ui" target="_blank">local Vault cluster</a>.
  After you instantiate the local Vault cluster you'll need to run the
  following Shell commands relative to the root of the repository at
  <tt>~/git/github/docker-compose-ha-consul-vault-ui</tt>.</p>
<pre><tt># Enable secrets engines KV v1 and KV v2
./scripts/curl-api.sh --request POST --data '{"type": "kv", "options": {"version": "1"}}' http://active.vault.service.consul:8200/v1/sys/mounts/secret
./scripts/curl-api.sh --request POST --data '{"type": "kv", "options": {"version": "2"}}' http://active.vault.service.consul:8200/v1/sys/mounts/kv

# Generate an admin token for initial setup
./scripts/get-admin-token.sh</tt></pre>
  <p>Afterwards, run the following Groovy Console script to populate the local
  Vault cluster with dummy secret data.</p>
<pre><tt>System.setProperty("socksProxyHost", "localhost")
System.setProperty("socksProxyPort", "1080")

import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

// generate token by executing ./scripts/get-admin-token.sh in
// docker-compose-ha-consul-vault-ui repo
TokenCredential creds = [getToken: {-> 'admin token' }] as TokenCredential
VaultService vault = new VaultService('http://active.vault.service.consul:8200/', creds)

// populate secret mounts with dummy data
vault.setSecret("kv/foo", ['hello':'world'])
vault.setSecret("kv/foo", ['another':'secret', 'hello':'world'])
vault.setSecret("kv/foo/bar", ['hello':'friend'])
vault.setSecret("kv/foo/bar/baz", ['foo':'bar'])
vault.setSecret("secret/foo", ['test':'data'])
vault.setSecret("secret/foo/bar", ['someother':'data'])
vault.setSecret("secret/foo/bar/baz", ['more':'secrets'])
println 'Success.'</tt></pre>
  <p><b>Please note:</b> If you're practicing against the local Vault cluster,
  then your Groovy Console requires the following lines of code at the top of
  the Groovy script.  It uses the SOCKS proxy provided by the test cluster.</p>
<pre><tt>System.setProperty("socksProxyHost", "localhost")
System.setProperty("socksProxyPort", "1080")</tt></pre>

  <h2>Recommended setup</h2>
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

VaultAppRoleCredential creds = new VaultAppRoleCredential('http://active.vault.service.consul:8200/', 'my-app-role', 'my-secret-id')
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

    private void checkLocationMap(Map location) {
        if(!(('mount' in location.keySet()) || !('path' in location.keySet()))) {
            throw new VaultException('"mount" and "path" must be set when using location Map.')
        }
        if(!(location.mount in String) || !(location.path in String)) {
            throw new VaultException('"mount" and "path" must contain String values when using location Map.')
        }
        if(!(location.mount in this.mountVersions.keySet())) {
            throw new VaultException('Location map contains an invalid mount.  It must be a KV v1 or KV v2 mount in Vault.')
        }
    }

    /**
      Internal method used by
      <tt>{@link #findAllKeys(java.lang.String, java.lang.Integer)}</tt>
      which tracks recursion level in addition to the user-passed settings.

      @param path         A path to recursively search.
      @param desiredLevel A recursive depth level to avoid going beyond to
                          limit recursion.
      @param level        Tracks the current recursive depth level used to
                          compare against desiredLevel.
      @return A list of keys that are paths.
      */
    private List<String> recursiveFindAllKeys(String path, Integer desiredLevel, Integer level) {
        if(desiredLevel > 0 && level > desiredLevel) {
            return []
        }
        List entries = []
        entries = listPath(path).collect { String key ->
            if(key.endsWith('/')) {
                recursiveFindAllKeys(path + key, desiredLevel, level + 1)
            }
            else {
                [path + key]
            }
        }
        entries.sum()
    }

    /**
      Checks version of a Key-Value engine mount.

      @param mount A Vault secrets engine mount point.
      @return <tt>true</tt> if Key-Value v2 or <tt>false</tt> if Key-Value v1.
      */
    private Boolean isKeyValueV2(String mount) {
        this.mountVersions[mount] == '2'
    }

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
    Map<String, String> mountVersions = [:]

    /**
      A list of KV v2 mounts which enforce cas_required for all write
      operations.
      */
    List cas_required = []

    /**
      Customizable HTTP headers which get sent to Vault in addition to
      authentication headers.
      */
    Map<String, String> headers = [:]

    // TODO document constructor
    VaultService(String vault_url, TokenCredential credential) {
        this.vault_url = addTrailingSlash(vault_url)
        if(!this.vault_url.endsWith('v1/')) {
            this.vault_url += 'v1/'
        }
        this.credential = credential
    }
    // TODO document constructor
    VaultService(VaultCredential credential) {
        this(credential.vault_url, credential)
    }

    /**
      Query Vault to find all of the KV mounts.  This can be called immediately
      following the constructure to initiate service connectivity.
      */
    void discoverKVMounts() throws IOException, VaultException {
        apiFetch("sys/mounts").with { Map mounts ->
            mounts.findAll { k, v ->
                v in Map && v.type == 'kv'
            }.each { k, v ->
                setMountVersions(k.replaceAll('/$', ''), v.options.version)
            }
        }
    }


    /**
      Resolves the API base URL to be used by
      <tt>{@link net.gleske.jervis.remotes.SimpleRestService#apiFetch(java.net.URL, java.util.Map, java.lang.String, java.lang.String)}</tt>.
      <tt>SimpleRestService.apiFetch</tt> is used internally for
      <tt>VaultService</tt> communication.

      @return A base API URL for a Vault instance for the <tt>SimpleRestService</tt> support class.
      */
    String baseUrl() {
        this.vault_url
    }

    /**
      Resolves authentication headers to be used by
      <tt>{@link net.gleske.jervis.remotes.SimpleRestService#apiFetch(java.net.URL, java.util.Map, java.lang.String, java.lang.String)}</tt>.
      <tt>SimpleRestService.apiFetch</tt> is used internally for
      <tt>VaultService</tt> communication.

      @return Authentication headers for the <tt>SimpleRestService</tt> support class.
      */
    Map header(Map headers = [:]) {
        Map tempHeaders = this.headers + headers
        tempHeaders['X-Vault-Token'] = credential.getToken()
        // https://www.vaultproject.io/api-docs#the-x-vault-request-header
        tempHeaders['X-Vault-Request'] = "true"
        tempHeaders
    }

    // TODO docs
    // TODO test alternate syntax
    // TODO test exception throwing
    Map getSecret(Map location, Integer version = 0) {
        checkLocationMap(location)
        String mount = location.mount
        String subpath = location.path
        if(isKeyValueV2(mount)) {
            apiFetch("${mount}/data/${subpath}?version=${version}")?.data?.data
        }
        else {
            apiFetch("${mount}/${subpath}")?.data
        }
    }

    /**
       Get secret from a KV v1 or KV v2 secret engine.  This method will
       gracefully handle either KV v1 or KV v2.

       @param path    A path to a secret JSON object to read from Vault.
       @param version Request a specific version of a secret.  If <tt>0</tt>,
                      then the latest version is returned.  This option is
                      ignored for KV v1 secrets engine.
       @return Parsed JSON object content from a secret <tt>path</tt>.  If KV
               v2 secrets engine and <tt>version</tt> was customized, then the
               secret at that version is returned (if it exists).
      */
    // TODO test getting mount with a slash in its name
    Map getSecret(String path, Integer version = 0) {
        getSecret(getLocationMapFromPath(path), version)
    }

    // TODO test mounts which contain a slash... because Vault allows that
    // TODO test exception throwing
    void setSecret(Map location, Map secret, Boolean enableCas = false) {
        checkLocationMap(location)
        String mount = location.mount
        String subpath = location.path
        if(isKeyValueV2(mount)) {
            Map data = [data: secret]
            Map secretMeta = [:]
            if(mount in cas_required) {
                enableCas = true
            }
            if(enableCas) {
                try {
                    secretMeta = apiFetch("${mount}/metadata/${subpath}") ?: [:]
                } catch(IOException ignore) {
                    // 40X exceptions for invalid paths ignored
                }
                data['options'] = [cas: (secretMeta?.data?.current_version ?: 0)]
            }
            apiFetch("${mount}/data/${subpath}", [:], 'POST', objToJson(data))
        }
        else {
            apiFetch("${mount}/${subpath}", [:], 'POST', objToJson(secret))
        }
    }

    /**
      Writes a secret to Vault KV v1 or KV v2 secrets engine.  This method will
      gracefully handle either KV v1 or KV v2.  It is recommend to set
      <tt>enableCas=true</tt> when writing to Vault, because it will only write
      if the destination <tt>path</tt> is in an expected state.

      @param path      The destination to write the <tt>secret</tt>.
      @param secret    A Map converted to a JSON Object written to the Vault
                       <tt>path</tt>.
      @param enableCas If enabled, a
                       <a href="https://learn.hashicorp.com/tutorials/vault/versioned-kv#step-8-check-and-set-operations" target="_blank">Check-and-Set operation</a>
                       is performed when writing to Vault.
      */
    // TODO write tests
    void setSecret(String path, Map secret, Boolean enableCas = false) {
        setSecret(getLocationMapFromPath(path), secret, enableCas)
    }

    /**
      Forces a specified secrets engine <tt>mount</tt> to be KV v1 or KV v2.
      See <tt>{@link #mountVersions}</tt> for additional usage.

      @param mount   A Vault secrets engine mount.
      @param version Must be <tt>"1"</tt> or <tt>"2"</tt> to denote KV v1 or KV
                     v2 secrets engine.  Any type is allowed to catch invalid
                     version setting.
      */
    // TODO write tests
    // TODO test manual mounts that have a slash in its name
    void setMountVersions(String mount, def version) {
        if(!(version in ['1', '2', 1, 2])) {
            throw new VaultException('Vault key-value mounts can only be version "1" or "2".')
        }
        this.mountVersions[mount] = version.toString()
        if(mountVersions[mount] == '2') {
            Boolean isCasRequired = apiFetch(mount + '/config').data.cas_required
            if(isCasRequired) {
                cas_required << mount
            }
        }
    }

    /**
      Forces a specified secrets engine mount to be KV v1 or KV v2.  See
      <tt>{@link #mountVersions}</tt> for additional usage.

      @param mountVersions A Key-Value map containing multiple Vault mounts and
                           respective version numbers.
      */
    // TODO write tests
    void setMountVersions(Map mountVersions) {
        mountVersions.each { k, v ->
            this.setMountVersions(k, v)
        }
    }

    /**
      Given a path this method will return the Vault secrets mount.

      @param path A path to a secret in Vault.  The path includes the KV v1 or
                  KV v2 secret engine mount path.
      @return The key-value mount in Vault where the path is stored.
      */
    String getMountFromPath(String path) throws VaultException {
        if(!mountVersions) {
            throw new VaultException('No mounts available.  Did you call discoverKVMounts() method?')
        }
        // returns a mount
        String mount = mountVersions.keySet().toList().find { String mount ->
            path.startsWith(mount + '/') || path == mount
        }
        if(mount == null) {
            throw new VaultException('Provided path does not match any existing mounts.')
        }
        mount
    }

    /**
      Given a path representing the full path including a mount and secret path,
      only the location relative to the parent mount is returned.

      @param path A path to a secret in Vault.  The path includes the KV v1 or
                  KV v2 secret engine mount path.
      @return Returns a relative location of a secret.  It is relative to the
              mount and does not include the mount in the return value.
      */
    String getLocationFromPath(String path) {
        String mount = getMountFromPath(path)
        if(!path.contains('/')) {
            return ''
        }
        (path -~ "^\\Q${mount}\\E/")
    }

    /**
      If given a full path to a secret in Vault, a location Map is returned.

      @param path A String representing a valid path to a Vault secret including
                  its secret engine mount.
      @return A location map with two keys: mount and path.  The mount is a KV
              mount in Vault and the path is a location of a secret relative to
              the given mount.
      */
    Map getLocationMapFromPath(String path) {
        Map location = [
            mount: getMountFromPath(path),
            path: getLocationFromPath(path)
        ]
        checkLocationMap(location)
        location
    }

    /**
      If given a location Map it will convert it to a String path for Vault APIs.

      @param location A map with two keys: mount and path.  The mount is a KV
                      mount in Vault and the path is a location of a secret
                      relative to the given mount.
      @return Returns a String representing a valid path to a Vault secret including its secret engine mount.
      */
    String getPathFromLocationMap(Map location) {
        checkLocationMap(location)
        [location.mount, location.path.replaceAll('^/', '')].join('/')
    }

    /**
      List a path in Vault to find KV secret entries.

      @param location A map with two keys: mount and path.  The mount is a KV
                      mount in Vault and the path is a location of a secret
                      relative to the given mount.
      @return Returns a List of secrets for a given path.  If the entry ends
              with a slash <tt>/</tt> then it is a subfolder for listing more secrets.
      */
    List listPath(Map location) {
        checkLocationMap(location)
        String mount = location.mount
        String subpath = (location.path) ? addTrailingSlash(location.path).replaceAll('^/', '') : ''

        if(isKeyValueV2(mount)) {
            apiFetch("${mount}/metadata/${subpath}", [:], 'LIST')?.data?.keys
        }
        else {
            // KV v1 API call here
            apiFetch("${mount}/${subpath}", [:], 'LIST')?.data?.keys
        }
    }

    /**
      List a path in Vault to find KV secret entries.

      @param path A path in Vault including the KV store mount.
      @return Returns a List of secrets for a given path.  If the entry ends
              with a slash <tt>/</tt> then it is a subfolder for listing more secrets.
      */
    List listPath(String path) {
        listPath(getLocationMapFromPath(path))
    }

    /**
      Recursively traverses the path for subkeys.  If level is 0 then there's
      no depth limit.  When level = n, keys are traversed up to the limit.

      <p>A trailing slash <tt>/</tt> is optional and will have different
      behavior depending on the existence of keys in Vault. For example, let's
      say Vault has the following key layout.  <tt>kv</tt> is a KV v2 and
      <tt>secret</tt> is a KV v1 secrets engine.</p>
<pre><tt>kv/
  |- foo
  |- foo/
    |- bar
  |- bar/
    |- baz
  |- hello/
    |- world
      |- friend
secret/
  |- foo
  |- foo/
    |- bar</tt></pre>

    <p>findAllKeys operates on both KV v1 and KV v2 secrets engines the same
    way.</p>

    <ul>
    <li>
    Calling <tt>findAllKeys('kv/foo')</tt> will return <tt>['kv/foo', 'kv/foo/bar']</tt>.
    </li>
    <li>
    Calling <tt>findAllKeys('kv/foo/')</tt> will return <tt>['kv/foo/bar']</tt>.
    </li>
    <li>
    Calling <tt>findAllKeys('secret/foo')</tt> will return <tt>['secret/foo', 'secret/foo/bar']</tt>.
    </li>
    <li>
    Calling <tt>findAllKeys('secret/foo/')</tt> will return <tt>['secret/foo/bar']</tt>.
    </li>
    </ul>
      <p>Given <tt>level</tt> parameter and the layout of the <tt>kv</tt> KV v2
      secrets store from earlier.  You can expect the following behavior when
      passing different integers of level.</tt>
      <ul>
      <li>
      Calling <tt>myvault.findAllKeys('kv', 0)</tt> returns a list of all keys in the entire keystore.
      </li>
      <li>
      Calling <tt>myvault.findAllKeys('kv', 1)</tt> returns <tt>['foo']</tt>.
      </li>
      <li>
      Calling <tt>myvault.findAllKeys('kv/foo', 1)</tt> returns <tt>['foo', 'foo/bar']</tt>.
      </li>
      <li>
      Calling <tt>myvault.findAllKeys('kv/foo/', 1)</tt> returns <tt>['foo/bar']</tt>.
      </li>
      <li>
      Calling <tt>myvault.findAllKeys('kv', 2)</tt> returns <tt>['foo',
      'foo/bar', 'bar/baz']</tt>.  Notice that key <tt>hello/world/friend</tt>
      is not in the list because it is 3 levels deep.
      </li>
      </ul>
      */
    List<String> findAllKeys(String path, Integer level = 0) throws IOException {
        List additionalKeys = []
        // check if a key exists instead of a path
        if(!path.endsWith('/') && path.contains('/')) {
            if(path in findAllKeys(path.tokenize('/')[0..-2].join('/'), 1)) {
                additionalKeys << path
            }
        }
        path = addTrailingSlash(path)
        additionalKeys + recursiveFindAllKeys(path, level, 1)
    }

    /**
      Recursively traverses the path for subkeys.  If level is 0 then there's
      no depth limit.  When level = n, keys are traversed up to the limit.

      <p>To learn more see <tt>{@link #findAllKeys(java.lang.String, java.lang.Integer)}</tt>.</p>
      @param location A location map contains two keys: mount and path.  The
                      mount is a KV mount in Vault and the path is a location of
                      a secret relative to the given mount.
      @param level When traversing secrets paths <tt>level</tt> limits how deep
                   the path goes when returning results.
      */
    List<String> findAllKeys(Map location, Integer level = 0) throws IOException {
        checkLocationMap(location)
        findAllKeys(getPathFromLocationMap(location), level)
    }

    /**
      Copies the contents of secret from a source key, srcKey, to the
      destination key, destKey.  Optional argument srcVersion allows you to
      select a specific version from a Key-Value v2 engine (default is get
      latest version of secret).

      TODO better java doc
      */
    // TODO write tests
    // TODO support Map location
    void copySecret(String srcKey, String destKey, Integer srcVersion = 0) {
        setSecret(destKey, getSecret(srcKey, srcVersion), true)
    }

    /**
      Recursively copies all secrets from the source path, srcPath, to the
      destination path, destPath.

      TODO better java doc
      */
    // TODO write tests
    // TODO support Map location for both srcPath and destPath
    void copyAllKeys(String srcPath, String destPath, Integer level = 0) {
        // TODO support path and path/ for source and destination.
        findAllKeys(srcPath, level).each { String srcKey ->
            String destKey = destPath + (srcKey -~ "^\\Q${srcPath}\\E")
            if(destPath.endsWith('/')) {
                destKey = destPath + getLocationFromPath(srcKey)
            }
            copySecret(srcKey, destKey)
        }
    }

    /**
      Returns a Map of key-value pairs compatible with bash environment
      variables.  The key and value returned in the Map will always be type
      String.

      TODO better java doc
      */
    // TODO write tests
    // TODO support Map location
    Map<String, String> getEnvironmentSecret(String path, Integer version = 0, Boolean allowInvalidKeys = false) {
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
                              are eventually combined.  A path can be a String
                              or a location Map.  A location Map includes a
                              mount and path key e.g.
                              <tt>[mount: 'kv', path: 'my/secret/path']</tt>.
      @param allowInvalidKeys Includes keys which have invalid bash variable
                              names.  This might be useful for additional logic
                              processing.
      @return Returns a Map which is the sum of all of the keys provided.

      TODO better java doc
      */
    // TODO write tests
    // TODO support Map location
    Map<String, String> getEnvironmentSecrets(List paths, Boolean allowInvalidKeys = false) {
        paths.collect { String path ->
            try {
                getEnvironmentSecret(path, 0, allowInvalidKeys)
            } catch(FileNotFoundException e) {
                [:]
            }
        }.sum()
    }

    /**
      Deletes data from a KV v1 or KV v2 secrets engine.
      @param key
      @param destroyVersions
      @param destroyAllVersions Permanently deletes the key metadata and all
                                version data for the specified <tt>key</tt>.
                                When enabled, <tt>destroyVersions</tt> is
                                ignored.  This option is ignored for KV v1
                                secrets engine.
      */
    /* TODO work in progress
    // TODO write tests
    void deleteKey(String key, List<Integer> destroyVersions = [], Boolean destroyAllVersions = false) {
        String mount = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        if(isKeyValueV2(mount)) {
            if(destroyAllVersions) {
                apiFetch("${mount}/metadata/${subpath}", [:], 'DELETE')
            }
            else if(destroyVersions) {
                apiFetch("${mount}/destroy/${subpath}", [:], 'DELETE', objToJson([versions: destroyVersions]))
            }
            else {
                // soft delete
                apiFetch("${mount}/data/${subpath}", [:], 'DELETE')
            }
        }
        else {
            apiFetch(path, [:], 'DELETE')
        }
    }
    */

    /**
      Deletes data from a KV v1 or KV v2 secrets engine.
      @param path
      @param level
      @param destroyAllVersions Permanently deletes the key metadata and all
                                version data for the specified <tt>key</tt>.
                                When enabled, <tt>destroyVersions</tt> is
                                ignored.  This option is ignored for KV v1
                                secrets engine.
      */
    /* TODO work in progress
    // TODO write tests
    void deleteKey(String key, List<Integer> destroyVersions = [], Boolean destroyAllVersions = false) {
    void deletePath(String path, Boolean level = 0, Boolean destroyAllVersions = false) {
        findAllKeys(path, level).sort { String a, String b ->
            // performs a reverse sort to list maximum depth at the beginning
            // of the list.  Depth is defined as the number of '/' in the path.
            b.count('/') <=> a.count('/')
        }.each { String key ->
            deleteKey(key)
        }
    }
    */

    // TODO implement DELETE key
    /* TODO implement recursive DELETE path
           Reverse sort showing deepest depth keys first in the list
           ['a', 'a/b/c', 'a/b', 'a/b/c/d'].sort { a, b -> b.count('/') <=> a.count('/') }
           returns ['a/b/c/d', 'a/b/c', 'a/b', 'a']
     */
    // TODO implement combining path of keys into a List of Maps?  This might not be useful.
    // TODO organize private methods to bottom of class
    // TODO add Vault exception higherarchy?
    // TODO getSecretsAsList should return a combined list matching the order of the input keys.
    // TODO getSecretsAsMap should return a Key-Value Map where the key is the secret key and value is contents of the secret.
}
