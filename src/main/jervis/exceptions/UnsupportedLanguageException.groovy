package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A type of <tt>{@link jervis.exceptions.LifecycleException}</tt> which is thrown when an unsupported language lifecycle generation is attempted.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.UnsupportedLanguageException
throw new UnsupportedLanguageException("derpy")</tt></pre><br>
 */
class UnsupportedLanguageException extends GeneratorException {
    private static String wiki_page = wikiPages.supported_languages

    /**
      Throw an exception when an unsupported language lifecycle generation is attempted.

      @param message A simple message that will be prepended with <tt>"ERROR: Unsupported language in yaml -> language: " + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link jervis.exceptions.wikiPages#supported_languages}</tt>.
     */
    def UnsupportedLanguageException(String message) {
        super("\nERROR: Unsupported language in yaml -> language: " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
