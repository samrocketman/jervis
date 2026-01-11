/*
   Copyright 2014-2026 Sam Gleske - https://github.com/samrocketman/jervis

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
//the GZipTest() class automatically sees the GZip() class because they're in the same package

import java.util.zip.GZIPInputStream
import org.junit.Test

class GZipTest extends GroovyTestCase {
    @Test public void test_GZip_utility_decompression() {
        StringWriter stdout = new StringWriter()
        StringWriter stderr = new StringWriter()
        Process proc = 'gunzip'.execute()
        // write to stdin
        proc.outputStream.withCloseable { stdin ->
            new GZip(stdin).withCloseable {
                it << 'hello\n'
            }
        }
        // capture stdout
        proc.waitForProcessOutput(stdout, stderr)
        // stdout is decompressed data by gunzip
        assert stdout.toString() == 'hello\n'
        assert stderr.toString() == ''
        assert proc.isAlive() == false
        assert proc.exitValue() == 0
    }
    @Test public void test_GZip_fast_decompression() {
        String text_to_compress = ''
        ByteArrayOutputStream compressed = new ByteArrayOutputStream()
        // fast compression
        new GZip(compressed, 0).withCloseable {
            it << text_to_compress
        }
        ByteArrayInputStream is = new ByteArrayInputStream(compressed.toByteArray())
        ByteArrayOutputStream plain = new ByteArrayOutputStream()
        GZIPInputStream gunzip = new GZIPInputStream(is)
        plain << gunzip
        assert plain.toString() == text_to_compress
    }
}
