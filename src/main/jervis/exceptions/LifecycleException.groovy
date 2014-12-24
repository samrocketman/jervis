package jervis.exceptions

/**
  A group of exceptions that are thrown when undesireable lifecycle generation conditions occur.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.LifecycleException
throw new LifecycleException("some text")</tt></pre><br>
 */
class LifecycleException extends JervisException {

    /**
      Throw a <tt>LifecycleException</tt>.

      @param message A simple message.
     */
    def LifecycleException(String message) {
        super(message)
    }
}
