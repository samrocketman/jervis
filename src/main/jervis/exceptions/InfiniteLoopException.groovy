package jervis.exceptions

/**
  A type of <code>{@link jervis.exceptions.ValidationException}</code> which is thrown when a lifecycles file keys reference in an infinite loop.

  <h2>Sample usage</h2>
<pre><code>import jervis.exceptions.InfiniteLoopException
throw new InfiniteLoopException("groovy.maven")</code></pre>
 */

class InfiniteLoopException extends ValidationException
{
    /**
      Throw an exception when lifecycle keys reference in an infinite loop.

      @param message A simple message that will be prepended with <code>"Infinite loop detected.  Last known key: " + message</code>
     */
    def InfiniteLoopException(String message) {
        super("Infinite loop detected.  Last known key: " + message)
    }
}
