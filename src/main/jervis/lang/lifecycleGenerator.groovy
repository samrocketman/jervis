package jervis.lang

import jervis.exceptions.UnsupportedLanguageException
import jervis.lang.lifecycleValidator
import jervis.lang.toolchainValidator
import jervis.tools.scmGit
import org.yaml.snakeyaml.Yaml

/**
  WIP: Docs will be written once this class has stabilized.
 */
class lifecycleGenerator {

    /**
      Contains the Jervis YAML loaded as an object.
     */
    def jervis_yaml

    /**
      A quick access variable for what language is selected for the loaded Jervis YAML.
     */
    def yaml_language

    /**
      A quick access variable for what root keys are in the loaded Jervis YAML.
     */
    def yaml_keys

    /**
      An instance of the <tt>{@link jervis.lang.lifecycleValidator}</tt> class which has loaded a lifecycles file.
     */
    def lifecycle_obj

    /**
      An instance of the <tt>{@link jervis.lang.toolchainValidator}</tt> class which as loaded a toolchains file.
     */
    def toolchain_obj

    def lifecycleGenerator() {
        def git = new scmGit()
        this.loadLifecycles("${git.getRoot()}/src/resources/lifecycles.json")
        this.loadToolchains("${git.getRoot()}/src/resources/toolchains.json")
    }
    /**
      Load a lifecycles file so that default scripts can be generated.  Lifecycles
      provide the build portions of the script.  This project comes with a lifecycles
      file which gets loaded by default when you instantiate this class.  The path to
      the lifecycles in this repository relative to the repository root is
      <tt>/src/resources/lifecycles.json</tt>.  This can be overridden by calling
      this method.

      @param file A path to a lifecycles file.
     */
    public void loadLifecycles(String file) {
        this.lifecycle_obj = new lifecycleValidator()
        this.lifecycle_obj.load_JSON(file)
        this.lifecycle_obj.validate()
    }
    /**
      Load a toolchains file so that default scripts can be generated.  Toolchains
      provide the default tool setup of the script (e.g. what version of Java will be
      used).  This project comes with a toolchains file which gets loaded by default
      when you instantiate this class.  The path to the toolchains in this repository
      relative to the repository root os <tt>/src/resources/toolchains.json</tt>.
      This can be overridden by calling this method.

      @param file A path to a toolchains file.
     */
    public void loadToolchains(String file) {
        this.toolchain_obj = new toolchainValidator()
        this.toolchain_obj.load_JSON(file)
        this.toolchain_obj.validate()
    }
    /**
      Load Jervis YAML to be interpreted.  This YAML will be used to generate the build scripts and components of a Jenkins job.
     */
    public void loadYaml(String raw_yaml) {
        if(!lifecycle_obj) {
        }
        def yaml = new Yaml()
        this.jervis_yaml = yaml.load(raw_yaml)
        this.yaml_language = this.jervis_yaml['language']
        if(!lifecycle_obj.supportedLanguage(this.yaml_language)) {
            throw new UnsupportedLanguageException(this.yaml_language)
        }
    }
    /**
      This will check if the loaded YAML is a matrix build.  The requirements for it
      to be a matrix build is that it must be a matrix specifically for the selected
      language and the array for the section must be greater than 1.

      <p>For example the following YAML would not produce a matrix build.</p>
      <pre><tt>language: groovy
env: foo=bar</tt></pre>
      <pre><tt>language: groovy
env:
  - foo=bar</tt></pre>
      <p>However, the following YAML will produce a matrix build.
      <pre><tt>language: groovy
env:
  - foobar=foo
  - foobar=bar</tt></pre>

      @return <tt>true</tt> if a matrix build will be generated or <tt>false</tt> if it will just be a regular build.
     */
    public Boolean isMatrixBuild() {
        def keys = jervis_yaml.keySet() as String[]
        Boolean result=false
        keys.each{
            if(toolchain_obj.supportedMatrix(yaml_language, it)) {
                if(jervis_yaml[it] instanceof ArrayList && jervis_yaml[it].size() > 1) {
                     result=true
                }
            }
        }
        return result
    }
    public String excludeFilter() {
    }
    public String generateToolchainSection() {
        //get toolchain order for this language
        //def toolchains_order = toolchain_obj.toolchains["toolchains"][yaml_language]
        //toolchains_order.each {
        //    def toolchain = it
        //}
    }
    public String generateBeforeInstall() {
    }
    public String generateInstall() {
    }
    public String generateBeforeScript() {
    }
    public String generateScript() {
    }
    public String generateAfterSuccess() {
    }
    public String generateAfterFailure() {
    }
    public String generateAfterScript() {
    }
    public String generateAll() {
    }
}
