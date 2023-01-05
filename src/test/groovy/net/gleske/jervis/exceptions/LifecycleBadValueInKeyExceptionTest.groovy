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
package net.gleske.jervis.exceptions
//the LifecycleBadValueInKeyExceptionTest() class automatically sees the LifecycleBadValueInKeyException() class because they're in the same package

import org.junit.After
import org.junit.Before
import org.junit.Test

class LifecycleBadValueInKeyExceptionTest extends GroovyTestCase {
    @Test public void test_LifecycleBadValueInKeyException() {
        //this basically ensures the exception isn't removed or renamed
        shouldFail(LifecycleBadValueInKeyException) {
            throw new LifecycleBadValueInKeyException('just a test')
        }
    }
}
