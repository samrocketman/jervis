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
    String git_root = ""
    String mygit = "git"
    def scmGit() {
        this.setRoot()
    }
    def scmGit(String git) {
        mygit = git
        this.setRoot()
    }
    public void setRoot() {
        def process = "${mygit} rev-parse --show-toplevel".execute()
        git_root = process.text.trim()
    }
    String getRoot() {
        git_root
    }
}
