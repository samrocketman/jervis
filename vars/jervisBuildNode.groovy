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
   This provides a means in which to build Jervis on a node.  By abstracting
   this to a pipeline variable we make it easy to swap out the agent
   provisioning where Jervis builds occur.
 */

import net.gleske.jervis.lang.PipelineGenerator

def call(PipelineGenerator pipeline_generator, String label, Closure body) {
    if(hasGlobalVar('adminJervisBuildNode')) {
        adminJervisBuildNode(pipeline_generator, label, body)
    }
    else {
        node(label) {
            body()
        }
    }
}
