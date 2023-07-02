package net.gleske.jervis.beta

import net.gleske.jervis.tools.YamlOperator
import net.gleske.jervis.lang.PlatformValidator

class MultiPlatformGenerator implements Serializable {
    final MultiPlatformValidator platforms_obj

    private MultiPlatformGenerator() {
        throw new IllegalStateException('ERROR: This class must be instantiated with a MultiPlatformValidator.')
    }

    MultiPlatformGenerator(MultiPlatformValidator platforms) {
        platforms.validate()
        this.platforms = platforms.known_platforms
        this.operating_systems = platforms.known_operating_systems
        this.platforms_obj = platforms
    }

    Map rawJervisYaml

    List platforms = []
    List operating_systems = []
    String defaultPlatform
    String defaultOS
    // TODO: figure out how to expose platforms and operating systems object
    // platforms PlatformValidator


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
    String getJervisYamlString() {
        YamlOperator.writeObjToYaml(getJervisYaml())
    }

    /**
      Remove any keys which match a platform or operating system name from the
      top-level key of the provided Map.  This will also perform a deep-copy on
      the Map before removing any keys to ensure that a Map is not modified
      in-place.
      @param map A map which should be modified.
      @return A new Map with any keys whiched matched platform or OS removed.
      */
    private Map removePlatformOsKeys(Map map) {
        Map copy = YamlOperator.deepCopy(map)
        [this.platforms, this.operating_systems].flatten().each { String key ->
            copy.remove(key)
        }
        // return
        copy
    }

    void loadMultiPlatformYaml(Map options) {
        def parsedJervisYaml = YamlOperator.loadYamlFrom(options.yaml)
        if(!(parsedJervisYaml in Map)) {
            // TODO throw new MultiPlatformException or JervisYamlException
            throw new Exception("Jervis YAML must be a YAML object but is YAML ${parsedJervisYaml.getClass()}")
        }
        platforms_obj.validateJervisYaml(parsedJervisYaml)
        this.rawJervisYaml = parsedJervisYaml

        // initialize platforms
        List user_platform = YamlOperator.getObjectValue(rawJervisYaml, 'jenkins.platform', [[], '']).with {
            (it in List) ? it : [it]
        }.findAll {
            it.trim()
        }
        if(!user_platform) {
            user_platform << YamlOperator.getObjectValue(platforms_obj.platform_obj.platforms, 'defaults.platform', '')
        }
        this.defaultPlatform = user_platform.first()

        List user_os = YamlOperator.getObjectValue(rawJervisYaml, 'jenkins.os', [[], '']).with {
            (it in List) ? it : [it]
        }.findAll {
            it.trim()
        }
        if(!user_os) {
            user_os << YamlOperator.getObjectValue(platforms_obj.platform_obj.platforms, 'defaults.os', '')
        }

        // get a List of platform / operating system pairs
        [user_platform, user_os].combinations().collect {
            [platform: it[0], os: it[1]]
        }.each { Map current ->
            // perform a deep copy on original YAML in order to update it
            this.platform_jervis_yaml[current.platform][current.os] = YamlOperator.deepCopy(rawJervisYaml).withDefault {
                [:].withDefault { [:] }
            }
            // For each platform and OS; flatten the YAML into a simpler text
            // for LifecycleGenerator; without matrix jenkins.platform or
            // jenkins.os
            this.platform_jervis_yaml[current.platform][current.os].with { Map jervis_yaml ->
                jervis_yaml.jenkins.platform = current.platform
                jervis_yaml.jenkins.os = current.os
                // ORDER of merging platform and operating system keys
                // More specific to least specific
                // - platform.os
                // - os
                // - platform

                [
                    "\"${current.platform}\".\"${current.os}\"",
                    "\"${current.os}\"",
                    "\"${current.platform}\""
                ].each { String searchString ->
                    Map merge = YamlOperator.getObjectValue(jervis_yaml, searchString, [:])
                    jervis_yaml.putAll(merge)
                }
            }
            // remove user-overridden platforms and OS setings.
            this.platform_jervis_yaml[current.platform][current.os] = removePlatformOsKeys(this.platform_jervis_yaml[current.platform][current.os])
            this.platform_generators[current.platform][current.os] = platforms_obj.getGeneratorFromJervis(
                yaml: YamlOperator.writeObjToYaml(this.platform_jervis_yaml[current.platform][current.os]),
                folder_listing: options.folder_listing,
                private_key: options.private_key)
        }
    }
    // TODO: implement a ToolchainsValidator
}
