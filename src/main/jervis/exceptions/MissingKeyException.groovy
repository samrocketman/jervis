package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.ValidationException}</tt> which is thrown when a lifecycles file key is referenced but missing.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.MissingKeyException
throw new MissingKeyException("groovy.defaultKey")</tt></pre><br>
 */
class MissingKeyException extends ValidationException {

    /**
      Throw an exception when lifecycles file key is referenced but missing.

      @param message A simple message that will be prepended with <tt>"Missing key: " + message</tt>
     */
    def MissingKeyException(String message) {
        super("Missing key: " + message)
    }
}
