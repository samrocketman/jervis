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
/**
  Contains <tt>remotes</tt> for Jervis to communicate to different API services.  Commonly for sources hosting
  Git repositories.  If writing a remote, be sure to follow the implementation of
  the <tt>{@link net.gleske.jervis.remotes.interfaces.JervisRemote}</tt>
  interface.  Otherwise, API clients should be implementing
  <tt>{@link net.gleske.jervis.remotes.SimpleRestServiceSupport}</tt>.

  <p>
  This allows remotes to be interchangeable.  Jervis will call only those functions to interact with a remote.
  See the <tt>GitHub</tt> class as an example for the implementation of a remote.
  </p>
 */
package net.gleske.jervis.remotes
