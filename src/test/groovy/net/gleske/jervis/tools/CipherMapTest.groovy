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

import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.tools.YamlOperator

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
    @Test public void test_CipherMap_file() {
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_4096')
        ciphermap = new CipherMap(new File(url.file))
        assert ciphermap.plainMap == [:]
    }
    @Test public void test_CipherMap_leftShift_CipherMap() {
        ciphermap.plainMap = [hello: 'friend']
        URL url = this.getClass().getResource('/rsa_keys/good_id_rsa_4096')
        CipherMap cmap2 = new CipherMap(new File(url.file))
        cmap2.hash_iterations = 0
        cmap2.plainMap = [hello: 'world', goodbye: 'friend']
        cmap2 << ciphermap
        assert cmap2.plainMap == [hello: 'friend', goodbye: 'friend']
    }
    @Test public void test_CipherMap_leftShift_bad_data() {
        shouldFail(JervisException) {
            ciphermap << 3
        }

        ciphermap.plainMap = [some: 'data']
        ciphermap << ''
        assert ciphermap.plainMap == [:]

        ciphermap.plainMap = [some: 'data']
        ciphermap << 'a: b'
        assert ciphermap.plainMap == [:]

        ciphermap.plainMap = [some: 'data']
        ciphermap << '''\
            age: ''
            cipher: ''
            data: ''
            signature: ''
            '''.stripIndent()
        assert ciphermap.plainMap == [:]

        ciphermap.plainMap = [some: 'data']
        ciphermap << '''\
            age: 23
            cipher:
              - ''
              - ''
            data: ''
            signature: ''
            '''.stripIndent()
        assert ciphermap.plainMap == [:]

        // signature validation fail due to corrupt data
        ciphermap.plainMap = [hello: 'friend']
        String ciphertext = ciphermap.toString()
        Map cipheryaml = YamlOperator.loadYamlFrom(ciphertext)
        cipheryaml.age = 'corrupt the data'
        ciphermap << YamlOperator.writeObjToYaml(cipheryaml)
        assert ciphermap.plainMap == [:]

        // signature validation fail due to corrupt signature
        ciphermap.plainMap = [hello: 'friend']
        ciphertext = ciphermap.toString()
        cipheryaml = YamlOperator.loadYamlFrom(ciphertext)
        cipheryaml.signature = 'corrupt the signature'
        ciphermap << YamlOperator.writeObjToYaml(cipheryaml)
        assert ciphermap.plainMap == [:]
    }
    // TODO fix test
    @Test public void test_CipherMap_rewrap_data_with_same_secrets() {
        ciphermap.plainMap = [some: 'data']
        Map yaml1 = YamlOperator.loadYamlFrom(ciphermap.toString())
        ciphermap.plainMap = [change: 'data']
        Map yaml2 = YamlOperator.loadYamlFrom(ciphermap.toString())
        assert yaml1.age == yaml2.age
        assert yaml1.cipher[0] == yaml2.cipher[0]
        assert yaml1.cipher[1] == yaml2.cipher[1]
        assert yaml1.data != yaml2.data
        assert yaml1.signature != yaml2.signature
    }
    // TODO test rotating secrets due to age
}
