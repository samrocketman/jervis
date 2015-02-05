@Grab(group='org.yaml', module='snakeyaml', version='1.14')

import jervis.remotes.GitHub

def git_service = new GitHub()

if("${project}".size() > 0 && "${project}".split('/').length == 2) {
    println("Generating jobs for " + git_service.toString() + " project ${project}.")

    project_folder = "${project}".split('/')[0]
    project_name = "${project}".split('/')[1]

    if(! new File("${JENKINS_HOME}/jobs/${project_folder}/config.xml").exists()) {
        println("Creating folder ${project_folder}")
        folder {
            name(project_folder)
        }
    }

    println("Creating project ${project}")
    view(type: ListView) {
        name("${project}")
        description(git_service.toString() + "Project " + git_service.getWebUrl() + "${project}")
        filterBuildQueue()
        filterExecutors()
        jobs {
            regex("^" + "${project_name}".replaceAll('/','-') + ".*")
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
        def branchName = it
        job {
            name("${project_folder}/" + "${project_name}-${branchName}".replaceAll('/','-'))
            scm {
                //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
                //for more info about the git closure
                git {
                    remote {
                        url(git_service.getCloneUrl() + "${project}.git")
                    }
                    branch(branchName)
                    shallowClone(true)
                    switch(git_service) {
                        case GitHub:
                            configure { gitHub ->
                                gitHub / browser(class: "hudson.plugins.git.browser.GithubWeb") {
                                    url(git_service.getWebUrl() + "${project}")
                                }
                            }
                    }
                }
            }
            steps {
                shell("echo 'Hello world! ${project}/${branchName}'")
            }
        }
    }
}
else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
