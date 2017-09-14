/*
   Detect required plugins to successfully use these Job DSL scripts.

   Any missing plugins will fail everything and warn the admin they need to install plugins
 */
import jenkins.model.Jenkins

void detect(Set required_plugins) {
    Set installed_plugins = (Jenkins.instance.pluginManager.plugins*.shortName).toSet()
    Set missing_plugins = required_plugins - installed_plugins
    if(missing_plugins) {
        throw new Exception("ERROR: Action required by Admin.  Jenkins is missing required plugins: ${missing_plugins.join(', ')}")
    }
}

Set required_plugins = [
    'bouncycastle-api',
    'cloudbees-folder',
    'cobertura',
    'covcomplplot',
    'credentials',
    'ghprb',
    'git',
    'github',
    'groovy',
    'groovy-label-assignment',
    'groovy-postbuild',
    'javadoc',
    'job-dsl',
    'junit',
    'matrix-auth',
    'matrix-project',
    'plain-credentials',
    'rich-text-publisher-plugin',
    'ssh-credentials',
    'view-job-filters',
    'workflow-aggregator'
    ].toSet()

detect required_plugins
