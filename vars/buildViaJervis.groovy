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

@Grab(group='net.gleske', module='jervis', version='1.2', transitive=false)
@Grab(group='org.yaml', module='snakeyaml', version='1.21', transitive=false)

import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.lang.pipelineGenerator

/**
  call() is the main method of buildViaJervis()
 */
def call() {
    def global_scm = scm
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
    def pipeline_generator
    String script_header
    String script_footer
    processJervisYamlStage(generator, jervisEnvList) {
        pipeline_generator = it
        script_header = loadCustomResource "header.sh"
        script_footer = loadCustomResource "footer.sh"
    }
    if(generator.isMatrixBuild()) {
        // this occurs in parallel across multiple build nodes (1 node per axis)
        matrixBuildProjectStage(global_scm, generator, pipeline_generator, jervisEnvList, script_header, script_footer)
    }


    jervisBuildNode(pipeline_generator, generator.labels) {
        if(!generator.isMatrixBuild()) {
            buildProjectStage(global_scm, generator, pipeline_generator, jervisEnvList, script_header, script_footer)
        }
        publishResultsStage(generator, pipeline_generator)

        if(currentBuild.result == 'FAILURE') {
            error 'This build has failed.  No user-defined pipelines will be run.'
        }
        /*
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
        */
    }
    pipeline_generator
}
