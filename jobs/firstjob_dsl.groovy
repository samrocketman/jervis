/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.remotes.GitHub

def git_service = new GitHub()
//Pre-job setup based on the type of remote
switch(git_service) {
    case GitHub:
        //authenticate
        if(System.getenv('GITHUB_TOKEN')) {
            println 'Found GITHUB_TOKEN environment variable.'
            git_service.gh_token = System.getenv('GITHUB_TOKEN')
        }
        //GitHub Enterprise web URL; otherwise it will simply be github.com
        if(System.getenv('GITHUB_URL')) {
            println 'Found GITHUB_URL environment variable.'
            git_service.gh_web = System.getenv('GITHUB_URL')
        }
}

if("${project}".size() > 0 && "${project}".split('/').length == 2) {
    println 'Generating jobs for ' + git_service.toString() + " project ${project}."

    project_folder = "${project}".split('/')[0]
    project_name = "${project}".split('/')[1]

    if(! new File("${JENKINS_HOME}/jobs/${project_folder}/config.xml").exists()) {
        println "Creating folder ${project_folder}"
        folder(project_folder) {
            //displayName('some display name')
        }
    }

    println "Creating project ${project}"
    listView("${project}") {
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

    git_service.branches("${project}").each {
        def JERVIS_BRANCH = it
        def folder_listing = git_service.getFolderListing(project, '/', JERVIS_BRANCH)
        def generator = new lifecycleGenerator()
        String jervis_yaml
        if('.jervis.yml' in folder_listing) {
            jervis_yaml = git_service.getFile(project, '.jervis.yml', JERVIS_BRANCH)
        }
        else if('.travis.yml' in folder_listing) {
            jervis_yaml = git_service.getFile(project, '.travis.yml', JERVIS_BRANCH)
        }
        else {
            //skip creating the job for this branch
            println "Skipping branch: ${JERVIS_BRANCH}"
            return
        }
        //try detecting no default language and setting to ruby
        if(jervis_yaml.indexOf('language:') < 0) {
            generator.yaml_language = 'ruby'
        }
        generator.loadPlatformsString(readFileFromWorkspace('src/main/resources/platforms.json').toString())
        generator.preloadYamlString(jervis_yaml)
        //could optionally read lifecycles and toolchains files by OS
        generator.loadLifecyclesString(readFileFromWorkspace('src/main/resources/lifecycles.json').toString())
        generator.loadToolchainsString(readFileFromWorkspace('src/main/resources/toolchains.json').toString())
        generator.loadYamlString(jervis_yaml)
        generator.folder_listing = folder_listing
        if(!generator.isGenerateBranch(JERVIS_BRANCH)) {
            //the job should not be generated for this branch
            //based on the branches section of .jervis.yml
            println "Skipping branch: ${JERVIS_BRANCH}"
            return
        }
        //chooses job type based on Jervis YAML
        def jobType
        if(generator.isMatrixBuild()) {
            jervis_jobType = { String name, Closure closure -> matrixJob(name, closure) }
        }
        else {
            jervis_jobType = { String name, Closure closure -> freeStyleJob(name, closure) }
        }
        println "Generating branch: ${JERVIS_BRANCH}"
        //the generated Job DSL enclosure depends on the job type
        jervis_jobType("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-')) {
            displayName("${project_name} (${JERVIS_BRANCH} branch)")
            label(generator.getLabels())
            scm {
                //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
                //for more info about the git closure
                git {
                    remote {
                        url(git_service.getCloneUrl() + "${project}.git")
                    }
                    branch(JERVIS_BRANCH)
                    shallowClone(true)
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
                    readFileFromWorkspace('assets/header.sh'),
                    "export JERVIS_LANG=\"${generator.yaml_language}\"",
                    "export JERVIS_PROJECT=\"${project_name}\"",
                    "export JERVIS_BRANCH=\"${JERVIS_BRANCH}\"",
                    generator.generateAll(),
                    readFileFromWorkspace('assets/footer.sh')
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
        }
    }
}
else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
