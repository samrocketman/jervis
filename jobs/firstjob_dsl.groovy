/*
   Copyright 2014-2016 Sam Gleske - https://github.com/samrocketman/jervis

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

import hudson.util.Secret
import jenkins.model.Jenkins
import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.remotes.GitHub

//script bindings
scriptApproval = Jenkins.instance.getExtensionList('org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval')[0]
system_creds = Jenkins.instance.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0]

def git_service = new GitHub()
//Pre-job setup based on the type of remote
switch(git_service) {
    case GitHub:
        //authenticate
        String gh_token = system_creds.getCredentials().find {
            it.class.simpleName == 'StringCredentialsImpl' && it.id == 'github-token'
        }.with {
            if(it) {
                it.secret
            }
        }
        if(gh_token) {
            println 'Found github-token credentials ID.'
            git_service.gh_token = gh_token
        }
        else if(System.getenv('GITHUB_TOKEN')) {
            println 'Found GITHUB_TOKEN environment variable.'
            git_service.gh_token = System.getenv('GITHUB_TOKEN')
        }
        //GitHub Enterprise web URL; otherwise it will simply be github.com
        if(System.getenv('GITHUB_URL')) {
            println 'Found GITHUB_URL environment variable.'
            git_service.gh_web = System.getenv('GITHUB_URL')
        }
}

//Get credentials from a folder
public String getFolderRSAKeyCredentials(String folder, String credentials_id) {
    if(folder.equals('') || credentials_id.equals('')) {
        return ''
    }
    def credentials
    def properties = Jenkins.getInstance().getJob(folder).getProperties()
    for(int i=0; i < properties.size(); i++) {
        if(properties.get(i).getClass().getSimpleName() == 'FolderCredentialsProperty') {
            credentials = properties.get(i)
        }
    }
    String found_credentials = ''
    if(credentials != null ) {
        credentials.getDomainCredentials().each { domain ->
            domain.getCredentials().each { credential ->
                if(credential != null && credential.getClass().getSimpleName() == 'BasicSSHUserPrivateKey') {
                    if(credential.getId() == credentials_id) {
                        found_credentials = credential.getPrivateKey()
                    }
                }
            }
        }
    }
    return found_credentials
}

//generate Jenkins jobs
def generate_project_for(def git_service, String JERVIS_BRANCH) {
    //def JERVIS_BRANCH = it
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
    generator.loadPlatformsString(readFileFromWorkspace('src/main/resources/platforms.json').toString())
    generator.preloadYamlString(jervis_yaml)
    //could optionally read lifecycles and toolchains files by OS
    generator.loadLifecyclesString(readFileFromWorkspace('src/main/resources/lifecycles.json').toString())
    generator.loadToolchainsString(readFileFromWorkspace('src/main/resources/toolchains.json').toString())
    generator.loadYamlString(jervis_yaml)
    String project_folder = "${project}".split('/')[0]
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
    generator.folder_listing = folder_listing
    if(!generator.isGenerateBranch(JERVIS_BRANCH)) {
        //the job should not be generated for this branch
        //based on the branches section of .jervis.yml
        println "Skipping branch: ${JERVIS_BRANCH}"
        return
    }
    //chooses job type based on Jervis YAML
    def jervis_jobType
    if(generator.isMatrixBuild()) {
        jervis_jobType = { String name, Closure closure -> matrixJob(name, closure) }
    }
    else {
        jervis_jobType = { String name, Closure closure -> freeStyleJob(name, closure) }
    }
    println "Generating branch: ${JERVIS_BRANCH}"
    //the generated Job DSL enclosure depends on the job type
    jervis_jobType("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-')) {
        displayName("${project_name} (${JERVIS_BRANCH} branch)")
        label(generator.getLabels())
        if(generator.isMatrixBuild()) {
            //workaround for matrix builds ref: https://github.com/jenkinsci/docker-plugin/issues/242
            properties {
                groovyLabelAssignmentProperty {
                    secureGroovyScript {
                        String groovyscript = "return currentJob.getClass().getSimpleName().equals('MatrixProject') ? 'master' : '${generator.getLabels()}'"
                        script(groovyscript)
                        sandbox(false)
                        //workaround for https://issues.jenkins-ci.org/browse/JENKINS-46016
                        scriptApproval.approveScript(scriptApproval.hash(groovyscript, 'groovy'))
                    }
                }
            }
        }
        //configure encrypted properties
        if(generator.plainlist.size() > 0) {
            configure { project ->
                project / 'buildWrappers' / 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper' / 'varPasswordPairs'() {
                    generator.plainlist.each { pair ->
                        'varPasswordPair'(var: pair['key'], password: Secret.fromString(pair['secret']).getEncryptedValue())
                    }
                }
            }
        }
        scm {
            //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
            //for more info about the git closure
            git {
                remote {
                    url(git_service.getCloneUrl() + "${project}.git")
                }
                branch("refs/heads/${JERVIS_BRANCH}")
                //configure git web browser based on the type of remote
                switch(git_service) {
                    case GitHub:
                        configure { gitHub ->
                            gitHub / browser(class: 'hudson.plugins.git.browser.GithubWeb') {
                                url(git_service.getWebUrl() + "${project}")
                            }
                        }
                }
            }
        }
        steps {
            shell([
                readFileFromWorkspace('assets/header.sh'),
                "export JERVIS_LANG=\"${generator.yaml_language}\"",
                "export JERVIS_DOMAIN=\"${git_service.getWebUrl().split('/')[2]}\"",
                "export JERVIS_ORG=\"${project_folder}\"",
                "export JERVIS_PROJECT=\"${project_name}\"",
                "export JERVIS_BRANCH=\"${JERVIS_BRANCH}\"",
                generator.generateAll(),
                readFileFromWorkspace('assets/footer.sh')
                ].join('\n'))
        }
        //if a matrix build then generate matrix bits
        if(generator.isMatrixBuild()) {
            axes {
                generator.yaml_matrix_axes.each {
                    text(it, generator.matrixGetAxisValue(it).split())
                }
            }
            combinationFilter(generator.matrixExcludeFilter())
        }
        publishers {
            String[] enabled_collections = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect', [:]).keySet() as String[]
            if('artifacts' in enabled_collections) {
                //artifact lists as a single string or a list in YAML
                def collect_artifacts = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.artifacts', new Object())
                collect_artifacts = (collect_artifacts instanceof List)? collect_artifacts.join(',') : collect_artifacts.toString()
                if(collect_artifacts.size() > 1) {
                    archiveArtifacts {
                        fingerprint(true)
                        onlyIfSuccessful(true)
                        pattern(collect_artifacts)
                    }
                }
            }
            if('junit' in enabled_collections) {
                String collect_junit = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.junit', '')
                if(collect_junit.size() > 0) {
                    archiveJunit(collect_junit)
                }
            }
            if('cobertura' in enabled_collections) {
                String collect_cobertura = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.cobertura', '')
                if(collect_cobertura.size() > 0) {
                    cobertura(collect_cobertura)
                    covComplPlotPublisher {
                        analyzer 'Cobertura'
                        excludeGetterSetter false
                        verbose false
                        locateTopMost true
                    }
                }
            }
        }
    }
}

if("${project}".size() > 0 && "${project}".split('/').length == 2) {
    println 'Generating jobs for ' + git_service.toString() + " project ${project}."

    project_folder = "${project}".split('/')[0]
    project_name = "${project}".split('/')[1]

    if(!Jenkins.instance.getItem(project_folder)) {
        println "Creating folder ${project_folder}"
        folder(project_folder)
    }

    println "Creating project ${project}"
    listView("${project}") {
        description(git_service.toString() + ' Project ' + git_service.getWebUrl() + "${project}")
        filterBuildQueue()
        filterExecutors()
        jobs {
            regex('^' + "${project_name}".replaceAll('/','-') + '.*')
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
    //generate projects for one or more branches
    if(branch != null && branch.size() > 0) {
        generate_project_for(git_service, branch)
    }
    else {
        git_service.branches("${project}").each { branch ->
            generate_project_for(git_service, branch)
        }
    }
}
else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
