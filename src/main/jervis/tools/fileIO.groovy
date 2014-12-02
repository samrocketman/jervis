package jervis.tools

class fileIO {
    def isReadFile(String file) {
        return (file =~ /^readFile\(\/.*\)$/).asBoolean()
    }
}
