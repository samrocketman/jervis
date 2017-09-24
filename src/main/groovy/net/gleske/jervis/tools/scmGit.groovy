/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.tools

import net.gleske.jervis.exceptions.JervisException

/**
   A class to provide useful functions for interacting with a local git repository.

   <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><tt>import net.gleske.jervis.tools.scmGit
def x = new scmGit()
print "Git root dir: "
println x.getRoot()</tt></pre><br>
 */
class scmGit {
    /**
      Stores the root directory of the git repository when the class is instantiated or <tt>{@link #setRoot()}</tt> is called.
      Default: <tt>""</tt>
     */
    String git_root = ""

    /**
      The default git command to be used when executing git commands.
      Default: <tt>"git"</tt>
     */
    String mygit = "git"

    /**
      Upon instantiation calls the <tt>{@link #setRoot()}</tt> function to set the <tt>{@link #git_root}</tt> property.
     */
    def scmGit() {
        this.setRoot()
    }

    /**
      Instantiates a custom <tt>git</tt> path and calls the <tt>{@link #setRoot()}</tt> function to set the <tt>{@link #git_root}</tt> property.

      @param git A path to a <tt>git</tt> executable.
     */
    def scmGit(String git) {
        mygit = git
        this.setRoot()
    }

    /**
      Gets the root directory of a repository and stores in in <tt>{@link #git_root}</tt>.  It is the equivalent of executing the following git command.
      <pre><tt>git rev-parse --show-toplevel</tt></pre><br>
     */
    public void setRoot() throws JervisException {
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        def process = [mygit, "rev-parse", "--show-toplevel"].execute()
        process.waitForProcessOutput(stdout, stderr)
        if(process.exitValue()) {
            throw new JervisException(stderr.toString())
        }
        git_root = stdout.toString().trim()
    }

    /**
      @return The value of <tt>{@link #git_root}</tt> which is a known directory of the git repository in the current working directory of when the <tt>scmGit</tt> class was instantiated.
     */
    String getRoot() {
        git_root
    }
}
