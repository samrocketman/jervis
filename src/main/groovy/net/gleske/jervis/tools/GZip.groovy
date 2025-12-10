/*
   Copyright 2014-2025 Sam Gleske - https://github.com/samrocketman/jervis

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

import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream


/**
  A class to provide compression features to Jervis, specifically GZip.
  <tt>GZip</tt> provides groovy-like leftShift for compressing strings.  This
  class also uses maximum compression by default and provides an easy means to
  change the compression level.


  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

  <h4>Write out a compressed file</h4>

<pre><code>
import net.gleske.jervis.tools.GZip

new File('test.gz').withOutputStream { OutputStream os -&gt;
    try {
        (new GZip(os)).withCloseable {
            it &lt;&lt; 'hello world\n\nMy friend\n'
        }
    } finally {
        os.close()
    }
}
</code></pre>

  <h4>Upload a compressed file</h4>
  <p>This example uploads compressed data directly to <a href="https://help.sonatype.com/repomanager3" target=_blank>Sonatype Nexus</a>.  Compare this example with <tt>{@link net.gleske.jervis.remotes.SimpleRestService}</tt></p>
<pre><code>
import net.gleske.jervis.tools.GZip
import net.gleske.jervis.tools.SecurityIO

String username = 'your user'
String password = 'your pass'

URL api_url = new URL('http://localhost:8081/repository/hosted-raw-repo/file.gz')
HttpURLConnection response = api_url.openConnection().with { conn -&gt;
    conn.doOutput = true
    conn.setRequestMethod('PUT')
    conn.setRequestProperty('Authorization', "Basic " + SecurityIO.encodeBase64("${username}:${password}"))
    conn.setRequestProperty('Accept', '*&sol;*')
    conn.setRequestProperty('Expect', '100-continue')
    conn.outputStream.withCloseable { nexus_os -&gt;
        new GZip(nexus_os).withCloseable { gzip -&gt;
            gzip &lt;&lt; 'hello world\n\nMy friend\n'
        }
    }
    conn
}
// getting the response code completes the setup and makes the network connection
assert response.responseCode == 201
</code></pre>
  <h4>Base64 encoded GZip compressed data</h4>
  <p>
    Some APIs, such as
    <a href="https://docs.github.com/en/free-pro-team@latest/rest/code-scanning/code-scanning?apiVersion=2022-11-28#upload-an-analysis-as-sarif-data" target=_blank>uploading to GitHub analysis as SARIF data</a>,
    may require compressing data and including the compressed payload as part
    of a plain text JSON request.  This example highlights getting base64
    encoded compressed data.  You can find a more advanced example in <tt>{@link net.gleske.jervis.remotes.GitHub}</tt> class documentation.
  </p>
<pre><code>
import net.gleske.jervis.tools.GZip
import net.gleske.jervis.tools.SecurityIO
import java.util.zip.GZIPInputStream

ByteArrayOutputStream compressed = new ByteArrayOutputStream()
// best speed compression
new GZip(compressed, 1).withCloseable {
    it &lt;&lt; 'hello'
    it &lt;&lt; ' world'
}

// COMPRESSED data encoded as base64
String data = SecurityIO.encodeBase64(compressed.toByteArray())

// DECOMPRESS example
ByteArrayOutputStream plain = new ByteArrayOutputStream()
new ByteArrayInputStream(SecurityIO.decodeBase64Bytes(data)).withCloseable { is -&gt;
    new GZIPInputStream(is).withCloseable { gunzip -&gt;
        plain &lt;&lt; gunzip
    }
}
assert plain.toString() == 'hello world'
</code></pre>
 */

 class GZip extends GZIPOutputStream {

    /**
      Creates a <tt>{@link java.util.zip.GZIPOutputStream}</tt> with maximum compression enabled.
      @see java.util.zip.Deflater
      @param os An <tt>{@link java.io.OutputStream}</tt> to wrap which will write out compressed data.
      @param level A compression level between fastest (<tt>1</tt>) and best compression (<tt>9</tt>).
      */
    GZip(OutputStream os, Integer level = Deflater.BEST_COMPRESSION) {
        super(os)
        setLevel(level)
    }

    /**
      This can change the compression level before you write data to <tt>GZip</tt> after it was instantiated.
      @param level A compression level between fastest (<tt>1</tt>) and best compression (<tt>9</tt>).
      */
    void setLevel(Integer level) {
        this.@def.setLevel(level)
    }

    /**
      Write a <tt>String</tt> similar to how you would write to a file with <tt>leftShift</tt> on a <tt>{@link java.io.Writer}</tt>.
      @param shiftString A string which will be written out after being compressed.
      */
    OutputStream leftShift(String shiftString) {
        leftShift(shiftString.bytes)
    }
}
