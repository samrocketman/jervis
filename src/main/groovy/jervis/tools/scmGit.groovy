package jervis.tools

/**
   A class to provide useful functions for interacting with a local git repository.

   <h2>Sample usage</h2>

<pre><tt>import jervis.tools.scmGit
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
    public void setRoot() {
        def process = "${mygit} rev-parse --show-toplevel".execute()
        git_root = process.text.trim()
    }

    /**
      @return The value of <tt>{@link #git_root}</tt> which is a known directory of the git repository in the current working directory of when the <tt>scmGit</tt> class was instantiated.
     */
    String getRoot() {
        git_root
    }
}
