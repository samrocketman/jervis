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
package net.gleske.jervis.remotes
/**
  A Mock URLConnection whose only purpose is to add a private field to the
  <tt>{@link java.util.LinkedHashMap}</tt> class and to enable method calls
  to be dispatched to closures stored as map entries.
  */
class MockURLConnection extends LinkedHashMap {
    /**
      A private field for setting a field on a HashMap that doesn't normally
      exist.
      */
    private def method

    /**
      Enables method calls on this Map-based mock object to be dispatched to
      closures stored as map entries. This is necessary for Groovy 2.4.x
      compatibility where method calls on LinkedHashMap subclasses don't
      automatically invoke closures stored as values.
      */
    def methodMissing(String name, args) {
        def closure = this.get(name)
        if(closure instanceof Closure) {
            return closure.call(*args)
        }
        throw new MissingMethodException(name, this.getClass(), args)
    }
}
