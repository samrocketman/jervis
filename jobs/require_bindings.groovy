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
   A helper script which forces required bindings to be called out when using other scripts.
 */

import net.gleske.jervis.exceptions.JervisException

require_bindings = null
require_bindings = { String include_script_name, List bindings_list ->
    Set required_bindings = bindings_list.toSet()
    Set missing_bindings = required_bindings - (binding.variables.keySet()*.toString() as Set)
    if(missing_bindings) {
        throw new JervisException("${include_script_name} is missing required bindings from calling script: ${missing_bindings.join(', ')}")
    }
}
