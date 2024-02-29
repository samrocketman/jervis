/*
   Copyright 2014-2024 Sam Gleske - https://github.com/samrocketman/jervis

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
  A type of <tt>{@link net.gleske.jervis.exceptions.SecurityException}</tt> which is thrown when an issue with generating key pairs occurs.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.exceptions.KeyPairDecodeException
throw new KeyPairDecodeException('some reason')</code></pre><br>
 */
public class KeyPairDecodeException extends SecurityException {

    /**
      Throw an exception relaying why decoding a string possibly containing a
      PEM encoded X.509 private key failed.

      @param message A simple message.
     */
    public KeyPairDecodeException(String message) {
        super(message);
    }
}
