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
  This step allows shared pipeline libraries to retrieve variables set in the
  runtime binding of the calling user's pipeline.

  Returns the contents of the variable in the binding (of any type).  This step
  makes it easier to access user pipeline data beyond being a functional-only
  context.

  Let's say a user has the following code in their Jenkinsfile.

        some_variable = 'foo'

    USAGE:
        getUserBinding('some_variable')

            returns String 'foo'
  */

import hudson.model.Run

@NonCPS
def getBuildBinding(Run run, String bindingVar) {
    run.execution.shell.context.getVariable(bindingVar)
}

@NonCPS
def call(String bindingVar) {
    getBuildBinding(currentBuild.rawBuild, bindingVar)
}
