package jervis.tools
import jervis.tools.scmGit

/**
  A class to provide basic file operations from <tt>readFile()</tt> strings in a yaml file.
  <tt>readFile()</tt> strings typically have the format of <tt>readFile(/path/to/file.sh)</tt> where <tt>/path/to/file.sh</tt> is a bash script that exists relative to the root of the Git repository.
  This is intended to simplify a yaml file by keeping complicated bash code in a file and allowing the <tt>readFile()</tt> command to read it as if it were written in the yaml file.

  <h2>Sample usage</h2>
<pre><tt>import jervis.tools.fileIO
//mystring path is relative to the root of the git repository
def mystring = 'readFile(/src/testResources/sample_script.sh)'
def x = new fileIO()
println 'Is readFile? ' + x.isReadFile(mystring)
println 'Does the file exist?' + x.fileExists(mystring)
println 'What are the contents of the file?'
println x.readFile(mystring)</tt></pre>
 */
class fileIO {
    /**
      Checks if the <tt>String</tt> starts with <tt>readFile(</tt> and ends with <tt>)</tt>.
      @param filestring A simple <tt>String</tt>.
      @return           A boolean based on the outcome of the description of this section.
     */
    public Boolean isReadFile(String filestring) {
        return (filestring =~ /^readFile\(\/.*\)$/).asBoolean()
    }
    /**
      Checks the file path inside the parenthesis of <tt>'readFile(/path/to/file.sh)'</tt> exists.
      Relative to the root of the Git repository, if the file <tt>/path/to/file.sh</tt> exists then it would return <tt>true</tt>.
      @param filestring A simple <tt>String</tt> that starts with <tt>readFile(</tt>, contains a path to a file, and ends with <tt>)</tt>.
      @return           A boolean.  If the file exists then it is <tt>true</tt>.  If the the file doesn't exist or it is a malformed <tt>String</tt> then it returns <tt>false</tt>.
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
      Read the contents of the file in a <tt>readFile()</tt> <tt>String</tt>.
      @param filestring Read the contents of <tt>/path/to/file.sh</tt> in the <tt>String</tt> <tt>readFile(/path/to/file.sh)</tt>.
      @return           A <tt>String</tt> which is the contents of the file read.  If the <tt>filestring</tt> is malformed or the file doesn't exist then it simply returns the original string.
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
