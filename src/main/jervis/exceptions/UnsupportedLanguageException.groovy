package jervis.exceptions

class UnsupportedLanguageException extends LifecycleException {
    private static String wiki_page = "https://github.com/samrocketman/jervis/wiki/Supported-Languages"
    def UnsupportedLanguageException(String message) {
        super("\nERROR: Unsupported language in yaml -> language: " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
