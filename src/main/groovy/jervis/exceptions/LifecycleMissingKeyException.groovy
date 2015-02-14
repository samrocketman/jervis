package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.LifecycleValidationException}</tt> which is thrown when a lifecycles file key is referenced but missing.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.LifecycleMissingKeyException
throw new LifecycleMissingKeyException('groovy.defaultKey')</tt></pre><br>
 */
class LifecycleMissingKeyException extends LifecycleValidationException {

    /**
      Throw an exception when lifecycles file key is referenced but missing.

      @param message A simple message that will be prepended with <tt>'Missing key: ' + message</tt>
     */
    def LifecycleMissingKeyException(String message) {
        super('Missing key: ' + message)
    }
}
