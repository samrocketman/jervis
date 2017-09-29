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
String include_script_name = 'jobs/firstjob_dsl.groovy'
Set required_bindings = ['project', 'branch']
Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
if(missing_bindings || !project || project.split('/').size() != 2) {
    String message = """
       |${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}
       |
       |Job parameter "project" must be specified correctly!  Its value is a
       |GitHub project in the form of ${namespace}/${project}.  For example,
       |the value for the jenkinsci organization jenkins project would be
       |set as "jenkinsci/jenkins".""".stripMargin().trim().toString()
    throw new Exception(message)
}

/*
   The main entrypoint where Job DSL plugin runs to generate jobs.  Your Job
   DSL script step should reference this file.  The rest of the files will be
   automatically loaded via this script.
 */

import jenkins.model.Jenkins
import net.gleske.jervis.remotes.GitHub

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
evaluate(readFileFromWorkspace('jobs/jenkins_job_classic.groovy').toString())
evaluate(readFileFromWorkspace('jobs/jenkins_job_pipeline.groovy').toString())
evaluate(readFileFromWorkspace('jobs/jenkins_job_multibranch_pipeline.groovy').toString())
evaluate(readFileFromWorkspace('jobs/jenkins_job.groovy').toString())
evaluate(readFileFromWorkspace('jobs/generate_project_for.groovy').toString())

println 'Generating jobs for ' + git_service.toString() + " project ${project}."

//create the folder because it doesn't exist, yet
if(!Jenkins.instance.getItem(project_folder)) {
    println "Creating folder ${project_folder}"
    folder(project_folder)
}

println "Creating project ${project}"

listView(project.toString()) {
    description(git_service.toString() + ' Project ' + git_service.getWebUrl() + "${project}")
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex('^' + "${project_name}".replaceAll('/','-') + '.*')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

//monkey patch Thread to handle surfacing exceptions
Thread.metaClass.ex = null
Thread.metaClass.checkForException = { ->
    if(ex) {
        throw ex
    }
}

//populate project description from GitHub
job_description = git_service.fetch("repos/${project}")['description']

//generate projects for one or more branches
if(branch) {
    generate_project_for(branch)
}
else {
    List <Thread> threads = []
    branches = []
    pipeline_jenkinsfile = ''
    is_pipeline()
    git_service.branches(project).each { branch ->
        threads << Thread.start {
            try {
                //generating jobs fails with 'anonymous' user permissions without impersonate
                Jenkins.instance.ACL.impersonate(hudson.security.ACL.SYSTEM)
                if(pipeline_jenkinsfile) {
                    is_pipeline(branch)
                }
                else {
                    generate_project_for(branch)
                }
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
    if(pipeline_jenkinsfile) {
        generate_project_for(branches.join(' '))
    }
}
