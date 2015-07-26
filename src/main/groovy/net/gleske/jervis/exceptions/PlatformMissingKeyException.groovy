/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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
package jervis.exceptions

/**
  A type of <tt>{@link jervis.exceptions.PlatformValidationException}</tt> which is thrown when a platforms file key is referenced but missing.

  <h2>Sample usage</h2>
<pre><tt>import jervis.exceptions.PlatformMissingKeyException
throw new PlatformMissingKeyException('defaults')</tt></pre><br>
 */
class PlatformMissingKeyException extends PlatformValidationException {

    /**
      Throw an exception when platforms file key is referenced but missing.

      @param message A simple message that will be prepended with <tt>'Missing key: ' + message</tt>
     */
    def PlatformMissingKeyException(String message) {
        super('Missing key: ' + message)
    }
}
