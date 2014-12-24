package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.ValidationException}</tt> which is thrown when there is a bad value in a lifecycles file key.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.BadValueInKeyException
throw new BadValueInKeyException("ruby.rake1.fileExistsCondition")</tt></pre>
 */

class BadValueInKeyException extends ValidationException
{
    /**
      Throw an exception for a bad value in a key.

      @param message A simple message that will be prepended with <tt>"Bad value in key: " + message</tt>
     */
    def BadValueInKeyException(String message) {
        super("Bad value in key: " + message)
    }
}
