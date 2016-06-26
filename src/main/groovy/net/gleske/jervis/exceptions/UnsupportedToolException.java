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
package net.gleske.jervis.exceptions;

import net.gleske.jervis.exceptions.wikiPages;

/**
  A type of <tt>{@link net.gleske.jervis.exceptions.GeneratorException}</tt> which is thrown when an unsupported language lifecycle generation is attempted.

  <h2>Sample usage</h2>
<pre><tt>import net.gleske.jervis.exceptions.UnsupportedToolException
throw new UnsupportedToolException('derpy')</tt></pre><br>
 */
public class UnsupportedToolException extends GeneratorException {
    private static String wiki_page = wikiPages.supported_tools;

    /**
      Throw an exception when an unsupported tool generation is attempted.  It would
      be most userfriendly to tell the user where in the YAML file they went wrong by
      passing in <tt>message</tt> the value of something like <tt>jdk: derpy</tt>
      which would tell the user they're trying to generate the 'derpy' tool when the
      jdk section doesn't support 'derpy'.

      @param message A simple message that will be prepended with <tt>'ERROR: Unsupported language in yaml -> ' + message</tt> as well as provide a link to a helpful wiki page, <tt>{@link net.gleske.jervis.exceptions.wikiPages#supported_tools}</tt>.
     */
    public UnsupportedToolException(String message) {
        super("\nERROR: Unsupported tool in yaml -> " + message + "\n\nSee wiki page:\n" + wiki_page + "\n\n");
    }
}
