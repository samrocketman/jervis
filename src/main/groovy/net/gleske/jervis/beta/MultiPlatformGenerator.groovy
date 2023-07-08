package net.gleske.jervis.beta

import net.gleske.jervis.lang.LifecycleValidator
import net.gleske.jervis.lang.PlatformValidator
import net.gleske.jervis.lang.ToolchainValidator
import net.gleske.jervis.tools.YamlOperator

class MultiPlatformGenerator implements Serializable {
    final MultiPlatformValidator platforms_obj
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

    /**
      Do not allow instantiating without arguments.
      */
    private MultiPlatformGenerator() {
        throw new IllegalStateException('ERROR: This class must be instantiated with a MultiPlatformValidator.')
    }

    MultiPlatformGenerator(MultiPlatformValidator platforms) {
        platforms.validate()
        this.platforms = platforms.known_platforms
        this.operating_systems = platforms.known_operating_systems
        this.platforms_obj = platforms
    }

    /**
      Get the <tt>{@link net.gleske.jervis.lang.LifecycleGenerator}</tt> for a
      given default platform and OS.

      @see #defaultPlatform
      @see #defaultOS
      @return A <tt>LifecycleGenerator</tt> instance.
      */
    Map getGenerator() {
        this.platform_generators[defaultPlatform][defaultOS]
    }

    /**
      Get the Jervis YAML for a given default platform and OS.  If you're
      looking for the original multi-platform Jervis YAML, then refer to
      <tt>{@link #rawJervisYaml}</tt>.

      @see #defaultPlatform
      @see #defaultOS
      @return A <tt>{@link java.util.HashMap}</tt> of the original parsed
              Jervis YAML.
      */
    Map getJervisYaml() {
        platform_jervis_yaml[defaultPlatform][defaultOS]
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
        // validate against the platform
        platforms_obj.validateJervisYaml(parsedJervisYaml)

        // initialize platforms
        List user_platform = YamlOperator.getObjectValue(parsedJervisYaml, 'jenkins.platform', [[], '']).with {
            (it in List) ? it : [it]
        }.findAll {
            it.trim()
        }
        if(!user_platform) {
            user_platform << YamlOperator.getObjectValue(this.platforms_obj.platform_obj.platforms, 'defaults.platform', '')
        }
        this.defaultPlatform = user_platform.first()

        List user_os = YamlOperator.getObjectValue(parsedJervisYaml, 'jenkins.os', [[], '']).with {
            (it in List) ? it : [it]
        }.findAll {
            it.trim()
        }
        if(!user_os) {
            user_os << YamlOperator.getObjectValue(platforms_obj.platform_obj.platforms, 'defaults.os', '')
        }

        // get a List of platform / operating system pairs
        List errors = []
        [user_platform, user_os].combinations().collect {
            [platform: it[0], os: it[1]]
        }.each { Map current ->
            // perform a deep copy on original YAML in order to update it
            this.platform_jervis_yaml[current.platform][current.os] = YamlOperator.deepCopy(parsedJervisYaml).withDefault {
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
            errors += validate(
                platform: current.platform,
                os: current.os,
                yaml: YamlOperator.writeObjToYaml(this.platform_jervis_yaml[current.platform][current.os]))
            if(errors) {
                return
            }
            this.platform_generators[current.platform][current.os] = platforms_obj.getGeneratorFromJervis(
                yaml: YamlOperator.writeObjToYaml(this.platform_jervis_yaml[current.platform][current.os]),
                folder_listing: options.folder_listing,
                private_key: options.private_key)
        }
        if(errors) {
            // reset parsed yaml
            this.platform_jervis_yaml = [:].withDefault {
                [:].withDefault { [:] }
            }
            // reset generators
            this.platform_generators = [:].withDefault {
                [:].withDefault { [:] }
            }
            // TODO multi-platform exception
            throw new Exception([
                    '',
                    'Found one or more Jervis YAML errors:',
                    '* ' + errors.sort().unique().reverse().join('\n* '),
                    ''].join('\n\n'))
        }
        this.rawJervisYaml = parsedJervisYaml
    }

    List getStashes() {
        YamlOperator.getObjectValue(generator.jervis_yaml, 'jenkins.stash', [[:], []]).with {
            (!it) ? [] : ((it in List) ? it : [it])
        }
    }

    /**
      Reviews the loaded Jervis YAML and detects invalid toolchains or
      languages.  This assumes a flattened YAML no matrix support.  This would
      typically be used for validating default YAML loaded from elsewhere or
      multi-platform YAML which has been flattened by platform and OS.
      @param options A map of options requiring: <tt>platform</tt>, <tt>os</tt>, and <tt>yaml</tt>.
      @return A <tt>List</tt> of errors.  If the list is empty, then there's no errors.
      */
    List validate(Map options) throws Exception {
        List errors = []
        String os = options.os
        String platform = options.platform
        Map jervisYaml = YamlOperator.loadYamlFrom(options.yaml)
        PlatformValidator platform_obj = this.platforms_obj.platform_obj
        if(!platform_obj) {
            return ['Admin setup error: must load platforms file first.']
        }
        String defaultStability = YamlOperator.getObjectValue(platform_obj.platforms, 'defaults.stability', 'stable')
        Boolean isUnstable = !(YamlOperator.getObjectValue(jervisYaml, 'jenkins.unstable', defaultStability) in ['stable', 'true'])
        String message = (isUnstable) ? 'Unstable; ' : 'Stable; '
        List known_platforms = platform_obj.getPlatforms(isUnstable)['supported_platforms'].keySet().toList()
        if(!(platform in known_platforms)) {
            return [message + "Unknown jenkins.platform: '${platform}'.  Remove it or choose: '${known_platforms.join('\', \'')}'"]
        }
        message += "platform '${platform}'; "
        List known_os = platform_obj.getPlatforms(isUnstable)['supported_platforms'][platform].keySet().toList()
        if(!(os in known_os)) {
            return [message + "Unknown jenkins.os: '${os}'.  Remove it or choose: '${known_os.join('\', \'')}'"]
        }
        message += "os: '${os}'; "
        LifecycleValidator lifecycle_obj = this.platforms_obj.lifecycles[os]
        ToolchainValidator toolchain_obj = this.platforms_obj.toolchains[os]
        String language = YamlOperator.getObjectValue(jervisYaml, 'language', '')
        if(![
            lifecycle_obj.supportedLanguage(language, isUnstable),
            toolchain_obj.supportedLanguage(language, isUnstable)
        ].every { it }) {
            errors << message + "Unsupported language in yaml -> language: ${language}"
        }
        else {
            if(!(language in platform_obj.getPlatforms(isUnstable)['supported_platforms'][platform][os].language)) {
                return ["Admin setup error: Unsupported language in platforms.yaml -> language: ${language}; however lifecycles and toolchains support it"]
            }
        }
        jervisYaml.each { toolchain, tool ->
            if(!toolchain_obj.supportedToolchain(toolchain, isUnstable)) {
                return
            }
            List toolValue = []
            if(tool in Map) {
                if(!(this.toolchain_obj.toolchainType(toolchain, isUnstable) == 'advanced')) {
                    errors << ( message + [
                        "toolchain '${toolchain}' does not support advanced matrices.",
                        'Its value must be a String or List.'
                        ].join('  '))
                    return
                }
                // check advanced matrix
                ['global', 'matrix'].each { String key ->
                    if(!(key in tool.keySet())) {
                        return
                    }
                    toolValue = YamlOperator.getObjectValue(tool, key, [[], '']).with {
                        (it in List) ? it : [it]
                    }.findAll {
                        it.toString().trim()
                    }
                    if(tool[key] && !toolValue) {
                        errors << (message + "Unsupported tool in yaml -> '${toolchain}': ${key}.${tool[key]}")
                    }
                    toolValue.each {
                        if(!toolchain_obj.supportedTool(toolchain, it, isUnstable)) {
                            errors << (message + "Unsupported tool in yaml -> '${toolchain}': ${key}.${it}")
                        }
                    }
                }
                return
            }
            toolValue = YamlOperator.getObjectValue(jervisYaml, toolchain, [[], '']).with {
                (it in List) ? it : [it]
            }.findAll {
                it.toString().trim()
            }
            if(tool && !toolValue) {
                errors << (message + "Unsupported tool in yaml -> '${toolchain}': ${tool}")
                return
            }
            toolValue.each {
                if(!toolchain_obj.supportedTool(toolchain, it, isUnstable)) {
                    errors << (message + "Unsupported tool in yaml -> '${toolchain}': ${it}")
                }
            }
        }
        // return
        errors
    }
}
