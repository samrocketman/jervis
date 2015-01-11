package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.ToolchainValidationException}</tt> which is thrown when a lifecycles file key is referenced but missing.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.ToolchainMissingKeyException
throw new ToolchainMissingKeyException("somekey")</tt></pre><br>
 */
class ToolchainMissingKeyException extends ToolchainValidationException {

    /**
      Throw an exception when toolchains file key is referenced but missing.  This usually means that it is referenced in the toolchains key but the toolchain does not actually exist.

      @param message A simple message that will be prepended with <tt>"Missing key: " + message</tt>
     */
    def ToolchainMissingKeyException(String message) {
        super("Missing key: " + message)
    }
}
