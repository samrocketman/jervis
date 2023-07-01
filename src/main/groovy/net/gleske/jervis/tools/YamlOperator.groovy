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
      @param yamlToSerialize A POJO consisting of standard Java class instance
                             objects.
      @return A YAML-spec String.
      */
    static String writeObjToYaml(def yamlToSerialize) {
        DumperOptions options = new DumperOptions()
        options.setIndent(2)
        options.setPrettyFlow(true)
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        def yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options)
        yaml.dump(yamlToSerialize)
    }

    /**
      Convert a POJO consisting of standard Java classes into YAML written in a
      file.  This will overwrite the provided file if it exists.

      @param destFile A file where the YAML output will be written.
      @param yamlToSerialize A POJO consisting of standard Java class instance
                             objects.
      */
    static void writeObjToYaml(File destFile, def yamlToSerialize) {
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

    /**
      Get an object from a <tt>Map</tt> or return any object from
      <tt>defaultValue</tt>.  Guarantees that what is returned is the same type as
      <tt>defaultValue</tt>.  This is used to get optional keys from YAML or JSON
      files.
      <h5>Code Example</h5>
<pre><code>
import static net.gleske.jervis.tools.YamlOperator.getObjectValue

Map hexKeys = [
    'hello.io': 'world',
    hello: ['jervis.io{': 'friend'],
    friend: [name: 'dog']
]

// Do a normal lookup with a hierarchy of keys
assert getObjectValue(hexKeys, 'friend.name', '') == 'dog'

// Do a normal lookup with a hierarchy of keys; but result is not a List so
// falls back to default
assert getObjectValue(hexKeys, 'friend.name', [['item']]) == ['item']

// The above is not to be confused with a List of default fallbacks.  Which
// would result in the dog being returned.
assert getObjectValue(hexKeys, 'friend.name', ['item']) == 'dog'

// top level lookup; but the key has a period in it
assert getObjectValue(hexKeys, 'hello\\.io', '') == 'world'

// top level lookup; but the period is already escaped with a hex code.
assert getObjectValue(hexKeys, 'hello%{2e}io', '') == 'world'

// A hierarchy of keys but some of the child keys have special characters
assert getObjectValue(hexKeys, 'hello.jervis\\.io\\{', '') == 'friend'

// quoting works, too
assert getObjectValue(hexKeys, '"hello.io"', '') == 'world'
assert getObjectValue(hexKeys, 'hello."jervis.io"{', '') == 'friend'

// you can request fallback expressions by separting expressions with ' // '
assert getObjectValue(hexKeys, 'hello."jervis.io"{ // friend', [:]) == [name: 'dog']

// Check the hello key for multiple types and if none, then return the first
// default.  The following will check for a String or a List as the default.
assert getObjectValue(hexKeys, 'hello', [ '', [] ]) == ''

// and now the same using both search fallback and default fallback
assert getObjectValue(hexKeys, 'hello // friend.name', [ '', [] ]) == 'dog'
</code></pre>

      <h5>Other notable features</h5>

      <p>In the above example, notice that some characters can be escaped.  For example, if the key has a period in its name it is escaped with <tt>\\.</tt>.  Any character can be escaped this way.</p>

      <p><tt>key</tt> can take multiple fallback expressions if you separate expressions with <tt> // </tt>.</p>

      <p><tt>defaultValue</tt> can take multiple fallback defaults.  So you can search for multiple types on a single path falling back to ones you support.</p>

      <p>If you need a List of items to be the <tt>defaultValue</tt>, then you must nest it within a List such as <tt>[['item']]</tt>.  This List contains a List of one item.</p>

      @param object A <tt>Map</tt> which was likely created from a YAML or JSON file.
      @param key A <tt>String</tt> with keys and subkeys separated by periods which is
                 used to search the <tt>object</tt> for a possible value.
      @param defaultValue A default value and type that should be returned.
      @return Returns the value of the key or a <tt>defaultValue</tt> which is of the
              same type as <tt>defaultValue</tt>.  This function has three coercion
              behaviors which is not the same as Groovy:
      <ol class="numbered">
        <li>
          If <tt>defaultValue</tt> is a non-zero list of items, then each item
          in the list will be treated as a series of fallback defaults.  If no
          default matchies the search then the first item of the list becomes
          the returned default.
        </li>
        <li>
          If the <tt>defaultValue</tt> is an instance of <tt>String</tt> and the
          retrieved key is an instance of <tt>Map</tt>, then <tt>defaultValue</tt> is
          returned rather than converting it to a <tt>String</tt>.
        </li>
        <li>
          If the <tt>defaultValue</tt> is an instance of <tt>String</tt> and the
          retrieved key is an instance of <tt>List</tt>, then <tt>defaultValue</tt> is
          returned rather than converting it to a <tt>String</tt>.
        </li>
        <li>
          If the <tt>defaultValue</tt> is an instance of <tt>Boolean</tt>, the
          retrieved key is an instance of <tt>String</tt> and has a value of
          <tt>false</tt>, then <tt>Boolean false</tt> is returned.
        </li>
      </ol>
     */
    public static final Object getObjectValue(Map object, String key, Object defaultValue) {
        getObjectValueRecurse(object, key, defaultValue, 0)
    }

    /**
      This non-public method is used to control recusion for returning default
      values.

      @param recursionDepth Ignore this parameter.  It is used internally by
                            this method to short circuit recursion.
      */
    private static final Object getObjectValueRecurse(Map object, String key, Object defaultValue, Integer recursionDepth) {
        if(key.contains(' // ')) {
            List keyResults = key.tokenize(' // ').collect {
                getObjectValueRecurse(object, it, defaultValue, 0)
            }
            return keyResults.findAll {
                if(defaultValue in List && defaultValue.size() > 0) {
                    !(it in defaultValue)
                }
                else {
                    it != defaultValue
                }
            }.with {
                if(it.size() > 0) {
                    return it.first()
                }
                if(defaultValue in List && defaultValue.size() > 0) {
                    return defaultValue.first()
                }
                return defaultValue
            }
        }
        if(recursionDepth == 0 && defaultValue in List && defaultValue.size() > 0) {
            List defaultValueResults = defaultValue.collect {
                [it, getObjectValueRecurse(object, key, it, recursionDepth + 1)]
            }
            return defaultValueResults.find {
                it[1] != null && it[0] != it[1]
            }?.get(1) ?: defaultValue.first()
        }
        // START OF ENCODER AND DECODER SETUP
        // find all backslash charachters and replace them with hex encoded values
        Map encoder = [:]
        key.eachMatch('\\\\.') {
            String matchedChar = it - ~'\\\\'
            encoder["\\Q${it}\\E".toString()] = "%{${matchedChar.bytes.encodeHex()}}".toString()
        }
        String encodedKey = key
        // replace backslash characters first
        encoder.each { k , v -> encodedKey = encodedKey.replaceAll(k, v) }
        // add quoted samples to replacements within encoder
        encodedKey.findAll( '"[^"]+"' ).each {
            encoder["\\Q${it}\\E".toString()] = it[1..-2].replaceAll('\\.', '\\\\\\\\.')
        }
        // quoted strings and add their replacement to the encoder
        encoder.each { k , v -> encodedKey = encodedKey.replaceAll(k, v) }
        // update encoder with new escaped characters after quote replacement
        encodedKey.eachMatch('\\\\.') {
            String matchedChar = it - ~'\\\\'
            encoder["\\Q${it}\\E".toString()] = "%{${matchedChar.bytes.encodeHex()}}".toString()
        }
        // produce the final string with all portions escaped
        encoder.each { k , v -> encodedKey = encodedKey.replaceAll(k, v) }
        //encodedKey now has hex variants

        // set a map of hex expressions to decode for lookups
        Map decoder = encodedKey.findAll('%\\{([^}]+)\\}') { [("\\Q${it[0]}\\E".toString()): (new String(it[1].decodeHex()))] }?.sum() ?: [:]
        // END OF ENCODER AND DECODER SETUP

        String primary = encodedKey
        if(encodedKey.indexOf('.') >= 0) {
            primary = encodedKey.split('\\.', 2)[0]
            String subkey = encodedKey.split('\\.', 2)[1]
            // decode the primary hex variants
            decoder.each { k, v -> primary = primary.replaceAll(k, v) }
            if(object.get(primary) != null && object.get(primary) instanceof Map) {
                return getObjectValueRecurse(object.get(primary), subkey, defaultValue, recursionDepth + 1)
            }
            else {
                return defaultValue
            }
        }

        // decode the primary hex variants
        decoder.each { k, v -> primary = primary.replaceAll(k, v) }

        //try returning the value casted as the same type as defaultValue
        try {
            if(object.get(primary) == null || ((defaultValue instanceof String) && ((object.get(primary) instanceof Map) || (object.get(primary) instanceof List)))) {
                return defaultValue
            }
            if((defaultValue instanceof Boolean) && (object.get(primary) == 'false')) {
                return false
            }
            return object.get(primary).asType(defaultValue.getClass())
        }
        catch(IllegalArgumentException|ClassCastException ignored) {
            return defaultValue
        }
    }

    /**
      Performs a deep clone of an object created from <tt>YamlOperator</tt>.
      This method serializes and deserializes the object to perform a deep
      copy; there's no cross-map references.  i.e. modifying child keys in one
      <tt>HashMap</tt> should not modify child keys in another.

      <tt>{@link java.util.HashMap#clone--}</tt> only shallow copies and does
      not account Maps within child keys.

      @param m An object that was parsed from YAML using <tt>YamlOperator</tt>
      @return A new instance of the Map and child keys as a 1:1 copy.
      */
    static def deepCopy(def m) {
        loadYamlFrom(writeObjToYaml(m))
    }
}
