/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.lang
//the lifecycleGeneratorTest() class automatically sees the lifecycleGenerator() class because they're in the same package
import org.junit.After
import org.junit.Before
import org.junit.Test

class pipelineGeneratorTest extends GroovyTestCase {
    def generator
    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        generator = new lifecycleGenerator()
        URL url = this.getClass().getResource('/good_lifecycles_simple.json');
        generator.loadLifecycles(url.getFile())
        url = this.getClass().getResource('/good_toolchains_simple.json');
        generator.loadToolchains(url.getFile())
    }
    //tear down after every test
    @After protected void tearDown() {
        generator = null
        super.tearDown()
    }
    @Test public void test_pipelineGenerator_serialization() {
        URL url = this.getClass().getResource('/good_platforms_simple.json');
        generator.loadPlatforms(url.getFile())
        generator.loadYamlString('language: ruby')
        def pipeline = new pipelineGenerator(generator)
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(pipeline)
    }
}
