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

@Grab(group='net.gleske', module='jervis', version='0.13', transitive=false)
@Grab(group='org.yaml', module='snakeyaml', version='1.18', transitive=false)

import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.lang.lifecycleGenerator
import static net.gleske.jervis.lang.lifecycleGenerator.getObjectValue

import hudson.console.HyperlinkNote
import hudson.util.Secret
import jenkins.bouncycastle.api.PEMEncodable
import jenkins.model.Jenkins
import static jenkins.bouncycastle.api.PEMEncodable.decode

/**
  Returns a list of maps which are buildable matrices in a matrix build.  This
  method takes into account that there are matrix exclusions and white lists in
  the YAML configuration.
 */
@NonCPS
List getBuildableMatrixAxes(lifecycleGenerator generator) {
    List matrix_axis_maps = generator.yaml_matrix_axes.collect { axis ->
        generator.matrixGetAxisValue(axis).split().collect {
            ["${axis}": it]
        }
    }
    if(generator.yaml_matrix_axes.size() < 2) {
        matrix_axis_maps = matrix_axis_maps[0]
    }
    else {
        //creates a list of lists which contain maps to be summed into one list of maps with every possible matrix combination
        matrix_axis_maps = matrix_axis_maps.combinations()*.sum()
    }
    //return all maps (or some maps allowed via filter)
    matrix_axis_maps.findAll {
        if(generator.matrixExcludeFilter()) {
            Binding binding = new Binding()
            it.each { k, v ->
                binding.setVariable(k, v)
            }
            //filter out the combinations (returns a boolean true or false)
            new GroovyShell(binding).evaluate(generator.matrixExcludeFilter())
        }
        else {
            //if there's no matrix exclude filter then include everything
            true
        }
    }
}

/**
  Returns a list of stashes from Jervis YAML to be stashed either serially or
  in this matrix axis for matrix builds.
 */

@NonCPS
Map getStashMap(List stashes, boolean isMatrix = false, Map matrix_axis = [:]) {
    Map stash_map = [:]
    stashes.each { s ->
        if((s instanceof Map) &&
                ('name' in s) &&
                getObjectValue(s, 'name', '') &&
                ('includes' in s) &&
                getObjectValue(s, 'includes', '') &&
                (!isMatrix || getObjectValue(s, 'matrix_axis', [:])) &&
                (!isMatrix || (getObjectValue(s, 'matrix_axis', [:]) == matrix_axis))) {
            stash_map[getObjectValue(s, 'name', '')] = [
                'name': getObjectValue(s, 'name', ''),
                'includes': getObjectValue(s, 'includes', ''),
                'excludes': getObjectValue(s, 'excludes', ''),
                'use_default_excludes': getObjectValue(s, 'use_default_excludes', 'true') == 'true',
                'allow_empty': getObjectValue(s, 'allow_empty', 'false') == 'true',
                'matrix_axis': getObjectValue(s, 'matrix_axis', [:])
                ]
        }
    }
    stash_map
}

/**
  If given a list of items, compare it to supported items for collection.
 */

@NonCPS
List getCollectItemsList(Map collect_items) {
    Set supported_collections = ['cobertura', 'junit', 'artifacts'] as Set
    Set known_items = collect_items.keySet() as Set
    (supported_collections.intersect(known_items) as List).sort()
}

/**
  Convert a matrix axis to use unfriendly names for stash comparison.
 */
@NonCPS
Map convertMatrixAxis(lifecycleGenerator generator, Map matrix_axis) {
    Map new_axis = [:]
    matrix_axis.each { k, v ->
        new_axis[k] = (generator.matrix_fullName_by_friendly[v]?:v) - ~/^${k}:/
    }
    new_axis
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
  Used by withEnvSecretWrapper() method.  Processes secret properties from
  .jervis.yml into two lists of key value pairs.
 */

@NonCPS
List processSecretEnvs(lifecycleGenerator generator) {
    List secretPairs = []
    List secretEnv = []
    generator.plainmap.each { k, v ->
        secretPairs << [var: k, password: v]
        secretEnv << "${k}=${v}"
    }
    //return a list of lists
    [secretPairs, secretEnv]
}

/**
  An environment wrapper which sets environment variables.  If available, also
  sets and masks decrypted properties from .jervis.yml.
 */
def withEnvSecretWrapper(lifecycleGenerator generator, List envList, Closure body) {
    List result = processSecretEnvs(generator)
    List secretPairs = result[0]
    List secretEnv = result[1]
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
  Automatically turn collect_items into valid stashes for non-matrix jobs.
 */
@NonCPS
List mergeCollectItemsWithStash(List stashes, Map collect_items) {
    stashes + collect_items.collect { k, v -> [name: k, includes: v] }
}

/**
  The main method of buildViaJervis()
 */
def call() {
    def generator = new lifecycleGenerator()
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
    Map tasks = [failFast: true]
    BRANCH_NAME = BRANCH_NAME?:env.GIT_BRANCH
    String is_pull_request = BRANCH_NAME.startsWith('PR-')
    env.IS_PULL_REQUEST = is_pull_request
    currentBuild.rawBuild.parent.parent.sources[0].source.with {
        github_org = it.repoOwner
        github_repo = it.repository
        github_domain = (it.apiUri)? it.apiUri.split('/')[2] : 'github.com'
    }
    jenkins_folder = currentBuild.rawBuild.parent.parent.fullName.split('/')[0]
    List jervisEnvList = [
        "JERVIS_DOMAIN=${github_domain}",
        "JERVIS_ORG=${github_org}",
        "JERVIS_PROJECT=${github_repo}",
        "JERVIS_BRANCH=${BRANCH_NAME}",
        "IS_PULL_REQUEST=${is_pull_request}"
    ]
    List secretEnvList = []

    def global_scm = scm

    stage('Process Jervis YAML') {
        platforms_json = libraryResource 'platforms.json'
        generator.loadPlatformsString(platforms_json)
        node('master') {
            checkout global_scm
            folder_listing = sh(returnStdout: true, script: 'ls -a -1').trim().split('\n') as List
            echo "Folder list: ${folder_listing}"
            if('.jervis.yml' in folder_listing) {
                jervis_yaml = readFile '.jervis.yml'
            }
            else if('.travis.yml' in folder_listing) {
                jervis_yaml = readFile '.travis.yml'
            }
            else {
                throw new FileNotFoundException('Cannot find .jervis.yml nor .travis.yml')
            }
            generator.preloadYamlString(jervis_yaml)
            os_stability = "${generator.label_os}-${generator.label_stability}"
            lifecycles_json = libraryResource "lifecycles-${os_stability}.json"
            toolchains_json = libraryResource "toolchains-${os_stability}.json"
            generator.loadLifecyclesString(lifecycles_json)
            generator.loadToolchainsString(toolchains_json)
            generator.loadYamlString(jervis_yaml)
            generator.folder_listing = folder_listing
            //attempt to get the private key else return an empty string
            String credentials_id = generator.getObjectValue(generator.jervis_yaml, 'jenkins.secrets_id', '')
            String private_key_contents = getFolderRSAKeyCredentials(jenkins_folder, credentials_id)
            if(credentials_id && !private_key_contents) {
                throw new SecurityException("Could not find private key using Jenkins Credentials ID: ${credentials_id}")
            }
            if(private_key_contents) {
                generator.setPrivateKey(private_key_contents)
                generator.decryptSecrets()
                echo "DECRYPTED PROPERTIES"
                echo printDecryptedProperties(generator, credentials_id)
            }
            //end decrypting secrets
            script_header = libraryResource "header.sh"
            script_footer = libraryResource "footer.sh"
            jervisEnvList << "JERVIS_LANG=${generator.yaml_language}"
            withEnvSecretWrapper(generator, jervisEnvList) {
                environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
                echo "PRINT ENVIRONMENT"
                echo "ENVIRONMENT:\n    ${environment_string}"
            }
        }
    }

    //prepare to run
    if(generator.isMatrixBuild()) {
        //a matrix build which should be executed in parallel
        getBuildableMatrixAxes(generator).each { matrix_axis ->
            //echo "Detected matrix axis: ${matrix_axis}"
            String stageIdentifier = matrix_axis.collect { k, v -> generator.matrix_fullName_by_friendly[v]?:v }.join('\n')
            String label = generator.labels
            List axisEnvList = matrix_axis.collect { k, v -> "${k}=${v}" }
            def stashes = (getObjectValue(generator.jervis_yaml, 'jenkins.stash', []))?: getObjectValue(generator.jervis_yaml, 'jenkins.stash', [:])
            Map stashMap = getStashMap((stashes instanceof List)? stashes : [stashes], true, convertMatrixAxis(generator, matrix_axis))
            tasks[stageIdentifier] = {
                node(label) {
                    stage("Checkout SCM") {
                        checkout global_scm
                    }
                    stage("Build axis ${stageIdentifier}") {
                        withEnvSecretWrapper(generator, axisEnvList + jervisEnvList) {
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
            }
        }
        parallel(tasks)
    }

    node(generator.labels) {
        Map collect_items = getObjectValue(generator.jervis_yaml, 'jenkins.collect', [:])
        if(!generator.isMatrixBuild()) {
            def stashes = (getObjectValue(generator.jervis_yaml, 'jenkins.stash', []))?: getObjectValue(generator.jervis_yaml, 'jenkins.stash', [:])
            stashes = mergeCollectItemsWithStash((stashes instanceof List)? stashes : [stashes], collect_items)
            Map stashMap = getStashMap(stashes)
            stage("Checkout SCM") {
                checkout global_scm
            }
            stage("Build Project") {
                withEnvSecretWrapper(generator, jervisEnvList) {
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
        List collectItemsList = getCollectItemsList(collect_items)
        if(collectItemsList) {
            stage("Publish results") {
                for(String name : collectItemsList) {
                    unstash name
                }
                if(('artifacts' in collectItemsList) && !is_pull_request) {
                    archiveArtifacts collect_items['artifacts']
                    fingerprint collect_items['artifacts']
                }
                if('cobertura' in collectItemsList) {
                    step([
                            $class: 'CoberturaPublisher',
                            autoUpdateHealth: false,
                            autoUpdateStability: false,
                            coberturaReportFile: collect_items['cobertura'],
                            failUnhealthy: false,
                            failUnstable: false,
                            maxNumberOfBuilds: 0,
                            onlyStable: false,
                            sourceEncoding: 'ASCII',
                            zoomCoverageChart: false
                    ])
                }
                if('junit' in collectItemsList) {
                    junit collect_items['junit']
                }
            }
        }
    }

}
