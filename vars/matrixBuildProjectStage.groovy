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
   This stage builds a matrix of builds in parallel.
 */

import net.gleske.jervis.lang.LifecycleGenerator
import net.gleske.jervis.lang.PipelineGenerator

def call(def global_scm, LifecycleGenerator generator, PipelineGenerator pipeline_generator, String script_header, String script_footer) {
    Map tasks = [failFast: true]
    pipeline_generator.buildableMatrixAxes.each { matrix_axis ->
        String stageIdentifier = matrix_axis.collect { k, v -> generator.matrix_fullName_by_friendly[v]?:v }.join('\n')
        String label = generator.labels
        List axisEnvList = matrix_axis.collect { k, v -> "${k}=${v}" }
        Map stashMap = pipeline_generator.getStashMap(matrix_axis)
        tasks[stageIdentifier] = {
            stage("Build axis ${stageIdentifier}") {
                jervisBuildNode(pipeline_generator, label) {
                    Boolean failed_stage = false
                    withEnvSecretWrapper(pipeline_generator, axisEnvList) {
                        String environment_string = sh(script: 'env | LC_ALL=C sort', returnStdout: true).split('\n').join('\n    ')
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
                            echo "Stashing ${name}; includes: '${stashMap[name]['includes']}'"
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
    if(generator.isMatrixBuild()) {
        stage("Build Project") {
            parallel(tasks)
        }
    }
}
