@Grab(group='org.yaml', module='snakeyaml', version='1.14')

import jervis.remotes.GitHub

def remote = new GitHub()

if("${project}".size() > 0 && "${project}".split('/').length == 2) {
println("Generating jobs for " + remote.toString() + " project ${project}.")

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
    description(remote.toString() + "Project " + remote.getWebEndpoint() + "${project}")
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex("^" + "${project}".replaceAll('/','-') + ".*")
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

remote.branches("${project}").each {
    def branchName = it
    job {
        name("${project_folder}/" + "${project}-${branchName}".replaceAll('/','-'))
        scm {
            //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
            //for more info about the git closure
            git {
                remote {
                    url(remote.getCloneUrl() + "${project}.git")
                }
                branch(branchName)
                shallowClone(true)
                switch(remote) {
                    case GitHub:
                        configure { gitHub ->
                            gitHub / browser(class: "hudson.plugins.git.browser.GithubWeb") {
                                url(remote.getWebEndpoint() + "${project}")
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
} else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
