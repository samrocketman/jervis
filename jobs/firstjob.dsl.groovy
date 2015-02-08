@Grab(group='org.yaml', module='snakeyaml', version='1.14')

import jervis.lang.lifecycleGenerator
import jervis.remotes.GitHub
import jervis.tools.scmGit


println "The current working directory is: ${new File(".").getAbsolutePath()}"
println System.getenv("WORKSPACE")

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

    def git = new scmGit()
    print 'Git root: '
    println git.getRoot()

    git_service.branches("${project}").each {
        def JERVIS_BRANCH = it
        def folder_listing = git_service.getFolderListing(project, '/', JERVIS_BRANCH)
        def generator = new lifecycleGenerator()
        generator.loadLifecycles("${git.getRoot()}/src/main/resources/lifecycles.json")
        generator.loadToolchains("${git.getRoot()}/src/main/resources/toolchains.json")
        if(".jervis.yml" in folder_listing) {
            generator.loadYaml(git_service.getFile(project, "/.jervis.yml", JERVIS_BRANCH))
        }
        else if(".travis.yml" in folder_listing) {
            generator.loadYaml(git_service.getFile(project, "/.travis.yml", JERVIS_BRANCH))
        }
        else {
            //skip creating the job for this branch
            return
        }
        generator.folder_listing = folder_listing
        job {
            name("${project_folder}/" + "${project_name}-${JERVIS_BRANCH}".replaceAll('/','-'))
            scm {
                //see https://github.com/jenkinsci/job-dsl-plugin/pull/108
                //for more info about the git closure
                git {
                    remote {
                        url(git_service.getCloneUrl() + "${project}.git")
                    }
                    branch(JERVIS_BRANCH)
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
                shell(generator.generateAll())
            }
        }
    }
}
else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
