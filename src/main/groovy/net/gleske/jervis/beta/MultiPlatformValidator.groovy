package net.gleske.jervis.beta
import net.gleske.jervis.lang.LifecycleGenerator
import net.gleske.jervis.lang.LifecycleValidator
import net.gleske.jervis.lang.PipelineGenerator
import net.gleske.jervis.lang.PlatformValidator
import net.gleske.jervis.lang.ToolchainValidator

/**

import net.gleske.jervis.beta.MultiPlatformValidator

MultiPlatformValidator platforms = new MultiPlatformValidator()
platforms.loadPlatformsString(new File('resources/platforms.yaml').text)
platforms.getToolchainFiles().each { String fileName ->
    platforms.loadToolchainsString(fileName, new File("resources/${fileName}.yaml").text)
}
platforms.getLifecycleFiles().each { String fileName ->
    platforms.loadLifecyclesString(fileName, new File("resources/${fileName}.yaml").text)
}
platforms.getGeneratorFromJervis(yaml: 'language: shell')

  */
class MultiPlatformValidator {
    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.PlatformValidator}</tt> class which as loaded a platforms file.
     */
    PlatformValidator platform_obj
    List platforms = []
    List operating_systems = []

    MultiPlatformValidator() {
    }

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
        this.platforms = this.platform_obj.platforms.supported_platforms.keySet().toList()
        this.operating_systems = this.platform_obj.platforms.supported_platforms.collect { k, v ->
            v.keySet().toList()
        }.flatten().sort().unique()

    }

    List<String> getLifecycleFiles() {
        // TODO if not platform_obj throw MultiPlatformException
        operating_systems.collect { String os ->
            [
                "lifecycles-${os}-stable",
                "lifecycles-${os}-unstable"
            ]
        }.flatten()*.toString()
    }

    List<String> getToolchainFiles() {
        // TODO if not platform_obj throw MultiPlatformException
        operating_systems.collect { String os ->
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
        // TODO verify this key is correct (by OS)
        String key = (fileName -~ '^lifecycles-') -~ '-(un)?stable$'
        if(!this.lifecycles[key]) {
            this.lifecycles[key] = new LifecycleValidator()
        }
        if(isUnstable) {
            // TODO support partial unstable
        }
        else {
            this.lifecycles[key].loadYamlString(yaml)
        }
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
        // TODO verify this key is correct (by OS)
        String key = (fileName -~ '^toolchains-') -~ '-(un)?stable$'
        if(!this.toolchains[key]) {
            this.toolchains[key] = new ToolchainValidator()
        }
        if(isUnstable) {
            // TODO support partial unstable
        }
        else {
            this.toolchains[key].loadYamlString(yaml)
        }
        this.toolchains[key].validate()
    }

    /**
      Creates a LifecycleGenerator object out of provided options.
Example with all options
getGeneratorFromJervis(yaml: '', folder_listing: []
      */
    LifecycleGenerator getGeneratorFromJervis(Map options) {
        LifecycleGenerator generator = new LifecycleGenerator()
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
        true
        // TODO validate the platforms has all platforms, lifecycles, and toolchains available
    }

    void validateJervisYaml(Map jervis_yaml) {
        true
        // TODO: validation for raw_jervis_yaml
        //   - A platform MUST NOT contain any platform keys
        //   - An OS within a platform MUST NOT contain any platform keys
        //   - An OS as a top-level key MUST NOT contain any platform keys
        //   - An OS MUST NOT contain a key with a valid OS
        //   - If jenkins.platform is a List, then each item MUST be a String
        //   - If jenkins.os is a List, then each item MUST be a String
        // ELSE this will cause confusion for the user because they won't have an
        // understanding of YAML parsing internals when there's unexpected
        // behavior.
    }
}
