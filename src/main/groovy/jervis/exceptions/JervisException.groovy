package jervis.exceptions

/**
  The base exception class for Jervis from which all other exceptions derive.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.JervisException
throw new JervisException("some text")</tt></pre><br>
 */
class JervisException extends Exception {

    /**
      Throw a <tt>JervisException</tt>.

      @param message A simple message.
     */
    def JervisException(String message) {
        super(message)
    }
}
