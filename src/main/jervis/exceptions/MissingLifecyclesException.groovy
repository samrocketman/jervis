package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A type of <code>{@link jervis.exceptions.LifecycleException}</code> which is thrown when lifecycles are not loaded from a yaml file.

  <h2>Sample usage</h2>
<pre><code>import jervis.exceptions.MissingLifecyclesException
throw new MissingLifecyclesException("Call loadLifecycles function before the loadYaml function.")</code></pre>
 */

class MissingLifecyclesException extends LifecycleException {
    private static String wiki_page = wikiPages.lifecycles_spec
    /**
      Throw an exception when lifecycles are not loaded from a yaml file.

      @param message A simple message that will be prepended with <code>"ERROR: Missing Lifecycles.  " + message</code> as well as provide a link to a helpful wiki page, <code>{@link jervis.exceptions.wikiPages#lifecycles_spec}</code>.
     */
    def MissingLifecyclesException(String message) {
        super("\nERROR: Missing Lifecycles.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
