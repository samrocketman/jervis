package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A type of <code>{@link jervis.exceptions.LifecycleException}</code> which is thrown when an unsupported language lifecycle generation is attempted.

  <h2>Sample usage</h2>
<pre><code>import jervis.exceptions.UnsupportedLanguageException
throw new UnsupportedLanguageException("derpy")</code></pre>
 */

class UnsupportedLanguageException extends LifecycleException {
    private static String wiki_page = wikiPages.supported_languages
    /**
      Throw an exception when an unsupported language lifecycle generation is attempted.

      @param message A simple message that will be prepended with <code>"ERROR: Unsupported language in yaml -> language: " + message</code> as well as provide a link to a helpful wiki page, <code>{@link jervis.exceptions.wikiPages#supported_languages}</code>.
     */
    def UnsupportedLanguageException(String message) {
        super("\nERROR: Unsupported language in yaml -> language: " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
