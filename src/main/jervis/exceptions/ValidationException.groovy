package jervis.exceptions

import jervis.exceptions.wikiPages

class ValidationException extends Exception
{
    private static String wiki_page = wikiPages.lifecycles_spec
    def ValidationException(String message) {
        super("\nERROR: Lifecycle validation failed.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
