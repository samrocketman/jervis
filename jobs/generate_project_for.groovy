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

//this code should be at the beginning of every script included which requires bindings
require_bindings('jobs/generate_project_for.groovy', ['parent_job', 'git_service', 'project', 'project_folder', 'global_threadlock'])

/*
   This will generate jobs for a given branch.
 */

import net.gleske.jervis.lang.lifecycleGenerator

//generate Jenkins jobs
generate_project_for = null
generate_project_for = { String JERVIS_BRANCH ->
    //Job DSL job generation must happen serially; race conditions cause unexpected behavior
    global_threadlock.withLock {
        jenkinsJobMultibranchPipeline(JERVIS_BRANCH)
    }
}
