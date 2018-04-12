/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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

@Grab(group='net.gleske', module='jervis', version='1.0', transitive=false)
@Grab(group='org.yaml', module='snakeyaml', version='1.19', transitive=false)

import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.lang.pipelineGenerator
import net.gleske.jervis.remotes.GitHub
import static net.gleske.jervis.lang.lifecycleGenerator.getObjectValue

import hudson.console.HyperlinkNote
import hudson.util.Secret
import jenkins.bouncycastle.api.PEMEncodable
import jenkins.model.Jenkins
import static jenkins.bouncycastle.api.PEMEncodable.decode


/**
  Gets GitHub API token from the global credential store.
 */

@NonCPS
String getGitHubAPIToken() {
    Jenkins.instance.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getCredentials().find {
        it.class.simpleName == 'StringCredentialsImpl' && it.id == 'github-token'
    }.with {
        if(it) {
            it.secret
        }
    } as String
}


/**
  Reads GitHub API and returns the .jervis.yml file via API instead of a
  workspace checkout.

  @return A list where the first item is jervis_yaml and the second item is a
          list of files in the root of the repository.
 */
@NonCPS
List getJervisMetaData(String project, String JERVIS_BRANCH) {
    String jervis_yaml
    def git_service = new GitHub()
    git_service.gh_token = getGitHubAPIToken()
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
    [jervis_yaml, folder_listing]
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
  An environment wrapper which sets environment variables.  If available, also
  sets and masks decrypted properties from .jervis.yml.
 */
def withEnvSecretWrapper(pipelineGenerator generator, List envList, Closure body) {
    List spe = generator.secretPairsEnv
    List secretPairs = spe[0]
    List secretEnv = spe[1]
    if(secretPairs) {
        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: secretPairs]) {
            withEnv(secretEnv + envList) {
                body()
            }
        }
    }
    else {
        withEnv(envList) {
            body()
        }
    }
}


/**
  Returns a string which can be printed.  It is the decrypted properties from a
  .jervis.yml file.
 */
@NonCPS
String printDecryptedProperties(lifecycleGenerator generator, String credentials_id) {
    [
        "Attempting to decrypt jenkins.secrets using Jenkins Credentials ID ${credentials_id}.",
        'Decrypted the following properties (indented):',
        '    ' + (generator.plainmap.keySet() as List).join('\n    ')
    ].join('\n') as String
}


/**
  The main method of buildViaJervis()
 */
def call() {
    def generator = new lifecycleGenerator()
    def pipeline_generator
    String environment_string
    String github_domain
    String github_org
    String github_repo
    String jenkins_folder
    String jervis_yaml
    String lifecycles_json
    String os_stability
    String platforms_json
    String script_footer
    String script_header
    String toolchains_json
    List folder_listing = []
    BRANCH_NAME = env.CHANGE_BRANCH?:env.BRANCH_NAME
    boolean is_pull_request = (env.CHANGE_ID?:false) as Boolean
    env.IS_PR_BUILD = "${is_pull_request}" as String
    currentBuild.rawBuild.parent.parent.sources[0].source.with {
        github_org = it.repoOwner
        github_repo = it.repository
        github_domain = (it.apiUri)? it.apiUri.split('/')[2] : 'github.com'
    }
    //fix pull request branch name.  Otherwise shows up as PR-* as the branch name.
    if(is_pull_request) {
        env.BRANCH_NAME = env.CHANGE_BRANCH
    }
    jenkins_folder = currentBuild.rawBuild.parent.parent.fullName.split('/')[0]
    List jervisEnvList = [
        "JERVIS_DOMAIN=${github_domain}",
        "JERVIS_ORG=${github_org}",
        "JERVIS_PROJECT=${github_repo}",
        "JERVIS_BRANCH=${BRANCH_NAME}",
        "IS_PR_BUILD=${is_pull_request}"
    ]

    def global_scm = scm

    stage('Process Jervis YAML') {
        platforms_json = libraryResource 'platforms.json'
        generator.loadPlatformsString(platforms_json)
        List jervis_metadata = getJervisMetaData("${github_org}/${github_repo}".toString(), BRANCH_NAME)
        jervis_yaml = jervis_metadata[0]
        folder_listing = jervis_metadata[1]
        generator.preloadYamlString(jervis_yaml)
        os_stability = "${generator.label_os}-${generator.label_stability}"
        lifecycles_json = libraryResource "lifecycles-${os_stability}.json"
        toolchains_json = libraryResource "toolchains-${os_stability}.json"
        generator.loadLifecyclesString(lifecycles_json)
        generator.loadToolchainsString(toolchains_json)
        generator.loadYamlString(jervis_yaml)
        generator.folder_listing = folder_listing
        pipeline_generator = new pipelineGenerator(generator)
        pipeline_generator.supported_collections = ['cobertura', 'junit', 'artifacts']
        //attempt to get the private key else return an empty string
        String credentials_id = generator.getObjectValue(generator.jervis_yaml, 'jenkins.secrets_id', '')
        String private_key_contents = getFolderRSAKeyCredentials(jenkins_folder, credentials_id)
        if(credentials_id && !private_key_contents) {
            throw new SecurityException("Could not find private key using Jenkins Credentials ID: ${credentials_id}")
        }
        if(private_key_contents) {
            generator.setPrivateKey(private_key_contents)
            generator.decryptSecrets()
            echo "DECRYPTED PROPERTIES\n${printDecryptedProperties(generator, credentials_id)}"
        }
        //end decrypting secrets
        script_header = libraryResource "header.sh"
        script_footer = libraryResource "footer.sh"
        jervisEnvList << "JERVIS_LANG=${generator.yaml_language}"
    }

    //prepare to run
    if(generator.isMatrixBuild()) {
        //a matrix build which should be executed in parallel
        Map tasks = [failFast: true]
        pipeline_generator.buildableMatrixAxes.each { matrix_axis ->
            String stageIdentifier = matrix_axis.collect { k, v -> generator.matrix_fullName_by_friendly[v]?:v }.join('\n')
            String label = generator.labels
            List axisEnvList = matrix_axis.collect { k, v -> "${k}=${v}" }
            Map stashMap = pipeline_generator.getStashMap(matrix_axis)
            tasks[stageIdentifier] = {
                node(label) {
                    stage("Checkout SCM") {
                        checkout global_scm
                    }
                    stage("Build axis ${stageIdentifier}") {
                        Boolean failed_stage = false
                        withEnvSecretWrapper(pipeline_generator, axisEnvList + jervisEnvList) {
                            environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
                            echo "ENVIRONMENT:\n    ${environment_string}"
                            try {
                                sh(script: [
                                    script_header,
                                    generator.generateAll(),
                                    script_footer
                                ].join('\n').toString())
                            }
                            catch(e) {
                                failed_stage = true
                            }
                        }
                        for(String name : stashMap.keySet()) {
                            try {
                                stash allowEmpty: stashMap[name]['allow_empty'], includes: stashMap[name]['includes'], name: name, useDefaultExcludes: stashMap[name]['use_default_excludes']
                            }
                            catch(e) {
                                if(!failed_stage) {
                                    //rethrow proper exception if this stage hasn't failed
                                    throw e
                                }
                            }
                        }
                        if(failed_stage) {
                            currentBuild.result = 'FAILURE'
                        }
                    }
                }
            }
        }
        parallel(tasks)
    }

    node(generator.labels) {
        if(!generator.isMatrixBuild()) {
            Map stashMap = pipeline_generator.stashMap
            stage("Checkout SCM") {
                checkout global_scm
            }
            stage("Build Project") {
                withEnvSecretWrapper(pipeline_generator, jervisEnvList) {
                    environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
                    echo "ENVIRONMENT:\n    ${environment_string}"
                    sh(script: [
                        script_header,
                        generator.generateAll(),
                        script_footer
                    ].join('\n').toString())
                }
                for(String name : stashMap.keySet()) {
                    stash allowEmpty: stashMap[name]['allow_empty'], includes: stashMap[name]['includes'], name: name, useDefaultExcludes: stashMap[name]['use_default_excludes']
                }
            }
        }
        List publishableItems = pipeline_generator.publishableItems
        if(publishableItems) {
            //admin defining default settings for publishers they support
            pipeline_generator.collect_settings_defaults = [
                artifacts: [
                    allowEmptyArchive: false,
                    caseSensitive: true,
                    defaultExcludes: true,
                    excludes: ''
                ],
                cobertura: [
                    autoUpdateHealth: false,
                    autoUpdateStability: false,
                    failNoReports: false,
                    failUnhealthy: false,
                    failUnstable: false,
                    maxNumberOfBuilds: 0,
                    onlyStable: false,
                    sourceEncoding: 'ASCII',
                    zoomCoverageChart: false,
                    methodCoverageTargets: '80, 0, 0',
                    lineCoverageTargets: '80, 0, 0',
                    conditionalCoverageTargets: '70, 0, 0'
                ],
                html: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: false,
                    includes: '**/*',
                    keepAll: false,
                    reportFiles: 'index.html',
                    reportName: 'HTML Report',
                    reportTitles: ''
                ],
                junit: [
                    allowEmptyResults: false,
                    healthScaleFactor: 1.0,
                    keepLongStdio: false
                ]
            ]
            //admin requiring stashes for HTML publisher to be formed a compatible way.
            pipeline_generator.stashmap_preprocessor = [
                html: { Map settings ->
                    settings['includes']?.tokenize(',').collect {
                        "${settings['path']  -~ '/$' -~ '^/'}/${it}"
                    }.join(',').toString()
                }
            ]
            //admin supporting optional list or string for filesets in default settings
            pipeline_generator.collect_settings_filesets = [artifacts: ['excludes'], html: ['includes']]
            //admin requiring regex validation of specific jenkins.collect setinggs
            //if a user fails the input validation it falls back to the default option
            //if an invalid path is specified for HTML publisher then do not attempt to collect
            String cobertura_targets_regex = '([0-9]*\\.?[0-9]*,? *){3}[^,]$'
            pipeline_generator.collect_settings_validation = [
                cobertura: [
                    methodCoverageTargets: cobertura_targets_regex,
                    lineCoverageTargets: cobertura_targets_regex,
                    conditionalCoverageTargets: cobertura_targets_regex
                ],
                html: [
                    path: '''^[^,\\:*?"'<>|]+$'''
                ]
            ]
            stage("Publish results") {
                for(String name : publishableItems) {
                    unstash name
                }
                for(String publishable : publishableItems) {
                    def item = pipeline_generator.getPublishable(publishable)
                    switch(publishable) {
                        case 'artifacts':
                            archiveArtifacts artifacts: item['path'], fingerprint: true,
                                             excludes: item['excludes'],
                                             allowEmptyArchive: item['allowEmptyArchive'],
                                             defaultExcludes: item['defaultExcludes'],
                                             caseSensitive: item['caseSensitive']
                            break
                        case 'cobertura':
                            cobertura coberturaReportFile: item['path'],
                                    autoUpdateHealth: item['autoUpdateHealth'],
                                    autoUpdateStability: item['autoUpdateStability'],
                                    failNoReports: item['failNoReports'],
                                    failUnhealthy: item['failUnhealthy'],
                                    failUnstable: item['failUnstable'],
                                    maxNumberOfBuilds: item['maxNumberOfBuilds'],
                                    onlyStable: item['onlyStable'],
                                    sourceEncoding: item['sourceEncoding'],
                                    zoomCoverageChart: item['zoomCoverageChart'],
                                    methodCoverageTargets: item['methodCoverageTargets'],
                                    lineCoverageTargets: item['lineCoverageTargets'],
                                    conditionalCoverageTargets: item['conditionalCoverageTargets']
                            break
                        case 'html':
                            publishHTML allowMissing: item['allowMissing'],
                                        alwaysLinkToLastBuild: item['alwaysLinkToLastBuild'],
                                        includes: item['includes'],
                                        keepAll: item['keepAll'],
                                        reportDir: item['path'],
                                        reportFiles: item['reportFiles'],
                                        reportName: item['reportName'],
                                        reportTitles: item['reportTitles']
                            break
                        case 'junit':
                            junit allowEmptyResults: item['allowEmptyResults'],
                                  healthScaleFactor: item['healthScaleFactor'],
                                  keepLongStdio: item['keepLongStdio'],
                                  testResults: item['path']

                            break
                    }
                }
            }
        }
        if(currentBuild.result == 'FAILURE') {
            error 'This build has failed.  No user-defined pipelines will be run.'
        }
        if(generator.isPipelineJob()) {
            if(generator.isMatrixBuild()) {
                stage("Checkout Jenkinsfile") {
                    checkout global_scm
                }
            }
            load generator.jenkinsfile
        }
    }
}
