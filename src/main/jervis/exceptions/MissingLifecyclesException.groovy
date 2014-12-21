package jervis.exceptions

import jervis.exceptions.wikiPages

class MissingLifecyclesException extends LifecycleException {
    private static String wiki_page = wikiPages.lifecycles_spec
    def MissingLifecyclesException(String message) {
        super("\nERROR: Missing Lifecycles file.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
