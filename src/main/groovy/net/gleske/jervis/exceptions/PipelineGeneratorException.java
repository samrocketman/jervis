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
  A group of exceptions that are thrown when validation errors occur in the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycles file</a>.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.exceptions.PipelineGeneratorException
throw new PipelineGeneratorException('some text')</code></pre><br>
 */
public class PipelineGeneratorException extends JervisException {
    private static final String WIKI_PAGE = WikiPages.PIPELINE_SUPPORT;

    /**
      Throw a <tt>PipelineGeneratorException</tt>.

      @param message A simple message that will be prepended with <tt>'ERROR: Pipeline Generator failed.  ' + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link net.gleske.jervis.exceptions.WikiPages#PIPELINE_SUPPORT}</tt>.
     */
    public PipelineGeneratorException(String message) {
        super("\nERROR: Pipeline Generator failed.  " + message + "\n\nSee wiki page:\n" + WIKI_PAGE + "\n\n");
    }
}
