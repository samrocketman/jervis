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
  This step allows shared pipeline libraries to set variables in the runtime
  binding of the calling user's pipeline.

  If in a scripted pipeline this is equivalent to:

        some_variable = 'foo'

    USAGE:
        setUserBinding('some_variable', 'foo')
  */

import hudson.model.Run

@NonCPS
def setBuildBinding(Run run, String bindingVar, def content) {
    run.execution.shell.context.setVariable(bindingVar, content)
}

@NonCPS
def call(String bindingVar, def content) {
    setBuildBinding(currentBuild.rawBuild, bindingVar, content)
}
