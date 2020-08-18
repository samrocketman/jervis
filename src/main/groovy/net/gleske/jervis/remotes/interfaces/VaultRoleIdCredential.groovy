package net.gleske.jervis.remotes.interfaces

interface VaultRoleIdCredential extends JervisCredential {
    String getRole_id()
    String getSecret_id()
}
