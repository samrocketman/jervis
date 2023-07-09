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

import net.gleske.jervis.exceptions.MultiPlatformValidatorException
import net.gleske.jervis.tools.YamlOperator

/**
  Validates platforms, lifecycles, and toolchains for multiple operating
  systems; and their unstable counterparts; as a whole collection.  This helps
  guarantee, for a given platforms file, lifecycles and toolchains are loaded
  for mult-platform matrix support.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code class="language-groovy">
import net.gleske.jervis.beta.MultiPlatformValidator

MultiPlatformValidator platforms = new MultiPlatformValidator()
platforms.loadPlatformsString(new File('resources/platforms.yaml').text)
platforms.getToolchainFiles().each { String fileName -&gt;
    if(!new File("resources/${fileName}.yaml").exists()) { return }
    platforms.loadToolchainsString(fileName, new File("resources/${fileName}.yaml").text)
}
platforms.getLifecycleFiles().each { String fileName -&gt;
    if(!new File("resources/${fileName}.yaml").exists()) { return }
    platforms.loadLifecyclesString(fileName, new File("resources/${fileName}.yaml").text)
}
platforms.getGeneratorFromJervis(yaml: 'language: shell')
</code></pre>
  */
class MultiPlatformValidator implements Serializable {
    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.PlatformValidator}</tt> class which as loaded a platforms file.
     */
    PlatformValidator platform_obj

    /**
      All known platforms.
      */
    List known_platforms = []

    /**
      All known operating systems across all platforms.
      */
    List known_operating_systems = []

    /**
      All known toolchains across all operating systems.
      */
    List known_toolchains = []

    Map<String, LifecycleValidator> lifecycles = [:]
    Map<String, ToolchainValidator> toolchains = [:]

    /**
      Load a platforms YAML <tt>String</tt> so that advanced labels can be generated
      for multiple platforms.  A platform could be a local datacenter or a cloud
      providor.  The platforms file allows labels to be generated which include
      stability, sudo access, and even operating system.  This could be used to load
      lifecycles and toolchains by platform and OS.

      @param yaml A <tt>String</tt> containing YAML which is from a platforms file.
     */
    public void loadPlatformsString(String yaml) {
        this.platform_obj = new PlatformValidator()
        this.platform_obj.loadYamlString(yaml)
        this.platform_obj.validate()
        this.known_platforms = this.platform_obj.platforms.supported_platforms.keySet().toList()
        this.known_operating_systems = this.platform_obj.platforms.supported_platforms.collect { k, v ->
            v.keySet().toList()
        }.flatten().sort().unique()
    }

    List<String> getLifecycleFiles() {
        if(!this.platform_obj) {
            return
        }
        this.known_operating_systems.collect { String os ->
            [
                "lifecycles-${os}-stable",
                "lifecycles-${os}-unstable"
            ]
        }.flatten()*.toString()
    }

    List<String> getToolchainFiles() {
        if(!this.platform_obj) {
            return
        }
        this.known_operating_systems.collect { String os ->
            [
                "toolchains-${os}-stable",
                "toolchains-${os}-unstable"
            ]
        }.flatten()*.toString()
    }

    /**
      Load a lifecycles YAML <tt>String</tt> so that default scripts can be generated.
      Lifecycles provide the build portions of the script.

      @param yaml A <tt>String</tt> containing YAML which is from a lifecycles file.
     */
    public void loadLifecyclesString(String fileName, String yaml) {
        Boolean isUnstable = fileName.endsWith('unstable')
        String key = (fileName -~ '^lifecycles-') -~ '-(un)?stable$'
        if(!this.lifecycles[key]) {
            this.lifecycles[key] = new LifecycleValidator()
        }
        this.lifecycles[key].loadYamlString(yaml, isUnstable)
        this.lifecycles[key].validate()
    }

    /**
      Load a toolchains YAML <tt>String</tt> so that default scripts can be generated.
      Toolchains provide the default tool setup of the script (e.g. what version of
      Java will be used).

      @param fileName The file name without the extension.
      @param yaml A <tt>String</tt> containing YAML which is from a toolchains file.
     */
    public void loadToolchainsString(String fileName, String yaml) {
        Boolean isUnstable = fileName.endsWith('unstable')
        String key = (fileName -~ '^toolchains-') -~ '-(un)?stable$'
        if(!this.toolchains[key]) {
            this.toolchains[key] = new ToolchainValidator()
        }
        this.toolchains[key].loadYamlString(yaml, isUnstable)
        this.toolchains[key].validate()
        this.known_toolchains = (this.known_toolchains + this.toolchains[key].toolchain_list.toList()).sort().unique()
    }

    /**
      Creates a LifecycleGenerator object out of provided options.
Example with all options
getGeneratorFromJervis(yaml: '', folder_listing: []
      */
    LifecycleGenerator getGeneratorFromJervis(Map options) {
        LifecycleGenerator generator = new LifecycleGenerator()
        generator.multiPlatform = true
        generator.platform_obj = this.platform_obj
        if(options.yaml) {
            generator.preloadYamlString(options.yaml)
            generator.lifecycle_obj = this.lifecycles[generator.label_os]
            generator.toolchain_obj = this.toolchains[generator.label_os]
            generator.loadYamlString(options.yaml)
        }
        if(options.folder_listing in List) {
            generator.folder_listing = options.folder_listing
        }
        if(options.private_key in String && options.private_key) {
            generator.setPrivateKey(options.private_key)
            generator.decryptSecrets()
        }

        // return initialized generator
        generator
    }

    PipelineGenerator getPipelineGeneratorForJervis(Map options) {
        MultiPlatformGenerator platforms = new MultiPlatformGenerator(this)
    }

    void validate() {
        List errors = []
        // this.platform_obj must not be null
        if(!this.platform_obj) {
            errors << 'platform_obj not available.  Did you call loadPlatformsString(String yaml) method?'
        }
        // this.lifecycles must not be empty
        if(!this.lifecycles) {
            errors << 'lifecycles are empty.  Did you call loadLifecyclesString(String fileName, String yaml) method?'
        }
        // this.toolchains must not be empty
        if(!this.toolchains) {
            errors << 'toolchains are empty.  Did you call loadToolchainsString(String fileName, String yaml) method?'
        }
        // this.lifecycles.keySet() must all be known operating systems
        List bad_keys = this.lifecycles.keySet().toList() - known_operating_systems
        if(bad_keys) {
            errors << "lifecycles contains an OS not known to platforms.yaml.  Bad OSes: ${bad_keys.inspect()}"
        }
        // this.toolchains.keySet() must all be known operating systems
        bad_keys = this.toolchains.keySet().toList() - known_operating_systems
        if(bad_keys) {
            errors << "toolchains contains an OS not known to platforms.yaml.  Bad OSes: ${bad_keys.inspect()}"
        }
        // A platform MUST NOT be in toolchains
        bad_keys = this.known_platforms.intersect(this.known_toolchains)
        if(bad_keys) {
            errors << "A platform may not be also known as a toolchain.  Bad platforms: ${bad_keys.inspect()}"
        }
        // An OS MUST NOT be in toolchains
        bad_keys = this.known_operating_systems.intersect(this.known_toolchains)
        if(bad_keys) {
            errors << "An OS may not be also known as a toolchain.  Bad OSes: ${bad_keys.inspect()}"
        }
        // Every OS on every platform must have a LifecycleValidator and ToolchainValidator
        this.platform_obj.platforms.supported_platforms.each { platform, pv ->
            // pv or platform value; ov or os value
            pv.each { os, ov ->
                if(!(this.lifecycles[os] in LifecycleValidator)) {
                    errors << "In platforms.yaml, '${platform}'.'${os}' has no lifecycles file."
                }
                if(!(this.toolchains[os] in ToolchainValidator)) {
                    errors << "In platforms.yaml, '${platform}'.'${os}' has no toolchains file."
                }
            }
        }
        if(errors) {
            throw new MultiPlatformValidatorException("* " + errors.join('\n* '))
        }
    }

    void validateJervisYaml(Map jervis_yaml) {
        List errors = []
        [this.known_platforms, this.known_operating_systems].combinations().collect {
            [platform: it[0], os: it[1]]
        }.each { Map current ->
            // A platform MUST NOT contain any platform keys
            // A platform MUST NOT contain any key named 'jenkins', 'platform', or 'os'
            List bad_platforms = YamlOperator.getObjectValue(
                jervis_yaml,
                "\"${current.platform}\"",
                [:]).keySet().toList().intersect(this.known_platforms + ['jenkins', 'platform', 'os'])
            if(bad_platforms) {
                errors << "'${current.platform}' must not contain any platforms: ${bad_platforms.inspect()}"
            }
            // An OS within a platform MUST NOT contain any platform keys
            // An OS within a platform MUST NOT contain a key with a valid OS
            // An OS within a platform MUST NOT contain any key named 'jenkins', 'platform', or 'os'
            bad_platforms = YamlOperator.getObjectValue(
                jervis_yaml,
                "\"${current.platform}\".\"${current.os}\"",
                [:]).keySet().toList().intersect(this.known_platforms + this.known_operating_systems + ['jenkins', 'platform', 'os'])
            if(bad_platforms) {
                errors << "'${current.platform}'.'${current.os}' must not contain any platforms or other OSes: ${bad_platforms.inspect()}"
            }
            // An OS as a top-level key MUST NOT contain any platform keys
            // An OS MUST NOT contain a key with a valid OS
            // An OS MUST NOT contain any key named 'jenkins', 'platform', or 'os'
            bad_platforms = YamlOperator.getObjectValue(
                jervis_yaml,
                "\"${current.os}\"",
                [:]).keySet().toList().intersect(this.known_platforms + this.known_operating_systems + ['jenkins', 'platform', 'os'])
            if(bad_platforms) {
                errors << "'${current.os}' must not contain any platforms or other OSes: ${bad_platforms.inspect()}"
            }
            // Top-level keys MUST NOT contian any key named 'platform' or 'os'
            bad_platforms = jervis_yaml.keySet().toList().intersect(['platform', 'os'])
            if(bad_platforms) {
                errors << "jenkins.platform and jenkins.os is supported but not as top-level keys: ${bad_platforms.inspect()}"
            }
            // If jenkins.platform is a List, then each item MUST be a String
            Boolean nonString = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.platform', []).any { !(it in String) }
            if(nonString) {
                errors << 'jenkins.platform List contains a value that is not a String.  All platforms must be a String.'
            }
            // If jenkins.os is a List, then each item MUST be a String
            nonString = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.os', []).any { !(it in String) }
            if(nonString) {
                errors << 'jenkins.os List contains a value that is not a String.  All operating systems must be a String.'
            }
            // TODO verify neither os nor platform are named as keys.
        }
        if(errors) {
            // TODO MultiPlatformException
            throw new Exception("Multi-platform YAML validation has failed:\n\n  - " + errors.sort().unique().join('\n  - ') + "\n\nSee one or more errors above.")
        }
        // ELSE this will cause confusion for the user because they won't have an
        // understanding of YAML parsing internals when there's unexpected
        // behavior.
    }
}
