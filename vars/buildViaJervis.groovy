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

@Grab(group='net.gleske', module='jervis', version='1.1', transitive=false)
@Grab(group='org.yaml', module='snakeyaml', version='1.19', transitive=false)

import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.lang.pipelineGenerator

/**
  Process default publishable items provided by this script.
 */
def processDefaultPublishable(def item, String publishable, boolean is_pull_request) {
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


/**
  The main method of buildViaJervis()
 */
def call() {
    def global_scm = scm
    String script_footer = ''
    String script_header = ''
    BRANCH_NAME = env.CHANGE_BRANCH ?: env.BRANCH_NAME

    // Pull Request detection
    boolean is_pull_request = (env.CHANGE_ID?:false) as Boolean
    env.IS_PR_BUILD = "${is_pull_request}" as String
    //fix pull request branch name.  Otherwise shows up as PR-* as the branch name.
    if(is_pull_request) {
        env.BRANCH_NAME = env.CHANGE_BRANCH
    }

    // variables which should be injected in build environments
    List jervisEnvList = [
        "JERVIS_BRANCH=${BRANCH_NAME}",
        "IS_PR_BUILD=${is_pull_request}"
    ]
    currentBuild.rawBuild.parent.parent.sources[0].source.with {
        jervisEnvList += [
            "JERVIS_DOMAIN=${(it.apiUri)? it.apiUri.split('/')[2] : 'github.com'}",
            "JERVIS_ORG=${it.repoOwner}",
            "JERVIS_PROJECT=${it.repository}",
        ]
    }


    /*
       Jenkins pipeline stages for a build pipeline.
     */
    def generator = new lifecycleGenerator()
    generator.is_pr = is_pull_request
    def pipeline_generator = processJervisYamlStage(generator, jervisEnvList, script_header, script_footer)
    if(generator.isMatrixBuild()) {
        matrixBuildProjectStage(generator, pipeline_generator, jervisEnvList)
    }


    jervisBuildNode(generator.labels) {
        if(!generator.isMatrixBuild()) {
            Map stashMap = pipeline_generator.stashMap
            stage("Build Project") {
                checkout global_scm
                withEnvSecretWrapper(pipeline_generator, jervisEnvList) {
                    String environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
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
            stage("Publish results") {
                //unstash and publish in parallel
                Map tasks = [failFast: true]
                for(String publishable : publishableItems) {
                    String publish = publishable
                    tasks["Publish ${publish}"] = {
                        try {
                            unstash publish
                            processDefaultPublishable(pipeline_generator.getPublishable(publish), publish, is_pull_request)
                        }
                        catch(e) {
                            currentBuild.result = 'FAILURE'
                        }
                    }
                }
                parallel(tasks)
            }
        }
        if(currentBuild.result == 'FAILURE') {
            error 'This build has failed.  No user-defined pipelines will be run.'
        }
        boolean allow_user_pipelines = true
        if(hasGlobalVar('adminAllowUserPipelinesBoolean')) {
            allow_user_pipelines = adminAllowUserPipelinesBoolean() as boolean
        }
        if(generator.isPipelineJob() && allow_user_pipelines) {
            if(generator.isMatrixBuild()) {
                stage("Checkout Jenkinsfile") {
                    checkout global_scm
                }
            }
            load generator.jenkinsfile
        }
    }
}
