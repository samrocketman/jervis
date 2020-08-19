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

import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.exceptions.JervisException

// TODO: java doc
    // TODO document recommended setup and usage (setting up recommended
    //   policy, AppId, use batch token, manually set mountVersions to cut down
    //   on API calls).  Add a bullet point list of useful methods.  Document
    //   usage of the method within the method itself.
    // TODO document minimum required role for full functionality
    // TODO document reducing the role and code changes required (e.g. using
    //   mountVersions or a batch token instead of a service token)
class VaultService implements SimpleRestServiceSupport {
    private final String vault_url
    private final TokenCredential credential

    // TODO: java doc
    Map mountVersions = [:]

    VaultService(String vault_url, TokenCredential credential) {
        this.vault_url = addTrailingSlash(vault_url)
        if(!this.vault_url.endsWith('v1/')) {
            this.vault_url += 'v1/'
        }
        this.credential = credential
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
}
