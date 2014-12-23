package jervis.exceptions

/**
  A type of <code>{@link jervis.exceptions.ValidationException}</code> which is thrown when a lifecycles file key is referenced but missing.

  <h2>Sample usage</h2>
<pre><code>import jervis.exceptions.MissingKeyException
throw new MissingKeyException("groovy.defaultKey")</code></pre>
 */

class MissingKeyException extends ValidationException
{
    /**
      Throw an exception when lifecycles file key is referenced but missing.

      @param message A simple message that will be prepended with <code>"Missing key: " + message</code>
     */
    def MissingKeyException(String message) {
        super("Missing key: " + message)
    }
}
