package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A group of exceptions that are thrown when validation errors occur in the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file</a>.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.ToolchainValidationException
throw new ToolchainValidationException("some text")</tt></pre><br>
 */
class ToolchainValidationException extends JervisException {
    private static String wiki_page = wikiPages.toolchains_spec

    /**
      Throw a <tt>ToolchainValidationException</tt>

      @param message A simple message that will be prepended with <tt>"ERROR: Toolchain validation failed.  " + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link jervis.exceptions.wikiPages#toolchains_spec}</tt>.
     */
    def ToolchainValidationException(String message) {
        super("\nERROR: Toolchain validation failed.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
