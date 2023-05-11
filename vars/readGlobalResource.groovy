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
/**
  readGlobalResource is similar to libraryResource; except it will skip
  printing an echo statement.  Returns an empty string if it doesn't exist.
  */
import hudson.model.Run
import org.jenkinsci.plugins.workflow.libs.LibrariesAction
import org.jenkinsci.plugins.workflow.libs.LibraryRecord

/**
  Check if a global library provides a resource.
 */
@NonCPS
String lookForGlobalLibraryResource(Run run, String resourceFile) {
    def action = run.getAction(LibrariesAction)
    String contents = ''
    action.libraries.any { LibraryRecord library ->
        File file = new File(run.getRootDir(), "libs/${library.name}/resources/${resourceFile}")
        if(file.exists()) {
            contents = file.text
        }
        // exit loop early
        file.exists()
    }
    contents
}

@NonCPS
String call(String resourceFile) {
    lookForGlobalLibraryResource(currentBuild.rawBuild, resourceFile)
}

