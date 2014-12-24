package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A group of exceptions that are thrown when validation errors occur in the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycles file</a>.
 */

class ValidationException extends Exception
{
    private static String wiki_page = wikiPages.lifecycles_spec
    /**
      Throw a <tt>ValidationException</tt>

      @param message A simple message that will be prepended with <tt>"ERROR: Lifecycle validation failed.  " + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link jervis.exceptions.wikiPages#lifecycles_spec}</tt>.
     */
    def ValidationException(String message) {
        super("\nERROR: Lifecycle validation failed.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
