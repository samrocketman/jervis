package jervis.exceptions

/**
  A group of exceptions that are thrown when undesireable lifecycle generation conditions occur.
 */
class LifecycleException extends Exception {
    /**
      Throw a <code>LifecycleException</code>

      @param message A simple message.
     */
    def LifecycleException(String message) {
        super(message)
    }
}
