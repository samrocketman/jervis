/*
   Copyright 2014-2016 Sam Gleske - https://github.com/samrocketman/jervis

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

import hudson.util.Secret
import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.remotes.GitHub

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
    //the generated Job DSL enclosure depends on the job type
    jervis_jobType("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-')) {
        displayName("${project_name} (${JERVIS_BRANCH} branch)")
        label(generator.getLabels())
        if(generator.isMatrixBuild()) {
            //workaround for matrix builds ref: https://github.com/jenkinsci/docker-plugin/issues/242
            properties {
                groovyLabelAssignmentProperty {
                    secureGroovyScript {
                        String groovyscript = "return currentJob.getClass().getSimpleName().equals('MatrixProject') ? 'master' : '${generator.getLabels()}'"
                        script(groovyscript)
                        sandbox(false)
                        //workaround for https://issues.jenkins-ci.org/browse/JENKINS-46016
                        script_approval.approveScript(script_approval.hash(groovyscript, 'groovy'))
                    }
                }
            }
        }
        //configure encrypted properties
        if(generator.plainlist.size() > 0) {
            configure { project ->
                project / 'buildWrappers' / 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper' / 'varPasswordPairs'() {
                    generator.plainlist.each { pair ->
                        'varPasswordPair'(var: pair['key'], password: Secret.fromString(pair['secret']).getEncryptedValue())
                    }
                }
            }
        }
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
        steps {
            shell([
                parent_job.readFileFromWorkspace('assets/header.sh'),
                "export JERVIS_LANG=\"${generator.yaml_language}\"",
                "export JERVIS_DOMAIN=\"${git_service.getWebUrl().split('/')[2]}\"",
                "export JERVIS_ORG=\"${project_folder}\"",
                "export JERVIS_PROJECT=\"${project_name}\"",
                "export JERVIS_BRANCH=\"${JERVIS_BRANCH}\"",
                "export IS_PULL_REQUEST=\"${isPullRequestJob}\"",
                generator.generateAll(),
                parent_job.readFileFromWorkspace('assets/footer.sh')
                ].join('\n'))
        }
        //if a matrix build then generate matrix bits
        if(generator.isMatrixBuild()) {
            axes {
                generator.yaml_matrix_axes.each {
                    text(it, generator.matrixGetAxisValue(it).split())
                }
            }
            combinationFilter(generator.matrixExcludeFilter())
        }
        publishers {
            String[] enabled_collections = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect', [:]).keySet() as String[]
            if('artifacts' in enabled_collections) {
                //artifact lists as a single string or a list in YAML
                def collect_artifacts = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.artifacts', new Object())
                collect_artifacts = (collect_artifacts instanceof List)? collect_artifacts.join(',') : collect_artifacts.toString()
                if(collect_artifacts.size() > 1) {
                    archiveArtifacts {
                        fingerprint(true)
                        onlyIfSuccessful(true)
                        pattern(collect_artifacts)
                    }
                }
            }
            if('junit' in enabled_collections) {
                String collect_junit = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.junit', '')
                if(collect_junit.size() > 0) {
                    archiveJunit(collect_junit)
                }
            }
            if('cobertura' in enabled_collections) {
                String collect_cobertura = generator.getObjectValue(generator.jervis_yaml, 'jenkins.collect.cobertura', '')
                if(collect_cobertura.size() > 0) {
                    cobertura(collect_cobertura)
                    covComplPlotPublisher {
                        analyzer 'Cobertura'
                        excludeGetterSetter false
                        verbose false
                        locateTopMost true
                    }
                }
            }
        }
    }
}
