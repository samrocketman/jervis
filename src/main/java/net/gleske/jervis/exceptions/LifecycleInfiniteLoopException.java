/*
   Copyright 2014-2016 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.exceptions

/**
  A type of <tt>{@link net.gleske.jervis.exceptions.LifecycleValidationException}</tt> which is thrown when a lifecycles file keys reference in an infinite loop.

  <h2>Sample usage</h2>
<pre><tt>import net.gleske.jervis.exceptions.LifecycleInfiniteLoopException
throw new LifecycleInfiniteLoopException('groovy.maven')</tt></pre><br>
 */
class LifecycleInfiniteLoopException extends LifecycleValidationException {

    /**
      Throw an exception when lifecycle keys reference in an infinite loop.

      @param message A simple message that will be prepended with <tt>'Infinite loop detected.  Last known key: ' + message</tt>
     */
    def LifecycleInfiniteLoopException(String message) {
        super("Infinite loop detected.  Last known key: " + message)
    }
}
