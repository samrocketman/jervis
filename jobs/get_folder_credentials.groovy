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

/*
   Gets a private key from a folder credentials ID.  If the private key is
   encrypted then it is automatically decrypted.
 */
import hudson.console.HyperlinkNote
import hudson.util.Secret
import jenkins.model.Jenkins

require_bindings('jobs/get_folder_credentials.groovy', ['hack_class_loader'])

getFolderRSAKeyCredentials = null
getFolderRSAKeyCredentials = { String folder, String credentials_id ->
    if(!folder || !credentials_id) {
        return ''
    }
    def credentials = Jenkins.instance.getJob(folder).properties.find { it.class.simpleName == 'FolderCredentialsProperty' }
    String found_credentials = ''
    try {
        if(credentials) {
            credentials.domainCredentials*.credentials*.each { c ->
                if(!found_credentials && c?.class?.simpleName == 'BasicSSHUserPrivateKey' && c?.id == credentials_id) {
                    String priv_key = c.privateKey
                    Secret p = c.passphrase
                    Binding superBinding = new Binding([priv_key: c.privateKey, p: p])
                    GroovyShell superShell = new GroovyShell(Jenkins.instance.pluginManager.uberClassLoader, superBinding)
                    superShell.evaluate('''
                        |import jenkins.bouncycastle.api.PEMEncodable
                        |import org.bouncycastle.openssl.PEMParser
                        |result = PEMEncodable.decode(priv_key, ((p)? p.plainText : null) as char[]).encode().toString()
                        '''.trim().stripMargin())
                    found_credentials = superBinding.getVariable('result')
                }
            }
        }
    }
    catch(Throwable t) {
        message = 'An exception occurred when decrypting credential '
        message += HyperlinkNote.encodeTo('/' + Jenkins.instance.getItemByFullName(folder).url + 'credentials/', credentials_id)
        message += ' from folder '
        message += HyperlinkNote.encodeTo('/' + Jenkins.instance.getItemByFullName(folder).url, folder) + '.'
        println message
        throw t
    }
    return found_credentials
}
