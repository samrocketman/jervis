/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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

import org.yaml.snakeyaml.Yaml
import net.gleske.jervis.exceptions.LifecycleBadValueInKeyException
import net.gleske.jervis.exceptions.LifecycleInfiniteLoopException
import net.gleske.jervis.exceptions.LifecycleMissingKeyException
import net.gleske.jervis.exceptions.LifecycleValidationException

/**
  Validates the contents of a
  <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycle file</a>
  and provides quick access to supported languages.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
  <p><b>Please note</b>, if you are writing Job DSL plugin groovy scripts you
  should not use the relative file paths to access files in the repository
  where your DSL scripts reside.  Instead, use the
  <a href="https://github.com/samrocketman/jervis/issues/43" target="_blank"><tt>readFileFromWorkspace</tt></a>
  method provided by the Job DSL plugin in Jenkins.</p>
<pre><tt>import net.gleske.jervis.lang.lifecycleValidator

def lifecycles = new lifecycleValidator()
lifecycles.load_JSON('resources/lifecycles-ubuntu1604-stable.json')
println 'Does the file validate? ' + lifecycles.validate()
print 'Supported languages include:\n    '
println lifecycles.languages.collect {
    lifecycles.lifecycles[it]['friendlyName']
}.sort().join('\n    ')</tt></pre>
 */
class lifecycleValidator implements Serializable {

    /**
      A <tt>{@link Map}</tt> of the parsed lifecycles file.
     */
    Map lifecycles

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of supported languages in the lifecycles file.  This is just a list of the keys in {@link #lifecycles}.
     */
    String[] languages

    /**
      Load the JSON of a lifecycles file and parse it.  This should be the first
      function called after class instantiation.  Alternately,
      <tt>{@link #load_JSONString()}</tt> can be called instead.  It populates
      <tt>{@link #lifecycles}</tt> and <tt>{@link #languages}</tt>.
      @param file A <tt>String</tt> which is a path to a lifecycles file.
     */
    public void load_JSON(String file) {
        load_JSONString(new File(file).text)
    }

    /**
      Parse the JSON which is the contents of a lifecycles file.  It populates
      <tt>{@link #lifecycles}</tt> and <tt>{@link #languages}</tt>.  This is required
      in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job DSL Plugin.
      @param json A <tt>String</tt> containing the contents of a lifecycles file.
     */
    public void load_JSONString(String json) {
        def yaml = new Yaml()
        lifecycles = yaml.load(json)?: [:]
        languages = lifecycles.keySet() as String[];
    }

    /**
      Checks to see if a language is a supported language based on the lifecycles file.
      @param lang A <tt>String</tt> which is a language to look up based on the keys in the lifecycles file.
      @return     <tt>true</tt> if the language is supported or <tt>false</tt> if the language is not supported.
     */
    public Boolean supportedLanguage(String lang) {
        lang in languages
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the lifecycles file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate()
            return true
        }
        catch(LifecycleValidationException E) {
            return false
        }
    }
    /**
      Validates the lifecycles file.
      @return <tt>true</tt> if the lifecycles file validates.  If the lifecycles file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.LifecycleValidationException}</tt>.
     */
    public Boolean validate() throws LifecycleMissingKeyException, LifecycleBadValueInKeyException, LifecycleInfiniteLoopException {
        lifecycles.keySet().each {
            def tools = lifecycles[it].keySet() as String[]
            if(!('defaultKey' in tools)) {
                throw new LifecycleMissingKeyException([it,'defaultKey'].join('.'))
            }
            if(!('friendlyName' in tools)) {
                throw new LifecycleMissingKeyException([it,'friendlyName'].join('.'))
            }
            if(!(lifecycles[it]['defaultKey'] in tools)) {
                throw new LifecycleMissingKeyException([it,'defaultKey',lifecycles[it]['defaultKey']].join('.'))
            }
            def current_key = lifecycles[it]['defaultKey']
            def count=0
            while(current_key != null) {
                def cycles = lifecycles[it][current_key].keySet() as String[]
                if('fallbackKey' in cycles) {
                    if(!(lifecycles[it][current_key]['fallbackKey'] in tools)) {
                        throw new LifecycleMissingKeyException([it,current_key,'fallbackKey',lifecycles[it][current_key]['fallbackKey']].join('.'))
                    }
                    if(!('fileExistsCondition' in cycles)) {
                        throw new LifecycleMissingKeyException([it,current_key,'fileExistsCondition'].join('.') + ' required by ' + [it,current_key,'fallbackKey'].join('.'))
                    }
                }
                count++
                if(count > 1000) {
                    throw new LifecycleInfiniteLoopException([it,current_key].join('.'))
                }
                current_key = lifecycles[it][current_key]['fallbackKey']
            }
        }
        return true
    }
}
