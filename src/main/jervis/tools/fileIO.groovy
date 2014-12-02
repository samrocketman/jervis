package jervis.tools
import jervis.tools.scmGit

class fileIO {
    def isReadFile(String filestring) {
        return (filestring =~ /^readFile\(\/.*\)$/).asBoolean()
    }
    def fileExists(String filestring) {
        def git = new scmGit()
        if (filestring[0..8] == 'readFile(') {
            File file = new File("${git.getRoot()}${filestring[9..-2]}")
            return file.exists()
        }
    }
    def fileContents(String filestring) {
        def contents = []
        def git = new scmGit()
        if (filestring[0..8] == 'readFile(') {
            File file = new File("${git.getRoot()}${filestring[9..-2]}")
            file.eachLine {
                contents << it
            }
        }
        return contents.join('\n').trim()
    }
    def readFile(String filestring) {
        if(this.isReadFile(filestring) && this.fileExists(filestring)) {
            return this.fileContents(filestring)
        }
        else {
            return filestring
        }
    }
}
