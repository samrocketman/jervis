/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */
package net.gleske.jervis.exceptions;

/**
  The base exception class for Jervis from which all other exceptions derive.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.exceptions.JervisException
throw new JervisException('some text')

// or alternately catch and re-throw with documentation
try {
    // some code which throws an exception
} catch(JervisException ex) {
    throw new JervisException('See docs at https://example.com/', ex)
}
</code></pre><br>
 */
public class JervisException extends Exception {

    /**
      Throw a <tt>JervisException</tt>.

      @param message A simple message.
     */
    public JervisException(String message) {
        super(message);
    }

    /**
      Throw a reduced <tt>JervisException</tt> attaching an additional message
      to the exception.  This is typically for providing additional
      supplementary documentation.

      @param message A simple message.
     */
    public JervisException(String message, Throwable t) {
        super("\n\n" + message + "\n", t, true, false);
    }
}
