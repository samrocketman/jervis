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
  hasGlobalVar will check to see if a global variable exists which is possible
  loaded by another pipeline library.  This is useful for optionally detecting
  and calling variables provided by other pipelines.  It gives a means of
  gracefully falling back if the variables provided by other pipelines are not
  loaded or do not exist.
  */

import hudson.ExtensionList
import org.jenkinsci.plugins.workflow.libs.LibraryAdder

/**
  Check if a global library provides a var.  Useful for providing optional
  functionality provided by other global libraries (only if they are defined).
 */
@NonCPS
Boolean hasGlobalVar(String var) {
    var in  ExtensionList.lookup(LibraryAdder.GlobalVars.class)[0].forRun(currentBuild.rawBuild)*.name
}

@NonCPS
Boolean call(String var) {
    hasGlobalVar(var)
}
