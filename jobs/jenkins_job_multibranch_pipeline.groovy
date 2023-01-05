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

import jenkins.model.Jenkins

//this code should be at the beginning of every script included which requires bindings
require_bindings('jobs/jenkins_job_multibranch_pipeline.groovy', ['parent_job', 'project', 'project_folder', 'project_name', 'script_approval', 'git_service'])

String getGitHubUrlPrefix() {
    def github = Jenkins.instance.with { j ->
        def clazz = j.pluginManager.uberClassLoader.findClass('org.jenkinsci.plugins.github.config.GitHubPluginConfig')
        j.getExtensionList(clazz)[0].configs.first()
    }
    String url = github.apiUrl -~ '/?(api/v3/?)?$'
    (url == 'https://api.github.com') ? 'https://github.com' : url
}

/*
   Configures a pipeline job designed to execute Jervis YAML.
 */

jenkinsJobMultibranchPipeline = null
jenkinsJobMultibranchPipeline = { List yamlFiles ->
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
                        repositoryUrl "${getGitHubUrlPrefix()}/${project_folder}/${project_name}"
                        configuredByUrl true
                        //behaviors not supported by job dsl

                        //additional behaviors
                        traits {
                            jervisFilter {
                                yamlFileName(yamlFiles.join(', '))
                            }
                        }
                    }
                }
            }
        }
        factory {
            pipelineBranchDefaultsProjectFactory {
                useSandbox true
                scriptId 'Jenkinsfile'
            }
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
