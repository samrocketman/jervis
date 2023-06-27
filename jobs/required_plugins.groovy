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
/*
   Detect required plugins to successfully use these Job DSL scripts.

   Any missing plugins will fail everything and warn the admin they need to install plugins
 */
import jenkins.model.Jenkins
import net.gleske.jervis.exceptions.JervisException

void detect(Set required_plugins) {
    Set installed_plugins = (Jenkins.instance.pluginManager.plugins*.shortName).toSet()
    Set missing_plugins = required_plugins - installed_plugins
    if(missing_plugins) {
        throw new JervisException("ERROR: Action required by Admin.  Jenkins is missing required plugins: ${missing_plugins.join(', ')}")
    }
}

//require in the future: ghprb, groovy-postbuild
Set required_plugins = [
    'bouncycastle-api',
    'cloudbees-folder',
    'cobertura',
    'credentials',
    'git',
    'github',
    'groovy',
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
