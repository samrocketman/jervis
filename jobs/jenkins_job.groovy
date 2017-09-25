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
String include_script_name = 'jobs/git_service.groovy'
Set required_bindings = ['parent_job', 'project', 'project_folder', 'project_name', 'script_approval', 'git_service']
Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
if(missing_bindings) {
    throw new Exception("${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}")
}

/*
   Configures matrix or freestyle jobs for both main and pull request builds.
 */

import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.remotes.GitHub
import static net.gleske.jervis.lang.lifecycleGenerator.getObjectValue

jenkinsJob = null
jenkinsJob = { lifecycleGenerator generator, boolean isPullRequestJob, String JERVIS_BRANCH ->
    //chooses job type based on Jervis YAML
    def jervis_jobType
    if(generator.isMatrixBuild()) {
        jervis_jobType = { String name, Closure closure -> parent_job.matrixJob(name, closure) }
    }
    else {
        jervis_jobType = { String name, Closure closure -> parent_job.freeStyleJob(name, closure) }
    }
    println "Generating branch: ${JERVIS_BRANCH}"
    jenkinsJobClassic(jervis_jobType, generator, isPullRequestJob, JERVIS_BRANCH)
}
