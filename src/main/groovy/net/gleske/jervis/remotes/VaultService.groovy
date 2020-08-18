package net.gleske.jervis.remotes

import net.gleske.jervis.remotes.interfaces.TokenCredential

class VaultService implements SimpleRestServiceSupport {
    private final String vault_url
    private final TokenCredential credential

    VaultService(String vault_url, TokenCredential credential) {
        this.vault_url = (vault_url[-1] == '/')? vault_url : vault_url + '/'
        this.credential = credential
    }

    String baseUrl() {
        this.vault_url
    }

    Map header(Map headers = [:]) {
        headers['X-Vault-Token'] = credential.getToken()
        headers
    }

    Map getSecretV2(String path, int version = 0) {
        String engine = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        apiFetch("v1/${engine}/data/${subpath}?version=${version}")?.data?.data
    }

    List listSecretV2(String path) {
        String engine = path -~ '/.*$'
        String subpath = path -~ '^[^/]+/'
        subpath = (!subpath || subpath[-1] == '/')? subpath : subpath + '/'
        apiFetch("v1/${engine}/metadata/${subpath}?list=true")?.data?.keys
    }
}
