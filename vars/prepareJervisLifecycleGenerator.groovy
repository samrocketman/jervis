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

// static imports always first according to CodeNarc
import static jenkins.bouncycastle.api.PEMEncodable.decode

import static net.gleske.jervis.tools.YamlOperator.getObjectValue
import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.lang.LifecycleGenerator
import net.gleske.jervis.remotes.GitHub

import hudson.console.HyperlinkNote
import hudson.util.Secret
import jenkins.bouncycastle.api.PEMEncodable
import jenkins.model.Jenkins

/**
  Gets GitHub API token from the global credential store.
 */
@NonCPS
Secret getGitHubAPIToken(String credentials_id) {
    Jenkins.instance.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getCredentials().find {
        it.class.simpleName == 'StringCredentialsImpl' && it.id == credentials_id
    }.with {
        if(it) {
            it.secret
        }
    }
}

/**
  Gets RSA credentials for a given folder.
 */
@NonCPS
String getFolderRSAKeyCredentials(String folder, String credentials_id) {
    if(!folder || !credentials_id) {
        return ''
    }
    def credentials = Jenkins.instance.getJob(folder).properties.find { it.class.simpleName == 'FolderCredentialsProperty' }
    String found_credentials = ''
    try {
        if(credentials) {
            credentials.domainCredentials*.credentials*.each { c ->
                if(c && c.class.simpleName == 'BasicSSHUserPrivateKey' && c.id == credentials_id) {
                    String priv_key = c.privateKey
                    Secret p = c.passphrase
                    found_credentials = new PEMEncodable(decode(priv_key, ((p)? p.plainText : null) as char[]).toPrivateKey()).encode()
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

/**
  Reads GitHub API and returns the .jervis.yml file via API instead of a
  workspace checkout.

  @return A list where the first item is jervis_yaml and the second item is a
          list of files in the root of the repository.
 */
@NonCPS
List initializeGenerator(LifecycleGenerator generator, String project, String JERVIS_BRANCH, String credentials_id) {
    String jervis_yaml
    def git_service = new GitHub()
    git_service.gh_token = getGitHubAPIToken(credentials_id).toString()
    def folder_listing = git_service.getFolderListing(project, '/', JERVIS_BRANCH)
    if('.jervis.yml' in folder_listing) {
        jervis_yaml = git_service.getFile(project, '.jervis.yml', JERVIS_BRANCH)
    }
    else if('.travis.yml' in folder_listing) {
        jervis_yaml = git_service.getFile(project, '.travis.yml', JERVIS_BRANCH)
    }
    else {
        throw new FileNotFoundException('Cannot find .jervis.yml nor .travis.yml')
    }
    generator.preloadYamlString(jervis_yaml)
    [jervis_yaml, folder_listing]
}

/**
  This finishes initializing the lifecycle generator object now that the
  platform and stability requested by the user in their YAML is known.  The
  reason why this is separate from initializeGemerator is because we needed to
  know the platform and stability a user desired for the build environment
  before we could obtain the lifecycles and toolchains appropriate for desired
  platform and stability.
 */
@NonCPS
void finalizeGenerator(LifecycleGenerator generator, String lifecycles_json, String toolchains_json, String jervis_yaml, List folder_listing, String jenkins_folder) {
    generator.loadLifecyclesString(lifecycles_json)
    generator.loadToolchainsString(toolchains_json)
    generator.loadYamlString(jervis_yaml)
    generator.folder_listing = folder_listing
    String secrets_credentials_id = getObjectValue(generator.jervis_yaml, 'jenkins.secrets_id', '')
    String private_key_contents = getFolderRSAKeyCredentials(jenkins_folder, secrets_credentials_id)
    if(secrets_credentials_id && !private_key_contents) {
        throw new SecurityException("Could not find private key using Jenkins Credentials ID: ${secrets_credentials_id}")
    }
    if(private_key_contents) {
        generator.setPrivateKey(private_key_contents)
        generator.decryptSecrets()
    }
}

@NonCPS
void call(LifecycleGenerator generator, String github_credentials) {
    /*
       Initialize generator object from GitHub API.
     */
    String project = currentBuild.rawBuild.parent.parent.sources[0].source.with { "${it.repoOwner}/${it.repository}" }
    generator.loadPlatformsString(loadCustomResource('platforms.yaml'))
    String branch = ((isPRBuild()) ? "refs/pull/${env.CHANGE_ID}/head" : env.BRANCH_NAME)

    initializeGenerator(generator, project, branch, github_credentials).with {
        String os_stability = "${generator.label_os}-${generator.label_stability}"
        finalizeGenerator(generator,
            loadCustomResource("lifecycles-${os_stability}.yaml"),
            loadCustomResource("toolchains-${os_stability}.yaml"),
            it[0],
            it[1],
            currentBuild.rawBuild.parent.parent.fullName.split('/')[0])
    }
}
