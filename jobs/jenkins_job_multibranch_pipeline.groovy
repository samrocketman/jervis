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

/*
   Configures a pipeline job designed to execute Jervis YAML.
 */

import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.remotes.GitHub
import static net.gleske.jervis.lang.lifecycleGenerator.getObjectValue

jenkinsJobMultibranchPipeline = null
jenkinsJobMultibranchPipeline = { def jervis_jobType, lifecycleGenerator generator, String JERVIS_BRANCH ->
    //the generated Job DSL enclosure depends on the job type
    jervis_jobType("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-')) {
        displayName("${project_name} (${JERVIS_BRANCH} branch)")
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
                            headWildcardFilter {
                                includes "${JERVIS_BRANCH} PR-*"
                                excludes ''
                            }
                            refSpecsSCMSourceTrait {
                                templates {
                                    refSpecTemplate {
                                        // A ref spec to fetch.
                                        value '+refs/pull/*/head:refs/heads/PR-*'
                                    }
                                }
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
            def factory = it / factory(class: 'org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory')
            factory << owner(class: 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject', reference: '../..')
            factory << scriptPath(getObjectValue(generator.jervis_yaml, 'jenkins.pipeline_jenkinsfile', 'Jenkinsfile'))
        }
        configure {
            def folderConfig = it / 'properties' / 'org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig'
            folderConfig << dockerLabel('docker')
            folderConfig << registry()
        }
        configure {
            def traits = it.sources.data.'jenkins.branch.BranchSource'.source.traits
            traits[0].children() << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' { strategyId('3') }
            traits[0].children() << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' { strategyId('1') }
            traits[0].children() << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
                strategyId('1')
                trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustEveryone')
            }
        }
        /*
        configure {
            def github = it /
                sources(class: 'jenkins.branch.MultiBranchProject$BranchSourceList') /
                data /
                'jenkins.branch.BranchSource' /
                source(class: 'org.jenkinsci.plugins.github_branch_source.GitHubSCMSource')
            traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' { strategyId('3') }
            traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' { strategyId('1') }
            traits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
                strategyId('1')
                trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustEveryone')
            }
        }
        */
    }
}
