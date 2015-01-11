package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.LifecycleValidationException}</tt> which is thrown when there is a bad value in a lifecycles file key.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.LifecycleBadValueInKeyException
throw new LifecycleBadValueInKeyException("ruby.rake1.fileExistsCondition")</tt></pre><br>
 */
class LifecycleBadValueInKeyException extends LifecycleValidationException {

    /**
      Throw an exception for a bad value in a key.

      @param message A simple message that will be prepended with <tt>"Bad value in key: " + message</tt>
     */
    def LifecycleBadValueInKeyException(String message) {
        super("Bad value in key: " + message)
    }
}
