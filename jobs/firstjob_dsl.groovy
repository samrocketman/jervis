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

//this should only be at the top of firstjob_dsl.groovy
evaluate(readFileFromWorkspace('jobs/require_bindings.groovy').toString())
//this code should be at the beginning of every script included which requires bindings
require_bindings('jobs/firstjob_dsl.groovy', ['project'])

/*
   The main entrypoint where Job DSL plugin runs to generate jobs.  Your Job
   DSL script step should reference this file.  The rest of the files will be
   automatically loaded via this script.
 */

import jenkins.model.Jenkins
import net.gleske.jervis.remotes.GitHub
import net.gleske.jervis.exceptions.JervisException

//ensure all prerequisite plugins are installed
evaluate(readFileFromWorkspace('jobs/required_plugins.groovy').toString())

//script bindings in this file
git_service = new GitHub()
project_folder = "${project}".split('/')[0].toString()
project_name = "${project}".split('/')[1].toString()
script_approval = Jenkins.instance.getExtensionList('org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval')[0]
system_creds = Jenkins.instance.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0]
parent_job = this

//prepare bindings from other files (order does not matter)
evaluate(readFileFromWorkspace('jobs/git_service.groovy').toString())
evaluate(readFileFromWorkspace('jobs/global_threadlock.groovy').toString())
evaluate(readFileFromWorkspace('jobs/get_folder_credentials.groovy').toString())

//prepare bindings from other files (order matters due to bindings loaded from other scripts)
evaluate(readFileFromWorkspace('jobs/is_pipeline.groovy').toString())
evaluate(readFileFromWorkspace('jobs/jenkins_job_multibranch_pipeline.groovy').toString())
evaluate(readFileFromWorkspace('jobs/generate_project_for.groovy').toString())

println 'Generating jobs for ' + git_service.toString() + " project ${project}."

//create the folder because it doesn't exist, yet
if(!Jenkins.instance.getItem(project_folder)) {
    println "Creating folder ${project_folder}"
    folder(project_folder)
}

println "Creating project ${project}"

//monkey patch Thread to handle surfacing exceptions
Thread.metaClass.ex = null
Thread.metaClass.checkForException = { ->
    if(ex) {
        throw ex
    }
}

//populate project description from GitHub
job_description = git_service.fetch("repos/${project}")['description']
branches = []
default_generator = null

if(binding.hasVariable('branch') && branch) {
    throw new JervisException('Specifying a branch is no longer supported.  If you see this, then your admin should remove it from the calling job.')
}

//populate the default branch with Jervis YAML first
is_pipeline()
//iterate through all branches in parallel to discover
List <Thread> threads = []
git_service.branches(project).each { branch ->
    threads << Thread.start {
        try {
            //generating jobs fails with 'anonymous' user permissions without impersonate
            Jenkins.instance.ACL.impersonate(hudson.security.ACL.SYSTEM)
            is_pipeline(branch)
        }
        catch(Throwable t) {
            //save the caught exception to be surfaced in the job result
            ex = t
        }
    }
}
threads.each {
    it.join()
    //throws an exception from the thread
    it.checkForException()
}
generate_project_for(branches.join(' '))
