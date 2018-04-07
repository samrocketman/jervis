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
require_bindings('jobs/jenkins_job_multibranch_pipeline.groovy', ['parent_job', 'project', 'project_folder', 'project_name', 'script_approval', 'git_service'])

/*
   Configures a pipeline job designed to execute Jervis YAML.
 */

jenkinsJobMultibranchPipeline = null
jenkinsJobMultibranchPipeline = { String JERVIS_BRANCH ->
    //uses groovy bindings to properly reference the Job DSL; in this case parent_job
    parent_job.multibranchPipelineJob(project) {
        description(job_description)
        //displayName(project_name)
        branchSources {
            branchSource {
                source {
                    github {
                        //github
                        id "owner-${project_folder}:repo-${project_name}"
                        credentialsId 'github-user-and-token'
                        repoOwner project_folder
                        repository project_name
                        //behaviors not supported by job dsl

                        //additional behaviors
                        traits {
                            if(default_generator && default_generator.filter_type == 'only' && default_generator.hasRegexFilter()) {
                                headRegexFilterWithPR {
                                    regex default_generator.getFullBranchRegexString(JERVIS_BRANCH.split(' ') as List)
                                }
                            }
                            else {
                                headWildcardFilterWithPR {
                                    includes "${JERVIS_BRANCH}"
                                    excludes ''
                                }
                            }
                        }
                    }
                }
                /* prevented webhooks from building
                strategy {
                    defaultBranchPropertyStrategy {
                        props {
                            noTriggerBranchProperty()
                        }
                    }
                }
                */
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                numToKeep(20)
            }
        }
        configure {
            //overwrite default factory with the pipeline multibranch defaults plugin "Global Jenkinsfile"
            Node factoryNode = it / factory
            factoryNode.attributes().put 'class', 'org.jenkinsci.plugins.pipeline.multibranch.defaults.PipelineBranchDefaultsProjectFactory'
            (factoryNode / 'owner').attributes().put 'class', 'org.jenkinsci.plugins.pipeline.multibranch.defaults.PipelineMultiBranchDefaultsProject'
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
        /* not necessary but leaving for now
        configure {
            it / triggers / 'com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger' {
                spec('H H * * *')
                interval('86400000')
            }
        }
        */
    }
}
