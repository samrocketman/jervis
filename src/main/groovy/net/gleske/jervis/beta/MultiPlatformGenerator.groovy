package net.gleske.jervis.beta

import net.gleske.jervis.tools.YamlOperator
import net.gleske.jervis.lang.PlatformValidator

class MultiPlatformGenerator {
    final MultiPlatformValidator platforms_obj
    private MultiPlatformGenerator() {
        throw new IllegalStateException('ERROR: This class must be instantiated with a MultiPlatformValidator.')
    }

    MultiPlatformGenerator(MultiPlatformValidator platforms) {
        this.platforms_obj = platforms
    }

    Map rawJervisYaml

    String defaultPlatform
    String defaultOS
    // TODO: figure out how to expose platforms and operating systems object
    // platforms PlatformValidator
    // operating_systems could be a Map where key is operating system


    // a map of LifecycleGenerators with top-level keys platform and child keys by operating system
    Map platform_generators = [:].withDefault {
        [:].withDefault { [:] }
    }

    // 2-level default hashmap
    Map platform_jervis_yaml = [:].withDefault {
        [:].withDefault { [:] }
    }

    Map getGenerator() {
        // TODO return generator for default platform and default OS
    }

    Map getJervisYaml() {
        // TODO return for default OS
    }


    void loadJervisYamlString(String jervisYaml) {
        def parsedJervisYaml = YamlOperator.loadYamlFrom(jervisYaml)
        if(!(parsedJervisYaml in Map)) {
            // TODO throw new MultiPlatformException or JervisYamlException
            throw new Exception("Jervis YAML must be a YAML object but is YAML ${parsedJervisYaml.getClass()}")
        }
        this.raw_jervis_yaml = parsedJervisYaml
        // TODO call validator on raw jervis yaml
        // see 'TODO: validation for raw_jervis_yaml'

        // initialize platforms
        Map platforms = YamlOperator.getObjectValue(raw_jervis_yaml, 'jenkins.platform', [[], '']).with {
            // TODO replace [''] with platforms.yaml default
           (it in String ? [it] : it) ?: ['']
        }*.trim()
        Map operating_systems = YamlOperator.getObjectValue(raw_jervis_yaml, 'jenkins.os', [[], '']).with {
            // TODO replace [''] with platforms.yaml default
           (it in String ? [it] : it) ?: ['']
        }*.trim()

        // get a List of platform / operating system pairs
        [platforms, operating_systems].combinations().collect {
          [platform: it[0], os: it[1]]
        }.each { Map current ->
            // perform a deep copy on original YAML in order to update it
            this.platform_jervis_yaml[current.platform][current.os] = YamlOperator.deepCopy(raw_jervis_yaml).withDefault {
                [:].withDefault { [:] }
            }
            // For each platform and OS; flatten the YAML into a simpler text
            // for LifecycleGenerator; without matrix jenkins.platform or
            // jenkins.os
            this.platform_jervis_yaml[current.platform][current.os].with { Map jervis_yaml ->
                jervis_yaml.jenkins.platform = current.platform
                jervis_yaml.jenkins.os = current.os
                // ORDER of merging platform and operating system keys
                // Note: use jervis_yaml.putAll to merge maps
                // More specific to least specific
                // - platform.os
                // - os
                // - platform

                // TODO: load in platforms and operating systems YAML because processing further is too hard

                /*
                Map merge = YamlOperator.deepCopy(YamlOperator.getObjectValue(jervis_yaml, "\"${current.platform}\".\"${current.os}\"", [:]))
                if(merge) {
                    jervis_yaml.putAll(merge)
                }
                jervis_yaml[current.platform].remove(current.os)
                merge = YamlOperator.deepCopy(YamlOperator.getObjectValue(jervis_yaml, "\"${current.platform}\"", [:]))
                if(merge) {
                    jervis_yaml.putAll(merge)
                }
                merge = YamlOperator.deepCopy(YamlOperator.getObjectValue(jervis_yaml, "\"${current.platform}\"", [:]))
                if(merge) {
                    jervis_yaml.putAll(merge)
                }
                */
                // TODO: for all top-level keys which match OS or platform,
                // remove them platforms.yaml must be used as the source of
                // truth.  This must be done last.
            }
            println "platform: ${current.platform}, os: ${current.os}"
        }
    }
    // TODO: implement a ToolchainsValidator

}
