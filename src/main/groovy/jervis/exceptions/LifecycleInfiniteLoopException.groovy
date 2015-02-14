package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.LifecycleValidationException}</tt> which is thrown when a lifecycles file keys reference in an infinite loop.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.LifecycleInfiniteLoopException
throw new LifecycleInfiniteLoopException('groovy.maven')</tt></pre><br>
 */
class LifecycleInfiniteLoopException extends LifecycleValidationException {

    /**
      Throw an exception when lifecycle keys reference in an infinite loop.

      @param message A simple message that will be prepended with <tt>'Infinite loop detected.  Last known key: ' + message</tt>
     */
    def LifecycleInfiniteLoopException(String message) {
        super("Infinite loop detected.  Last known key: " + message)
    }
}
