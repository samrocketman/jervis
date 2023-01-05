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

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer

/**
  A utility class for centralizing basic YAML operations required by Jervis.
  Reads and writes YAML.  This utility class is necessary because of a few
  major changes around how YAML loading works due to vulnerabilities or usage.
  So this utility class is to help minimize required changes to YAML usage.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
<pre><code>
import net.gleske.jervis.tools.YamlOperator

String yaml = '''
key: value
list:
  - one
  - two
  - three
'''

// Load YAML from many object types
Map result = YamlOperator.loadYamlFrom(yaml)

// Export YAML as a String
println(YamlOperator.writeObjToYaml(result))

// Write YAML to a File
YamlOperator.writeObjToYaml(new File('/tmp/file.yaml')), result)
</code></pre>
  */
class YamlOperator {
    private YamlOperator() {
        throw new IllegalStateException('ERROR: This utility class only provides static methods and is not meant for instantiation.  See Java doc for this class for examples.')
    }

    /**
      Convert a POJO consisting of standard Java classes into a YAML string.
      @param yamlToSerialize A map object consisting of standard Java class
                             instance objects.
      @return A YAML-spec String.
      */
    static String writeObjToYaml(Map yamlToSerialize) {
        DumperOptions options = new DumperOptions()
        options.setIndent(2)
        options.setPrettyFlow(true)
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        def yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(), options)
        yaml.dumpAsMap(yamlToSerialize)
    }

    /**
      Convert a POJO consisting of standard Java classes into YAML written in a
      file.  This will overwrite the provided file if it exists.

      @param destFile A file where the YAML output will be written.
      @param yamlToSerialize A map object consisting of standard Java class
                             instance objects.
      */
    static void writeObjToYaml(File destFile, Map yamlToSerialize) {
        destFile.withWriter('UTF-8') { Writer w ->
            w << writeObjToYaml(yamlToSerialize)
        }
    }

    /**
      Parse YAML from an <tt>InputStream</tt>.

      @param srcStream A stream which contains String-data consisting of YAML.
      @return A plain old Java object consisting of standard Java classes.
      */
    static def loadYamlFrom(InputStream srcStream) {
        LoaderOptions options = new LoaderOptions()
        options.allowDuplicateKeys = true
        options.allowRecursiveKeys = false
        // 5MB data limit?  code point limit is not well explained
        options.codePointLimit = 5242880
        options.maxAliasesForCollections = 500
        options.nestingDepthLimit = 500
        options.processComments = false
        options.wrappedToRootException = false
        def yaml = new Yaml(new SafeConstructor(options))
        yaml.load(srcStream)
    }

    /**
      Parse YAML from a <tt>String</tt>.

      @param srcString A <tt>String</tt> which contains YAML to be parsed.
      @return A plain old Java object consisting of standard Java classes.
      */
    static def loadYamlFrom(String srcString) {
        loadYamlFrom(new ByteArrayInputStream(srcString.bytes))
    }

    /**
      Parse YAML from a <tt>StringWriter</tt> which is typically used by Jervis
      as an in-memory <tt>String</tt> buffer.

      @param srcString A <tt>String</tt> which contains YAML to be parsed.
      @return A plain old Java object consisting of standard Java classes.
      */
    static def loadYamlFrom(StringWriter srcString) {
        loadYamlFrom(new ByteArrayInputStream(srcString.toString().bytes)) ?: ''
    }

    /**
      Parse YAML from a <tt>Byte</tt> array.

      @param srcBytes A <tt>Byte</tt> array which contains YAML to be parsed.
      @return A plain old Java object consisting of standard Java classes.
      */
    static def loadYamlFrom(byte[] srcBytes) {
        loadYamlFrom(new ByteArrayInputStream(srcBytes))
    }

    /**
      Given a file, this will load YAML from a File and return a Java object
      consisting of standard classes.
      @param srcFile A file with YAML content.
      @return A plain old Java object consisting of standard Java classes.
      */
    static def loadYamlFrom(File srcFile) {
        loadYamlFrom(new FileInputStream(srcFile))
    }
}
