/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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

//this code should be at the beginning of every script included which requires bindings
String include_script_name = 'jobs/generate_project_for.groovy'
Set required_bindings = ['parent_job', 'git_service', 'project', 'project_folder', 'global_threadlock']
Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
if(missing_bindings) {
    throw new Exception("${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}")
}

/*
   This will generate jobs for a given branch.
 */

import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.lang.lifecycleGenerator

//generate Jenkins jobs
generate_project_for = null
generate_project_for = { String JERVIS_BRANCH ->
    def folder_listing = git_service.getFolderListing(project, '/', JERVIS_BRANCH)
    def generator = new lifecycleGenerator()
    String jervis_yaml
    if('.jervis.yml' in folder_listing) {
        jervis_yaml = git_service.getFile(project, '.jervis.yml', JERVIS_BRANCH)
    }
    else if('.travis.yml' in folder_listing) {
        jervis_yaml = git_service.getFile(project, '.travis.yml', JERVIS_BRANCH)
    }
    else {
        //skip creating the job for this branch
        println "Skipping branch: ${JERVIS_BRANCH}"
        return
    }
    //try detecting no default language and setting to ruby
    if(jervis_yaml.indexOf('language:') < 0) {
        generator.yaml_language = 'ruby'
    }
    generator.loadPlatformsString(parent_job.readFileFromWorkspace('src/main/resources/platforms.json').toString())
    generator.preloadYamlString(jervis_yaml)
    //could optionally read lifecycles and toolchains files by OS
    def os_stability = "${generator.label_os}-${generator.label_stability}"
    generator.loadLifecyclesString(parent_job.readFileFromWorkspace("src/main/resources/lifecycles-${os_stability}.json").toString())
    generator.loadToolchainsString(parent_job.readFileFromWorkspace("src/main/resources/toolchains-${os_stability}.json").toString())
    generator.loadYamlString(jervis_yaml)

    generator.folder_listing = folder_listing
    if(!generator.isGenerateBranch(JERVIS_BRANCH)) {
        //the job should not be generated for this branch
        //based on the branches section of .jervis.yml
        println "Skipping branch: ${JERVIS_BRANCH}"
        return
    }

    //attempt to get the private key else return an empty string
    String credentials_id = generator.getObjectValue(generator.jervis_yaml, 'jenkins.secrets_id', '')
    String private_key_contents = getFolderRSAKeyCredentials(project_folder, credentials_id)
    //try decrypting secrets
    if(credentials_id.size() > 0 && private_key_contents.size() == 0) {
        throw new SecurityException("Could not find private key using Jenkins Credentials ID: ${credentials_id}")
    }
    if(private_key_contents.size() > 0) {
        println "Attempting to decrypt jenkins.secrets using Jenkins Credentials ID ${credentials_id}."
        File priv_key = File.createTempFile('temp', '.txt')
        //delete file if JVM is shut down
        priv_key.deleteOnExit()
        try {
            priv_key.write(private_key_contents)
            generator.setPrivateKeyPath(priv_key.getAbsolutePath())
            generator.decryptSecrets()
        }
        catch(Throwable t) {
            //clean up temp file
            priv_key.delete()
            //rethrow caught throwable
            throw t
        }
        //done decrypting so clean up the private key
        priv_key.delete()
        //print a list of the keys attempting to be decrypted
        println "Decrypted the following properties (indented):"
        generator.plainlist*.get('key').each { println "    ${it}" }
    }
    //end decrypting secrets

    //non-pull request job provided by jobs/main_job.groovy
    global_threadlock.withLock {
        jenkinsJob generator, false, JERVIS_BRANCH
    }
}
