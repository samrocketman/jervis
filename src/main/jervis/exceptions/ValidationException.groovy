package jervis.exceptions


class ValidationException extends Exception
{
    private static String wiki_page = "https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file"
    def ValidationException(String message) {
        super("\nERROR: Lifecycle validation failed.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
