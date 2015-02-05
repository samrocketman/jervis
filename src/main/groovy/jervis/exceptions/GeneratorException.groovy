package jervis.exceptions

/**
  A group of exceptions that are thrown when undesireable script generation conditions occur.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.LifecycleException
throw new GeneratorException("some text")</tt></pre><br>
 */
class GeneratorException extends JervisException {

    /**
      Throw a <tt>GeneratorException</tt>.

      @param message A simple message.
     */
    def GeneratorException(String message) {
        super(message)
    }
}
