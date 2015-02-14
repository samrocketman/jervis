package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A type of <tt>{@link jervis.exceptions.GeneratorException}</tt> which is thrown when an unsupported language lifecycle generation is attempted.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.UnsupportedToolException
throw new UnsupportedToolException('derpy')</tt></pre><br>
 */
class UnsupportedToolException extends GeneratorException {
    private static String wiki_page = wikiPages.supported_tools

    /**
      Throw an exception when an unsupported tool generation is attempted.  It would
      be most userfriendly to tell the user where in the YAML file they went wrong by
      passing in <tt>message</tt> the value of something like <tt>jdk: derpy</tt>
      which would tell the user they're trying to generate the 'derpy' tool when the
      jdk section doesn't support 'derpy'.

      @param message A simple message that will be prepended with <tt>'ERROR: Unsupported language in yaml -> ' + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link jervis.exceptions.wikiPages#supported_tools}</tt>.
     */
    def UnsupportedToolException(String message) {
        super('\nERROR: Unsupported tool in yaml -> ' + message + ['\n\nSee wiki page:', wiki_page,'\n'].join('\n'))
    }
}
