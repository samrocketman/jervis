package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A group of exceptions that are thrown when validation errors occur in the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycles file</a>.
 */

class ValidationException extends Exception
{
    private static String wiki_page = wikiPages.lifecycles_spec
    /**
      Throw a <code>ValidationException</code>

      @param message A simple message that will be prepended with <code>"ERROR: Lifecycle validation failed.  " + message</code> as well as provide a link to a helpful wiki page, <code>{@link jervis.exceptions.wikiPages#lifecycles_spec}</code>.
     */
    def ValidationException(String message) {
        super("\nERROR: Lifecycle validation failed.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
