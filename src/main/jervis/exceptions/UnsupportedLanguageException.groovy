package jervis.exceptions

import jervis.exceptions.wikiPages

class UnsupportedLanguageException extends LifecycleException {
    private static String wiki_page = wikiPages.supported_languages
    def UnsupportedLanguageException(String message) {
        super("\nERROR: Unsupported language in yaml -> language: " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
