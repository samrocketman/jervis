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
String include_script_name = 'jobs/jenkins_job_pipeline.groovy'
Set required_bindings = ['parent_job', 'project', 'project_folder', 'project_name', 'script_approval', 'git_service']
Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
if(missing_bindings) {
    throw new Exception("${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}")
}

jenkinsJobPipeline = null
jenkinsJobPipeline = { def jervis_jobType, lifecycleGenerator generator, String JERVIS_BRANCH ->
    //the generated Job DSL enclosure depends on the job type
    jervis_jobType("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-')) {
        displayName("${project_name} (${JERVIS_BRANCH} branch)")
        label(generator.labels)

        scm {
            //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
            //for more info about the git closure
            git {
                remote {
                    url(git_service.getCloneUrl() + "${project}.git")
                }
                branch("refs/heads/${JERVIS_BRANCH}")
                //configure git web browser based on the type of remote
                switch(git_service) {
                    case GitHub:
                        configure { gitHub ->
                            gitHub / browser(class: 'hudson.plugins.git.browser.GithubWeb') {
                                url(git_service.getWebUrl() + "${project}")
                            }
                        }
                }
            }
        }
		definition {
			cps {
				script('buildViaJervis()')
				sandbox()
			}
		}
    }
}
