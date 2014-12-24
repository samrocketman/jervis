package jervis.exceptions

import jervis.exceptions.wikiPages

/**
  A type of <tt>{@link jervis.exceptions.LifecycleException}</tt> which is thrown when lifecycles are not loaded from a yaml file.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.MissingLifecyclesException
throw new MissingLifecyclesException("Call loadLifecycles function before the loadYaml function.")</tt></pre>
 */

class MissingLifecyclesException extends LifecycleException {
    private static String wiki_page = wikiPages.lifecycles_spec
    /**
      Throw an exception when lifecycles are not loaded from a yaml file.

      @param message A simple message that will be prepended with <tt>"ERROR: Missing Lifecycles.  " + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link jervis.exceptions.wikiPages#lifecycles_spec}</tt>.
     */
    def MissingLifecyclesException(String message) {
        super("\nERROR: Missing Lifecycles.  " + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n'))
    }
}
