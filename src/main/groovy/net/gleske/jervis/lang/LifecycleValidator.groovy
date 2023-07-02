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
package net.gleske.jervis.lang

import net.gleske.jervis.exceptions.LifecycleBadValueInKeyException
import net.gleske.jervis.exceptions.LifecycleInfiniteLoopException
import net.gleske.jervis.exceptions.LifecycleMissingKeyException
import net.gleske.jervis.exceptions.LifecycleValidationException
import net.gleske.jervis.tools.YamlOperator

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
<pre><code>
import net.gleske.jervis.lang.LifecycleValidator

def lifecycles = new LifecycleValidator()
lifecycles.loadYamlFile('resources/lifecycles-ubuntu2204-stable.yaml')
println 'Does the file validate? ' + lifecycles.validate()
print 'Supported languages include:\n    '
println lifecycles.languages.collect {
    lifecycles.lifecycles[it]['friendlyName']
}.sort().join('\n    ')
</code></pre>
 */
class LifecycleValidator implements Serializable {
    /**
      Check if there is an unstable lifecycles object available.
      */
    private Boolean isUnstable(Boolean unstable) {
        unstable && this.unstable_lifecycles
    }

    /**
      A <tt>{@link Map}</tt> of the parsed lifecycles file.
     */
    Map lifecycles

    /**
      Get lifecycles or optionally unstable lifecycles.
      @param unstable Request unstable instead of stable.
      @return A <tt>{@link Map}</tt> of the parsed lifecycles files (could include unstable).
      */
    Map getLifecycles(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_lifecycles : this.@lifecycles
    }

    /**
      A <tt>{@link Map}</tt> of the parsed unstable lifecycles file.
     */
    Map unstable_lifecycles

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the lifecycles file.  This is just a list of the
      keys in {@link #lifecycles}.
     */
    String[] languages

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the lifecycles file.  This is just a list of the
      keys in {@link #lifecycles}.
      @param unstable Request unstable languages instead of stable.
      @return Returns a languages list.
     */
    String[] getLanguages(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_languages : this.@languages
    }

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the unstable lifecycles file.  This is just a list
      of the keys in {@link #unstable_lifecycles}.
     */
    String[] unstable_languages

    /**
      Load the YAML of a lifecycles file and parse it.  This should be the first
      function called after class instantiation.  Alternately,
      <tt>{@link #loadYamlString()}</tt> can be called instead.  It populates
      <tt>{@link #lifecycles}</tt> and <tt>{@link #languages}</tt>.
      @param file A <tt>String</tt> which is a path to a lifecycles file.
     */
    public void loadYamlFile(String file, Boolean unstable = false) {
        loadYamlString(new File(file).text, unstable)
    }

    /**
      Parse the YAML which is the contents of a lifecycles file.  It populates
      <tt>{@link #lifecycles}</tt> and <tt>{@link #languages}</tt>.  This is required
      in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job DSL Plugin.
      @param yaml A <tt>String</tt> containing the contents of a lifecycles file.
     */
    public void loadYamlString(String yaml, Boolean unstable = false) {
        if(unstable) {
            Map tempLifecycles = YamlOperator.deepCopy(this.@lifecycles)
            tempLifecycles.putAll(YamlOperator.loadYamlFrom(yaml) ?: [:])
            this.unstable_lifecycles = tempLifecycles
            this.unstable_languages = this.unstable_lifecycles.keySet() as String[];
        }
        else {
            this.lifecycles = YamlOperator.loadYamlFrom(yaml) ?: [:]
            this.languages = lifecycles.keySet() as String[];
        }
    }

    /**
      Checks to see if a language is a supported language based on the lifecycles file.
      @param lang A <tt>String</tt> which is a language to look up based on the keys in the lifecycles file.
      @return     <tt>true</tt> if the language is supported or <tt>false</tt> if the language is not supported.
     */
    public Boolean supportedLanguage(String lang, Boolean unstable = false) {
        lang in this.getLanguages(unstable)
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the lifecycles file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate(false)
            this.validate(true)
            return true
        }
        catch(LifecycleValidationException E) {
            return false
        }
    }

    /**
      Validates the lifecycles file.  Covers both unstable and stable.
      @return <tt>true</tt> if the lifecycles file validates.  If the lifecycles file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.LifecycleValidationException}</tt>.
     */
    Boolean validate() {
        this.validate(false)
        this.validate(true)
    }
    /**
      Validates the lifecycles file.
      @param unstable Validate as an unstable instead of stable.
      @return <tt>true</tt> if the lifecycles file validates.  If the lifecycles file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.LifecycleValidationException}</tt>.
     */
    public Boolean validate(Boolean unstable) throws LifecycleMissingKeyException, LifecycleBadValueInKeyException, LifecycleInfiniteLoopException {
        Map lifecycles = this.getLifecycles(unstable)
        lifecycles.each { language, lifecycleMap ->
            if(!(lifecycleMap in Map)) {
                throw new LifecycleMissingKeyException([language, 'must contain a Map but found:', lifecycleMap.getClass()].join(' '))
            }
            if(!(lifecycleMap.defaultKey in String)) {
                throw new LifecycleMissingKeyException([language,'defaultKey'].join('.'))
            }
            if(!(lifecycleMap.friendlyName in String)) {
                throw new LifecycleMissingKeyException([language, 'friendlyName'].join('.'))
            }
            String current_key = lifecycleMap.defaultKey
            if(!(current_key in lifecycleMap.keySet())) {
                throw new LifecycleMissingKeyException([language, 'defaultKey', current_key].join('.'))
            }
            Integer count = 0
            while(current_key) {
                Map cycles = YamlOperator.getObjectValue(lifecycles, "\"${language}\".\"${current_key}\"", [:])
                if(!cycles) {
                    break
                }
                if(cycles.fallbackKey) {
                    if(!(cycles.fallbackKey in lifecycleMap.keySet())) {
                        throw new LifecycleMissingKeyException([language, current_key, 'fallbackKey', cycles.fallbackKey].join('.'))
                    }
                    if(!cycles.fileExistsCondition) {
                        throw new LifecycleMissingKeyException([language, current_key, 'fileExistsCondition'].join('.') + ' required by ' + [language, current_key, 'fallbackKey'].join('.'))
                    }
                }
                count++
                if(count > 1000) {
                    throw new LifecycleInfiniteLoopException([language, current_key].join('.'))
                }
                current_key = cycles.fallbackKey
            }
        }
        return true
    }
}
