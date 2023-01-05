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
  A type of <tt>{@link net.gleske.jervis.exceptions.PlatformValidationException}</tt> which is thrown when there is a bad value in a platforms file key.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.exceptions.PlatformBadValueInKeyException
throw new PlatformBadValueInKeyException('defaults.platform')</code></pre><br>
 */
public class PlatformBadValueInKeyException extends PlatformValidationException {

    /**
      Throw an exception for a bad value in a key.

      @param message A simple message that will be prepended with <tt>'Bad value in key: ' + message</tt>
     */
    public PlatformBadValueInKeyException(String message) {
        super("Bad value in key: " + message);
    }
}
