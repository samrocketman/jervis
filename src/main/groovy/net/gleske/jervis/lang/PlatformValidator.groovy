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

import net.gleske.jervis.exceptions.PlatformBadValueInKeyException
import net.gleske.jervis.exceptions.PlatformMissingKeyException
import net.gleske.jervis.exceptions.PlatformValidationException
import net.gleske.jervis.tools.YamlOperator

/**
  Validates the contents of a
  <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-platforms-file" target="_blank">platforms file</a>
  and provides quick access to supported platforms.

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
import net.gleske.jervis.lang.PlatformValidator

def platforms = new PlatformValidator()
platforms.loadYamlFile('resources/platforms.yaml')
println 'Does the file validate? ' + platforms.validate()
//List supported platforms and operating systems
platforms.platforms['supported_platforms'].sort { k, v -&gt; k }.each { platform, os -&gt;
    println "Platform '${platform}' includes operating systems:"
    os.sort { k, v -&gt; k }.each { o, s -&gt;
        println "    ${o} (Supported languages: ${s['language'].sort().join(', ')})"
    }
}
null
</code></pre>
 */
class PlatformValidator implements Serializable {
    /**
      Check if there is an unstable platforms object available.
      */
    private Boolean isUnstable(Boolean unstable) {
        unstable && this.unstable_platforms
    }

    /**
      A <tt>{@link Map}</tt> of the parsed platforms file.
     */
    Map platforms

    Map getPlatforms(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_platforms : this.@platforms
    }

    /**
      A <tt>{@link Map}</tt> of the parsed unstable platforms file.
     */
    Map unstable_platforms

    /**
      Load the YAML of a platforms file and parse it.  This should be the first
      function called after class instantiation.  Alternately,
      <tt>{@link #loadYamlString()}</tt> can be called instead.  It populates
      <tt>{@link #platforms}</tt>.
      @param file A <tt>String</tt> which is a path to a platforms file.
      @param unstable Load unstable platforms instead of stable.
     */
    public void loadYamlFile(String file, Boolean unstable = false) {
        loadYamlString(new File(file).text, unstable)
    }

    /**
      Parse the YAML which is the contents of a platforms file.  It populates
      <tt>{@link #platforms}</tt>.  This is required in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job DSL Plugin.
      @param yaml A <tt>String</tt> containing the contents of a platforms file.
      @param unstable Load unstable platforms instead of stable.
     */
    public void loadYamlString(String yaml, Boolean unstable = false) {
        if(unstable) {
            // perform a deep merge of unstable YAML with stable YAML
            Map tempPlatforms = YamlOperator.deepCopy(this.@platforms)
            Map tempYaml = YamlOperator.loadYamlFrom(yaml) ?: [:]
            ['defaults', 'supported_platforms', 'restrictions'].each { String topKey ->
                if(topKey in tempYaml.keySet()) {
                    if(topKey == 'supported_platforms') {
                        // pv is platform value
                        tempYaml[topKey].each { platform, pv ->
                            if(!(platform in tempPlatforms[topKey].keySet())) {
                                tempPlatforms[topKey][platform] = pv
                                // skip to next platform
                                return
                            }
                            // ov is OS value
                            pv.each { os, ov ->
                                if(!(os in tempPlatforms[topKey][platform].keySet())) {
                                    tempPlatforms[topKey][platform][os] = ov
                                    // skip to next OS
                                    return
                                }
                                // OS exists in both places so merge language and toolchain lists
                                List addLanguage = YamlOperator.getObjectValue(ov, 'language', [])
                                List addToolchain = YamlOperator.getObjectValue(ov, 'toolchain', [])
                                List currentLanguage = YamlOperator.getObjectValue(tempPlatforms, "supported_platforms.\"${platform}\".\"${os}\".language", [])
                                List currentToolchain = YamlOperator.getObjectValue(tempPlatforms, "supported_platforms.\"${platform}\".\"${os}\".toolchain", [])
                                tempPlatforms['supported_platforms'][platform][os].putAll(ov)
                                tempPlatforms['supported_platforms'][platform][os].language = (currentLanguage + addLanguage).sort().unique()
                                tempPlatforms['supported_platforms'][platform][os].toolchain = (currentToolchain + addToolchain).sort().unique()
                            }
                        }
                    }
                    else {
                        tempPlatforms[topKey].putAll(tempYaml[topKey])
                    }
                }
            }
            this.unstable_platforms = tempPlatforms
        }
        else {
            this.platforms = YamlOperator.loadYamlFrom(yaml) ?: [:]
        }
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the platforms file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate(false)
            this.validate(true)
            return true
        }
        catch(PlatformValidationException E) {
            return false
        }
    }
    /**
      Validates the platforms file.
      @return <tt>true</tt> if the platforms file validates.  If the platforms file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.PlatformValidationException}</tt>.
     */
    public Boolean validate() throws PlatformMissingKeyException, PlatformBadValueInKeyException, PlatformValidationException {
        validate(false)
        validate(true)
    }

    /**
      Validates the platforms file.
      @param
      @return <tt>true</tt> if the platforms file validates.  If the platforms file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.PlatformValidationException}</tt>.
     */
    public Boolean validate(Boolean unstable) throws PlatformMissingKeyException, PlatformBadValueInKeyException, PlatformValidationException {
        Map platforms = getPlatforms(unstable)
        //check for required root keys and types
        ['defaults', 'supported_platforms', 'restrictions'].each {
            if(!platforms.containsKey(it)) {
                throw new PlatformMissingKeyException("${it} - Must exist as a root key.")
            }
            if(!(platforms[it] instanceof Map)) {
                throw new PlatformBadValueInKeyException("${it} - Must be a YAML Object.")
            }
        }
        //validate defaults root key for keys, types, and  values
        ['platform', 'os', 'stability', 'sudo'].each {
            if(!platforms['defaults'].containsKey(it)) {
                throw new PlatformMissingKeyException(['defaults', it].join('.'))
            }
            if(!(platforms['defaults'][it] instanceof String)) {
                throw new PlatformBadValueInKeyException(['defaults', it].join('.'))
            }
        }
        String default_platform = platforms['defaults']['platform']
        String default_os = platforms['defaults']['os']
        if(!platforms['supported_platforms'].containsKey(default_platform)) {
            throw new PlatformMissingKeyException(['supported_platforms', default_platform].join('.') + ' - Missing default platform.')
        }
        if(!(platforms['supported_platforms'][default_platform] instanceof Map)) {
            throw new PlatformBadValueInKeyException("supported_platforms.${default_platform} - Must be a YAML Object.")
        }
        if(!platforms['supported_platforms'][default_platform].containsKey(default_os)) {
            throw new PlatformMissingKeyException(['supported_platforms', default_platform, default_os].join('.') + ' - Missing default OS.')
        }
        if(platforms['defaults']['stability'] != 'stable' && platforms['defaults']['stability'] != 'unstable') {
            throw new PlatformBadValueInKeyException(['defaults', 'stability'].join('.') + ' - Must be stable or unstable.')
        }
        if(platforms['defaults']['sudo'] != 'sudo' && platforms['defaults']['sudo'] != 'nosudo') {
            throw new PlatformBadValueInKeyException(['defaults', 'sudo'].join('.') + ' - Must be sudo or nosudo.')
        }
        //validate the supported_platforms root key for keys, types, and values
        (platforms['supported_platforms'].keySet() as String[]).each {
            String platform = it
            if(!(platforms['supported_platforms'][platform] instanceof Map)) {
                throw new PlatformBadValueInKeyException("supported_platforms.${platform} - Must be a YAML Object.")
            }
            if((platforms['supported_platforms'][platform].keySet() as String[]).size() <= 0) {
                throw new PlatformBadValueInKeyException("supported_platforms.${platform} - YAML Object must not be empty.")
            }
            (platforms['supported_platforms'][platform].keySet() as String[]).each {
                String os = it
                if(!(platforms['supported_platforms'][platform][os] instanceof Map)) {
                    throw new PlatformBadValueInKeyException(['supported_platforms', platform, os].join('.') + ' - Must be a YAML Object.')
                }
                ['language', 'toolchain'].each {
                    if(!platforms['supported_platforms'][platform][os].containsKey(it)) {
                        throw new PlatformMissingKeyException(['supported_platforms', platform, os, it].join('.'))
                    }
                    else if(!(platforms['supported_platforms'][platform][os][it] instanceof List)) {
                        throw new PlatformBadValueInKeyException(['supported_platforms', platform, os, it].join('.') + ' - Must be a YAML Array.')
                    }
                }
            }
        }
        //validate restrictions root key for keys, types, and values
        (platforms['restrictions'].keySet() as String[]).each {
            String platform = it
            if(!(platforms['restrictions'][platform] instanceof Map)) {
                throw new PlatformBadValueInKeyException(['restrictions', platform].join('.') + ' - Must be a YAML Object.')
            }
            if(!platforms['supported_platforms'].containsKey(platform)) {
                throw new PlatformMissingKeyException(['supported_platforms', platform].join('.') + ' - Missing restricted platform.')
            }
            ['only_organizations', 'only_projects'].each {
                if(platforms['restrictions'][platform].containsKey(it)) {
                    if(!(platforms['restrictions'][platform][it] instanceof List)) {
                        throw new PlatformBadValueInKeyException(['restrictions', platform, it].join('.') + ' - Must be a YAML Array.')
                    }
                }
            }
        }
        return true
    }
}
