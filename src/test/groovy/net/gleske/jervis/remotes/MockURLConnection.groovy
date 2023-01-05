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
package net.gleske.jervis.remotes
/**
  A Mock URLConnection whose only purpose is to add a private field to the
  <tt>{@link java.util.LinkedHashMap}</tt> class.
  */
class MockURLConnection extends LinkedHashMap {
    /**
      A private field for setting a field on a HashMap that doesn't normally
      exist.
      */
    private def method
}
