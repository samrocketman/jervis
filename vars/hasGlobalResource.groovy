/*
   Copyright 2014-2024 Sam Gleske - https://github.com/samrocketman/jervis

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
  hasGlobalResource will check to see if a file exists in the resources
  directory of any shared library.  This will allow for conditional
  libraryResource steps.  Skip attempting to load the resource if it doesn't
  exist.
  */
import hudson.model.Run
import org.jenkinsci.plugins.workflow.libs.LibrariesAction
import org.jenkinsci.plugins.workflow.libs.LibraryRecord

/**
  Check if a global library provides a resource.
 */
@NonCPS
Boolean lookForGlobalLibraryResource(Run run, String resourceFile) {
    def action = run.getAction(LibrariesAction)
    action.libraries.any { LibraryRecord library ->
        File file = new File(run.getRootDir(), "libs/${library.name}/resources/${resourceFile}")
        file.exists()
    }
}

@NonCPS
Boolean call(String resourceFile) {
    lookForGlobalLibraryResource(currentBuild.rawBuild, resourceFile)
}
