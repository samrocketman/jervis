if("${project}".size() > 0 && "${project}".split('/').length == 2) {
println("Generating jobs for GitHub project ${project}.")

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
    description("GitHub Project https://github.com/${project}")
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

def branchApi = new URL("https://api.github.com/repos/${project}/branches")
def branches = new groovy.json.JsonSlurper().parse(branchApi.newReader())
branches.each {
    def branchName = it.name
    job {
        name("${project_folder}/" + "${project}-${branchName}".replaceAll('/','-'))
        scm {
            git("git://github.com/${project}.git", branchName)
        }
        steps {
            shell("echo 'Hello world! ${project}/${branchName}'")
        }
    }
}
} else {
    throw new ScriptException('Job parameter "project" must be specified correctly!  It\'s value is a GitHub project in the form of ${namespace}/${project}.  For example, the value for the jenkinsci organization jenkins project would be set as "jenkinsci/jenkins".')
}
