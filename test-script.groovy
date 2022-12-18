import static net.gleske.jervis.remotes.StaticMocking.recordMockUrls
import net.gleske.jervis.remotes.SimpleRestServiceSupport

if(!binding.hasVariable('url')) {
    String persistStr
    url = persistStr
}
if(binding.hasVariable('request_meta')) {
    request_meta.clear()
} else {
    request_meta = [:]
}

if(binding.hasVariable('request_history')) {
    request_history.clear()
} else {
    request_history = []
}

import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.remotes.VaultService

TokenCredential creds = [getToken: {-> 'hvs.CT1912OdOBRWnn1UVQntX9Ld' }] as TokenCredential
VaultService myvault = new VaultService('http://vault:8200/v1/', creds)

myvault.discoverKVMounts()

// enable secrets mounts
Boolean init_kv = false
Boolean init_kv_cas = false
Boolean init_secret = false
if(!('kv' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/kv', [:], 'POST', '{"type": "kv", "options": {"version": "2"}}')
    init_kv = true
}

if(!('kv_cas' in myvault.mountVersions.keySet())) {
    myvault.apiFetch('sys/mounts/kv_cas', [:], 'POST', '{"type": "kv", "options": {"version": "2"}}')
    myvault.apiFetch('kv_cas/config', [:], 'POST', '{"cas_required":true}')
    init_kv_cas = true
}
if(!('secret' in myvault.mountVersions.keySet())) {
  myvault.apiFetch('sys/mounts/secret', [:], 'POST', '{"type": "kv", "options": {"version": "1"}}')
  init_secret = true
}

// Discover mounts after potentially adding new secrets engines.
myvault.discoverKVMounts()

if(init_kv) {
    myvault.setSecret("kv/v2_force_cas_update", [test: 'data'], true)
    myvault.setSecret("kv/foo", ['hello':'world'])
    myvault.setSecret("kv/foo", ['another':'secret', 'hello':'world'])
    myvault.setSecret("kv/foo/bar", ['hello':'friend'])
    myvault.setSecret("kv/foo/bar/baz", ['foo':'bar'])
}
if(init_kv_cas) {
    myvault.setSecret("kv_cas/data_to_update", ['hello':'world'])
}
if(init_secret) {
    myvault.setSecret("secret/foo", ['test':'data'])
    myvault.setSecret("secret/foo/bar", ['someother':'data'])
    myvault.setSecret("secret/foo/bar/baz", ['more':'secrets'])
}

// Reset discovered mounts in order to capture API responses
myvault.@mountVersions = [:]
myvault.@cas_required = []

// Record URL API data to files as mock data
recordMockUrls(url, URL, request_meta, true, 'SHA-256', request_history)

// Rediscover mounts
myvault.discoverKVMounts()

// Read operations
myvault.getSecret('secret/foo')
myvault.getSecret('secret/foo/bar')
myvault.getSecret('secret/foo/bar/baz')
myvault.getSecret('kv/foo')
myvault.getSecret('kv/foo/bar')
myvault.getSecret('kv/foo/bar/baz')
myvault.getSecret('kv/foo', 1)
myvault.getSecret(mount: 'secret', path: 'foo')
myvault.getSecret(mount: 'secret', path: 'foo/bar')
myvault.getSecret(mount: 'secret', path: 'foo/bar/baz')
myvault.getSecret(mount: 'secret', path: 'foo/bar/baz', dont_care: 'value')
myvault.getSecret(mount: 'kv', path: 'foo')
myvault.getSecret(mount: 'kv', path: 'foo/bar')
myvault.getSecret(mount: 'kv', path: 'foo/bar/baz')
myvault.getSecret(mount: 'kv', path: 'foo/bar/baz', dont_care: 'value')
myvault.getSecret(mount: 'kv', path: 'foo', 1)
myvault.findAllKeys('secret')
myvault.findAllKeys('secret/')
myvault.findAllKeys('secret/', 1)
myvault.findAllKeys('secret/', 2)
myvault.findAllKeys('secret/', 3)
myvault.findAllKeys('kv')
myvault.findAllKeys('kv/')
myvault.findAllKeys('kv/', 1)
myvault.findAllKeys('kv/', 2)
myvault.findAllKeys('kv/', 3)
myvault.listPath('secret')
myvault.listPath('secret/')
myvault.listPath('secret/foo')
myvault.listPath('secret/foo/')
myvault.listPath('secret/foo/bar')
myvault.listPath('secret/foo/bar/')
myvault.listPath('kv')
myvault.listPath('kv/')
myvault.listPath('kv/foo')
myvault.listPath('kv/foo/')
myvault.listPath('kv/foo/bar')
myvault.listPath('kv/foo/bar/')

// Write operations
myvault.copySecret('secret/foo', 'kv/v1_to_v2')
myvault.copySecret('kv/foo', 'secret/v2_to_v1')
myvault.copySecret('kv/foo', 'secret/v2_to_v1_version_1', 1)
myvault.copySecret('kv/foo', 'kv/v2_to_v2/v2_to_v2')
myvault.copySecret('kv/foo', 'kv/v2_to_v2_version_1', 1)
myvault.setSecret('secret/v1_set', [another: 'secret', hello: 'world'])
myvault.setSecret('secret/v1_set_force_cas', [another: 'secret', hello: 'world'], true)
myvault.setSecret('kv/v2_no_cas', [test: 'data'])
myvault.setSecret('kv/v2_force_cas', [test: 'data'], true)
myvault.setSecret('kv/v2_force_cas_update', [test: 'update'], true)
myvault.setSecret('kv_cas/v2_detect_cas', [another: 'secret', hello: 'world'])
myvault.setSecret('kv_cas/data_to_update', [update: 'secret'])
println 'Success.'