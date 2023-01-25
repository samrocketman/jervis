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
package net.gleske.jervis.tools
//the CipherMapTest() class automatically sees the CipherMap() class because they're in the same package

import org.junit.After
import org.junit.Before
import org.junit.Test

class CipherMapTest extends GroovyTestCase {
    CipherMap ciphermap
    String privateKey
    @Before protected void setUp() {
        if(!privateKey) {
            URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_2048')
            privateKey = url.content.text
        }
        ciphermap = new CipherMap(privateKey)
        ciphermap.hash_iterations = 0
    }
    @After protected void tearDown() {
        ciphermap = null
    }
    @Test public void test_CipherMap_basic_usage() {
        Map plainTextMap = [hello: 'world']
        ciphermap.plainMap = plainTextMap
        assert ciphermap.plainMap == plainTextMap
        String ciphertext = ciphermap.toString()
        ciphermap.plainMap = [:]
        assert ciphermap.plainMap == [:]
        ciphermap << ciphertext
        assert ciphermap.plainMap == plainTextMap
    }
}
