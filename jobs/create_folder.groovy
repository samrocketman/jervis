/*
   Copyright 2014-2024 Sam Gleske - https://github.com/samrocketman/jervis

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
   Create a folder in which jobs will be grouped based on GitHub organization
   or user.
 */
require_bindings('jobs/create_folder.groovy', ['parent_job', 'project_folder'])

import jenkins.model.Jenkins

if(!Jenkins.instance.getItem(project_folder)) {
    println "Creating folder ${project_folder}"
    parent_job.folder(project_folder)
}
