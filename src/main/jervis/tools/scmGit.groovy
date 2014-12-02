package jervis.tools

class scmGit {
    def git_root = ""
    def mygit = "git"
    def scmGit() {
        this.setRoot()
    }
    def scmGit(String git) {
        mygit = git
        this.setRoot()
    }
    def setRoot() {
        def process = "${mygit} rev-parse --show-toplevel".execute()
        git_root = process.text.trim()
    }
    def getRoot() {
        git_root
    }
}
