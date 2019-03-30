/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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

    // JERVIS INJECTED ENVIRONMENT VARIABLES
    // Pull Request detection
    env.IS_PR_BUILD = isPRBuild().toString()
    // Tag detection
    env.IS_TAG_BUILD = isTagBuild().toString()
    currentBuild.rawBuild.parent.parent.sources[0].source.with {
        env.JERVIS_DOMAIN = ((it.apiUri)? it.apiUri.split('/')[2] : 'github.com')
        env.JERVIS_ORG = it.repoOwner
        env.JERVIS_PROJECT =it.repository
    }
    env.JERVIS_BRANCH = BRANCH_NAME
    //fix pull request branch name.  Otherwise shows up as PR-* as the branch name.
    if(isPRBuild()) {
        env.BRANCH_NAME = env.CHANGE_BRANCH
    }

    /*
       Jenkins pipeline stages for a build pipeline.
     */
    def generator = new lifecycleGenerator()
    generator.is_pr = isPRBuild()
    def pipeline_generator
    String script_header
    String script_footer
    processJervisYamlStage(generator) {
        pipeline_generator = it
        script_header = loadCustomResource "header.sh"
        script_footer = loadCustomResource "footer.sh"
    }
    if(generator.isMatrixBuild()) {
        // this occurs in parallel across multiple build nodes (1 node per axis)
        matrixBuildProjectStage(global_scm, generator, pipeline_generator, script_header, script_footer)
    }


    jervisBuildNode(pipeline_generator, generator.labels) {
        if(!generator.isMatrixBuild()) {
            try {
                buildProjectStage(global_scm, generator, pipeline_generator, script_header, script_footer)
            }
            catch(e) {
                currentBuild.result = 'FAILURE'
            }
        }
        publishResultsStage(generator, pipeline_generator)

        if(currentBuild.result == 'FAILURE') {
            error 'This build has failed.  No user-defined pipelines will be run.'
        }
    }
    pipeline_generator
}
