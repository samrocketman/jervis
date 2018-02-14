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
String include_script_name = 'jobs/jenkins_job_multibranch_pipeline.groovy'
Set required_bindings = ['parent_job', 'project', 'project_folder', 'project_name', 'script_approval', 'git_service']
Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
if(missing_bindings) {
    throw new Exception("${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}")
}

/*
   Configures a pipeline job designed to execute Jervis YAML.
 */

jenkinsJobMultibranchPipeline = null
jenkinsJobMultibranchPipeline = { def jervis_jobType, String JERVIS_BRANCH ->
    //the generated Job DSL enclosure depends on the job type
    jervis_jobType(project) {
        description(job_description)
        //displayName(project_name)
        branchSources {
            branchSource {
                source {
                    github {
                        //github
                        credentialsId 'github-user-and-token'
                        repoOwner project_folder
                        repository project_name
                        //behaviors not supported by job dsl

                        //additional behaviors
                        traits {
                            headWildcardFilterWithPR {
                                includes "${JERVIS_BRANCH}"
                                excludes ''
                            }
                        }
                    }
                }
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                numToKeep(20)
            }
        }
        configure {
            def factory = it / factory(class: 'org.jenkinsci.plugins.pipeline.multibranch.defaults.PipelineBranchDefaultsProjectFactory')
            factory << owner(class: 'org.jenkinsci.plugins.pipeline.multibranch.defaults.PipelineMultiBranchDefaultsProject', reference: '../..')
        }
        configure {
            def folderConfig = it / 'properties' / 'org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig'
            folderConfig << dockerLabel('docker')
            folderConfig << registry()
        }
        configure {
            def traits = it.sources.data.'jenkins.branch.BranchSource'.source.traits
            traits[0].children().add 0, 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
                strategyId('1')
                trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustEveryone')
            }
            traits[0].children().add 0, 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' { strategyId('1') }
            traits[0].children().add 0, 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' { strategyId('3') }
        }
    }
}
