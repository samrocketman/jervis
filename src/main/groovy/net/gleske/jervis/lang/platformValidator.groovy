/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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
import net.gleske.jervis.exceptions.PlatformBadValueInKeyException
import net.gleske.jervis.exceptions.PlatformMissingKeyException
import net.gleske.jervis.exceptions.PlatformValidationException

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
<pre><tt>import net.gleske.jervis.lang.platformValidator

def platforms = new platformValidator()
platforms.load_JSON('resources/platforms.json')
println 'Does the file validate? ' + platforms.validate()
//List supported platforms and operating systems
platforms.platforms['supported_platforms'].sort { k, v -> k }.each { platform, os ->
    println "Platform '${platform}' includes operating systems:"
    os.sort { k, v -> k }.each { o, s ->
        println "    ${o} (Supported languages: ${s['language'].sort().join(', ')})"
    }
}
null</tt></pre>
 */
class platformValidator implements Serializable {

    /**
      A <tt>{@link Map}</tt> of the parsed platforms file.
     */
    Map platforms

    /**
      Load the JSON of a platforms file and parse it.  This should be the first
      function called after class instantiation.  Alternately,
      <tt>{@link #load_JSONString()}</tt> can be called instead.  It populates
      <tt>{@link #platforms}</tt>.
      @param file A <tt>String</tt> which is a path to a platforms file.
     */
    public void load_JSON(String file) {
        load_JSONString(new File(file).text)
    }

    /**
      Parse the JSON which is the contents of a platforms file.  It populates
      <tt>{@link #platforms}</tt>.  This is required in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job DSL Plugin.
      @param json A <tt>String</tt> containing the contents of a platforms file.
     */
    public void load_JSONString(String json) {
        def yaml = new Yaml()
        platforms = yaml.load(json)?: [:]
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the platforms file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate()
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
        //check for required root keys and types
        ['defaults', 'supported_platforms', 'restrictions'].each {
            if(!platforms.containsKey(it)) {
                throw new PlatformMissingKeyException("${it} - Must exist as a root key.")
            }
            if(!(platforms[it] instanceof Map)) {
                throw new PlatformBadValueInKeyException("${it} - Must be a JSON Object.")
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
            throw new PlatformBadValueInKeyException("supported_platforms.${default_platform} - Must be a JSON Object.")
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
                throw new PlatformBadValueInKeyException("supported_platforms.${platform} - Must be a JSON Object.")
            }
            if((platforms['supported_platforms'][platform].keySet() as String[]).size() <= 0) {
                throw new PlatformBadValueInKeyException("supported_platforms.${platform} - JSON Object must not be empty.")
            }
            (platforms['supported_platforms'][platform].keySet() as String[]).each {
                String os = it
                if(!(platforms['supported_platforms'][platform][os] instanceof Map)) {
                    throw new PlatformBadValueInKeyException(['supported_platforms', platform, os].join('.') + ' - Must be a JSON Object.')
                }
                ['language', 'toolchain'].each {
                    if(!platforms['supported_platforms'][platform][os].containsKey(it)) {
                        throw new PlatformMissingKeyException(['supported_platforms', platform, os, it].join('.'))
                    }
                    else if(!(platforms['supported_platforms'][platform][os][it] instanceof List)) {
                        throw new PlatformBadValueInKeyException(['supported_platforms', platform, os, it].join('.') + ' - Must be a JSON Array.')
                    }
                }
            }
        }
        //validate restrictions root key for keys, types, and values
        (platforms['restrictions'].keySet() as String[]).each {
            String platform = it
            if(!(platforms['restrictions'][platform] instanceof Map)) {
                throw new PlatformBadValueInKeyException(['restrictions', platform].join('.') + ' - Must be a JSON Object.')
            }
            if(!platforms['supported_platforms'].containsKey(platform)) {
                throw new PlatformMissingKeyException(['supported_platforms', platform].join('.') + ' - Missing restricted platform.')
            }
            ['only_organizations', 'only_projects'].each {
                if(platforms['restrictions'][platform].containsKey(it)) {
                    if(!(platforms['restrictions'][platform][it] instanceof List)) {
                        throw new PlatformBadValueInKeyException(['restrictions', platform, it].join('.') + ' - Must be a JSON Array.')
                    }
                }
            }
        }
        return true
    }
}
