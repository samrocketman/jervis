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
package net.gleske.jervis.remotes.creds
//the EphemeralTokenCacheTest() class automatically sees the EphemeralTokenCache() class because they're in the same package

import org.junit.After
import org.junit.Before
import org.junit.Test

class EphemeralTokenCacheTest extends GroovyTestCase {

    EphemeralTokenCache tokenCache

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        this.tokenCache = new EphemeralTokenCache({-> ''})
        tokenCache.loadCache = null
        tokenCache.saveCache = null
        tokenCache.obtainLock = null
    }
    //tear down after every test
    @After protected void tearDown() {
        this.tokenCache = null
        super.tearDown()
    }
    @Test public void test_EphemeralTokenCache_fail_instantiation() {
        shouldFail(IllegalStateException) {
            new EphemeralTokenCache()
        }
    }
    @Test public void test_EphemeralTokenCache_isExpired_without_existing() {
        String hash = 'fake'
        assert this.tokenCache.isExpired(hash) == true
    }
}
