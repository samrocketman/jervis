package jervis.exceptions

/**
  A type of <code>{@link jervis.exceptions.ValidationException}</code> which is thrown when there is a bad value in a lifecycles file key.

  <h2>Sample usage</h2>
<pre><code>import jervis.exceptions.BadValueInKeyException
throw new BadValueInKeyException("ruby.rake1.fileExistsCondition")</code></pre>
 */

class BadValueInKeyException extends ValidationException
{
    /**
      Throw an exception for a bad value in a key.

      @param message A simple message that will be prepended with <code>"Bad value in key: " + message</code>
     */
    def BadValueInKeyException(String message) {
        super("Bad value in key: " + message)
    }
}
