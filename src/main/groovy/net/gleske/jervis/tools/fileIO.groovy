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
package net.gleske.jervis.tools
import net.gleske.jervis.tools.scmGit

/**
  A class to provide basic file operations from <tt>readFile()</tt> strings in a
  yaml file.  <tt>readFile()</tt> strings typically have the format of
  <tt>readFile(/path/to/file.sh)</tt> where <tt>/path/to/file.sh</tt> is a bash
  script that exists relative to the root of the Git repository.  This is intended
  to simplify a yaml file by keeping complicated bash code in a file and allowing
  the <tt>readFile()</tt> command to read it as if it were written in the yaml
  file.

  <h2>Sample usage</h2>
<pre><tt>import jervis.tools.fileIO
//mystring path is relative to the root of the git repository
def mystring = 'readFile(/src/test/resources/sample_script.sh)'
def x = new fileIO()
println 'Is readFile? ' + x.isReadFile(mystring)
println 'Does the file exist?' + x.fileExists(mystring)
println 'What are the contents of the file?'
println x.readFile(mystring)</tt></pre>
 */
class fileIO {
    /**
      Checks if the <tt>String</tt> starts with <tt>readFile(</tt> and ends with
      <tt>)</tt>.
      @param filestring A simple <tt>String</tt>.
      @return           A boolean based on the outcome of the description of this
                        section.
     */
    public Boolean isReadFile(String filestring) {
        return (filestring =~ /^readFile\(\/.*\)$/).asBoolean()
    }
    /**
      Checks the file path inside the parenthesis of
      <tt>'readFile(/path/to/file.sh)'</tt> exists.  Relative to the root of the Git
      repository, if the file <tt>/path/to/file.sh</tt> exists then it would return
      <tt>true</tt>.  This method uses <tt>{@link jervis.tools.scmGit}</tt> to
      determine the root of the Git repository.
      @param filestring A simple <tt>String</tt> that starts with <tt>readFile(</tt>,
                        contains a path to a file, and ends with <tt>)</tt>.
      @return           A boolean.  If the file exists then it is <tt>true</tt>.  If
                        the the file doesn't exist or it is a malformed
                        <tt>String</tt> then it returns <tt>false</tt>.
     */
    public Boolean fileExists(String filestring) {
        def git = new scmGit()
        if (this.isReadFile(filestring)) {
            File file = new File("${git.getRoot()}${filestring[9..-2]}")
            return file.exists()
        }
        else {
            return false
        }
    }
    /**
      Read the contents of the file in a <tt>readFile()</tt> <tt>String</tt>.  This
      method uses <tt>{@link jervis.tools.scmGit}</tt> to determine the root of the
      Git repository.
      @param filestring Read the contents of <tt>/path/to/file.sh</tt> in the
                        <tt>String</tt> <tt>readFile(/path/to/file.sh)</tt>.
      @return           A <tt>String</tt> which is the contents of the file read.  If
                        the <tt>filestring</tt> is malformed or the file doesn't exist
                        then it simply returns the original string.
     */
    public String readFile(String filestring) {
        def contents = []
        def git = new scmGit()
        if (this.isReadFile(filestring) && this.fileExists(filestring)) {
            File file = new File("${git.getRoot()}${filestring[9..-2]}")
            file.eachLine {
                contents << it
            }
            return contents.join('\n').trim()
        }
        else {
            return filestring
        }
    }
}
