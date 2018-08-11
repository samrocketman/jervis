/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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

/**
  This pipeline var will attempt to load resources from other pipeline
  libraries and the global config files plugin first.  If all else fails then
  it will attempt to load resources from this library as a last resort.
 */

import jenkins.model.Jenkins
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles

/**
  Gets a library resource.  A resource can be loaded from an external library
  provided by an admin, a config file defined in global settings, or return
  null if we couldn't find a custom resource.
 */
@NonCPS
String tryLoadCustomResource(String resource) {
    def config_files = Jenkins.instance.getExtensionList(GlobalConfigFiles)[0]
    if(hasGlobalVar('adminLibraryResource')) {
        adminLibraryResource(resource)
        echo "Loaded resource ${resource} from adminLibraryResource."
    }
    else if(config_files.getById(resource)) {
        config_files.getById(resource).content
        echo "Loaded resource ${resource} from global config files."
    }
    else {
        null
    }
}

String call(String resource) {
    tryLoadCustomResource(resource) ?: libraryResource(resource)
}
