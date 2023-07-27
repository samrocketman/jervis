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

import net.gleske.jervis.lang.LifecycleGenerator

@NonCPS
void setupJervisEnvironment() {
    BRANCH_NAME = env.CHANGE_BRANCH ?: env.BRANCH_NAME

    // JERVIS INJECTED ENVIRONMENT VARIABLES
    // Pull Request detection
    env.IS_PR_BUILD = isBuilding(['pr']).toString()
    // Branch detection
    env.IS_BRANCH_BUILD = isBuilding(['branch']).toString()
    // Tag detection
    env.IS_TAG_BUILD = isBuilding(['tag']).toString()
    // Build was triggered by cron timer
    env.IS_CRON_BUILD = isBuilding(['cron']).toString()
    // Build was triggered by PR comment
    env.IS_PR_COMMENT = isBuilding(['pr_comment']).toString()
    // Someone clicked the build button
    env.IS_MANUAL_BUILD = isBuilding(['manually']).toString()
    currentBuild.rawBuild.parent.parent.sources[0].source.with {
        env.JERVIS_DOMAIN = ((it.apiUri)? it.apiUri.split('/')[2] : 'github.com')
        env.JERVIS_ORG = it.repoOwner
        env.JERVIS_PROJECT =it.repository
    }
    env.JERVIS_BRANCH = BRANCH_NAME
    // Fix pull request branch name.  Otherwise shows up as PR-* as the branch
    // name.
    if(isBuilding('pr')) {
        env.BRANCH_NAME = env.CHANGE_BRANCH
    }
}

/**
  call() is the main method of buildViaJervis()
 */
def call() {
    if(hasGlobalVar('adminInitialSetup')) {
        adminInitialSetup()
    }
    def global_scm = scm
    setupJervisEnvironment()

    /*
       Jenkins pipeline stages for a build pipeline.
     */
    def generator = new LifecycleGenerator()
    generator.is_pr = isBuilding('pr')
    def pipeline_generator
    String script_header
    String script_footer
    processJervisYamlStage(generator) {
        pipeline_generator = it
        script_header = loadCustomResource "header.sh"
        script_footer = loadCustomResource "footer.sh"
    }
    if(hasGlobalVar('adminCustomizePipelineGenerator')) {
        pipeline_generator = adminCustomizePipelineGenerator(pipeline_generator)
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
    setUserBinding('jervis_global_pipeline_generator', pipeline_generator)
    pipeline_generator
}
