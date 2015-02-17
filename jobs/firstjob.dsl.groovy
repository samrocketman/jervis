@Grab(group='org.yaml', module='snakeyaml', version='1.14')

import jervis.lang.lifecycleGenerator
import jervis.remotes.GitHub

def git_service = new GitHub()
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

if("${project}".size() > 0 && "${project}".split('/').length == 2) {
    println 'Generating jobs for ' + git_service.toString() + " project ${project}."

    project_folder = "${project}".split('/')[0]
    project_name = "${project}".split('/')[1]

    if(! new File("${JENKINS_HOME}/jobs/${project_folder}/config.xml").exists()) {
        println "Creating folder ${project_folder}"
        folder {
            name(project_folder)
        }
    }

    println "Creating project ${project}"
    view(type: ListView) {
        name("${project}")
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
        println "Generating branch: ${JERVIS_BRANCH}"
        def folder_listing = git_service.getFolderListing(project, '/', JERVIS_BRANCH)
        def generator = new lifecycleGenerator()
        generator.loadLifecyclesString(readFileFromWorkspace('src/main/resources/lifecycles.json').toString())
        generator.loadToolchainsString(readFileFromWorkspace('src/main/resources/toolchains.json').toString())
        String jervis_yaml
        if('.jervis.yml' in folder_listing) {
            jervis_yaml = git_service.getFile(project, '.jervis.yml', JERVIS_BRANCH)
        }
        else if('.travis.yml' in folder_listing) {
            jervis_yaml = git_service.getFile(project, '.travis.yml', JERVIS_BRANCH)
        }
        else {
            //skip creating the job for this branch
            return
        }
        //try detecting no default language and setting to ruby
        if(jervis_yaml.indexOf('language:') < 0) {
            generator.yaml_language = 'ruby'
        }
        generator.loadYamlString(jervis_yaml)
        generator.folder_listing = folder_listing
        def jobType
        if(generator.isMatrixBuild()) {
            jobType = Matrix
        }
        else {
            jobType = Freeform
        }
        job(type: jobType) {
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
                                gitHub / browser(class: 'hudson.plugins.git.browser.GithubWeb') {
                                    url(git_service.getWebUrl() + "${project}")
                                }
                            }
                    }
                }
            }
            steps {
                shell(generator.generateAll())
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
