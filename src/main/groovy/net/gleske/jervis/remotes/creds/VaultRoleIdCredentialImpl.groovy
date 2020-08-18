import net.gleske.jervis.remotes.interfaces.VaultRoleIdCredential

class VaultRoleIdCredentialImpl implements VaultRoleIdCredential {
    private final String role_id
    private final String secret_id
    VaultRoleIdCredentialImpl(String role_id, String secret_id) {
        this.role_id = role_id
        this.secret_id = secret_id
    }

    String getRole_id() {
        this.role_id
    }

    String getSecret_id() {
        this.secret_id
    }
}
