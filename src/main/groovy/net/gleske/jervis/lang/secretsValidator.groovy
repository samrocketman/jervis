/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.lang

/**
  Validates the contents of a Jervis YAML file and decrypts secrets.

  <h2>Sample usage</h2>
  <p>Please note, if you are writing Job DSL plugin groovy scripts you should not
  use the <tt>scmGit</tt> class to access files in the repository where your DSL
  scripts reside.  Instead, use the
  <a href="https://github.com/samrocketman/jervis/issues/43" target="_blank"><tt>readFileFromWorkspace</tt></a>
  method provided by the Job DSL plugin in Jenkins.</p>
<pre><tt>import net.gleske.jervis.lang.secretsValidator
import net.gleske.jervis.tools.scmGit
def git = new scmGit()
def secrets = new secretsValidator()
println 'Does the file validate? ' + secrets.validate()</tt></pre>
 */
class secretsValidator {

    /**
      A <tt>{@link Map}</tt> of the parsed secrets from a YAML file.
     */
    Map secrets
}
