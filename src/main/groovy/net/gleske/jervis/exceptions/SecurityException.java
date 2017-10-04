/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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

import net.gleske.jervis.exceptions.wikiPages;

/**
  A group of exceptions that are thrown when security issues occur.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><tt>import net.gleske.jervis.exceptions.SecurityException
throw new SecurityException('some text')</tt></pre><br>
 */
public class SecurityException extends JervisException {
    private static String wiki_page = wikiPages.secure_secrets;

    /**
      Throw a <tt>SecurityException</tt>.

      @param message A simple message that will be prepended with <tt>'ERROR: An encryption related issue occurred.  ' + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link net.gleske.jervis.exceptions.wikiPages#secure_secrets}</tt>.
     */
    public SecurityException(String message) {
        super("\nERROR: An encryption related issue occured.  " + message + "\n\nSee wiki page:\n" + wiki_page + "\n\n");
    }
}
