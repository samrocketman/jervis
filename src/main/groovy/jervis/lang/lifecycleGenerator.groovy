package jervis.lang

import jervis.exceptions.JervisException
import jervis.exceptions.UnsupportedLanguageException
import jervis.exceptions.UnsupportedToolException
import jervis.lang.lifecycleValidator
import jervis.lang.toolchainValidator
import org.yaml.snakeyaml.Yaml

/**
  WIP: Docs will be written once this class has stabilized.

  <h2>Sample usage</h2>
<pre><tt>import jervis.lang.lifecycleGenerator
def x = new lifecycleGenerator()
x.loadYamlString('language: ruby\nrvm: 2.1.0\njdk: oraclejdk8')
x.folder_listing = ['Gemfile.lock']
println x.generateAll()
</tt></pre>
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
    String[] yaml_keys

    /**
      An instance of the <tt>{@link jervis.lang.lifecycleValidator}</tt> class which has loaded a lifecycles file.
     */
    def lifecycle_obj

    /**
      An instance of the <tt>{@link jervis.lang.toolchainValidator}</tt> class which as loaded a toolchains file.
     */
    def toolchain_obj

    /**
      This is a folder listing of the root of the repository so that scripts can be
      conditionally generated depending on build tool is being used.  This way we can
      do neat things like generate different script output depending on if there's a
      <tt>build.gradle</tt>, <tt>pom.xml</tt>, or <tt>build.xml</tt>.
      <tt>{@link #loadYamlString()}</tt> should be called before this.
     */
    ArrayList folder_listing

    /**
      The value is the key to be looked up in the lifecycles file by default when
      determining how to generate scripts.  This is set when a list of files is set in
      the <tt>{@link #folder_listing}</tt>.
     */
    String lifecycle_key

    /**
      This function sets the <tt>{@link #folder_listing}</tt> and based on the
      <tt>listing</tt> conditionally sets <tt>{@link #lifecycle_key}</tt>.  This uses
      the <tt>fileExistsCondition</tt> and <tt>fallbackKey</tt> from the lifecycles
      file to determine the contents of <tt>lifecycle_key</tt>.
      <tt>{@link #loadYamlString()}</tt> should be called before this.
      @param listing An <tt>ArrayList</tt> which is a list of files from a directory
                     path in a repository.
     */
    void setFolder_listing(ArrayList listing) {
        if(!yaml_language) {
            throw new JervisException("Must call loadYamlString() first.")
        }
        folder_listing = listing
        String current_key = lifecycle_obj.lifecycles[yaml_language].defaultKey
        while(current_key != null) {
            def cycles = lifecycle_obj.lifecycles[yaml_language][current_key].keySet() as String[]
            if("fileExistsCondition" in cycles) {
                if(lifecycle_obj.lifecycles[yaml_language][current_key]["fileExistsCondition"][1] in listing) {
                    lifecycle_key = current_key
                    current_key = null
                }
                else {
                    if("fallbackKey" in cycles) {
                        current_key = lifecycle_obj.lifecycles[yaml_language][current_key]["fallbackKey"]
                    }
                    else {
                        lifecycle_key = current_key
                        current_key = null
                    }
                }
            }
            else {
                lifecycle_key = current_key
                current_key = null
            }
        }
    }

    /**
      Load a lifecycles file so that default scripts can be generated.  Lifecycles
      provide the build portions of the script.  This project comes with a lifecycles
      file.  The lifecycles file in this repository relative to the repository root is
      <tt>/src/main/resources/lifecycles.json</tt>.

      @param file A path to a lifecycles file.
     */
    public void loadLifecycles(String file) {
        this.lifecycle_obj = new lifecycleValidator()
        this.lifecycle_obj.load_JSON(file)
        this.lifecycle_obj.validate()
    }

    /**
      Load a lifecycles JSON <tt>String</tt> so that default scripts can be generated.
      Lifecycles provide the build portions of the script.  This project comes with a
      lifecycles file.  The lifecycles file in this repository relative to the
      repository root is <tt>/src/main/resources/lifecycles.json</tt>.

      @param json A <tt>String</tt> containing JSON which is from a lifecycles file.
     */
    public void loadLifecyclesString(String json) {
        this.lifecycle_obj = new lifecycleValidator()
        this.lifecycle_obj.load_JSONString(json)
        this.lifecycle_obj.validate()
    }

    /**
      Load a toolchains file so that default scripts can be generated.  Toolchains
      provide the default tool setup of the script (e.g. what version of Java will be
      used).  This project comes with a toolchains file.  The toolchains file in this
      repository relative to the repository root is
      <tt>/src/main/resources/toolchains.json</tt>.

      @param file A path to a toolchains file.
     */
    public void loadToolchains(String file) {
        this.toolchain_obj = new toolchainValidator()
        this.toolchain_obj.load_JSON(file)
        this.toolchain_obj.validate()
    }

    /**
      Load a toolchains JSON <tt>String</tt> so that default scripts can be generated.
      Toolchains provide the default tool setup of the script (e.g. what version of
      Java will be used).  This project comes with a toolchains file.  The toolchains
      file in this repository relative to the repository root is
      <tt>/src/main/resources/toolchains.json</tt>.

      @param json A <tt>String</tt> containing JSON which is from a toolchains file.
     */
    public void loadToolchainsString(String json) {
        this.toolchain_obj = new toolchainValidator()
        this.toolchain_obj.load_JSONString(json)
        this.toolchain_obj.validate()
    }

    /**
      Load Jervis YAML to be interpreted.  This YAML will be used to generate the build scripts and components of a Jenkins job.
     */
    public void loadYamlString(String raw_yaml) {
        def yaml = new Yaml()
        this.jervis_yaml = yaml.load(raw_yaml)
        this.yaml_language = this.jervis_yaml['language']
        this.yaml_keys = this.jervis_yaml.keySet() as String[]
        if(!lifecycle_obj.supportedLanguage(this.yaml_language) || !toolchain_obj.supportedLanguage(this.yaml_language)) {
            throw new UnsupportedLanguageException(this.yaml_language)
        }
        //avoid throwing a NullPointer exception if the user forgets to call obj.folder_listing to load a list of files.
        //just load an empty file list by default initially that can then be overridden.
        this.setFolder_listing([])
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

    /**
      This function generates a Jenkins groovy expression from Jervis YAML which will
      be used to exclude matrix build combinations.  This is typically done from a
      a <a href="https://github.com/samrocketman/jervis/wiki/Matrix-job-support#matrix-exclusion" target="_blank">matrix exclusion</a>
      in the form of a whitelist or blacklist.

      @return A <tt>String</tt> which is a simple groovy expression.
     */
    public String excludeFilter() {
    }

    /**
      Interpolate <tt>${jervis_toolchain_ivalue}</tt> on an ArrayList of strings.
      This is mostly used by the <tt>{@link #generateToolchainSection()}</tt> function.
      @param  cmds   A list of strings which contain bash commands.
      @param  ivalue A value which will be string interpolated on the <tt>cmds</tt>
      @return        A list of strings which contain bash commands that have had string interpolation done.
     */
    private ArrayList interpolate_ivalue(ArrayList cmds, String ivalue) {
        def z = []
        cmds.each{ z << it.replace('${jervis_toolchain_ivalue}',ivalue) }
        z
    }

    /**
      Generate the toolchains shell script based on the Jervis YAML or taking defaults
      from the toolchains file.
      @return A bash script setting up the toolchains for building.
     */
    public String generateToolchainSection() {
        //get toolchain order for this language
        def toolchains_order = toolchain_obj.toolchains["toolchains"][yaml_language]
        String output = "#\n# TOOLCHAINS SECTION\n#\n"
        toolchains_order.each {
            def toolchain = it
            def toolchain_keys = toolchain_obj.toolchains[toolchain].keySet() as String[]
            output += "#${toolchain} toolchain section\n"
            if(toolchain in yaml_keys) {
                //do non-default stuff
                ArrayList user_toolchain
                if(!(jervis_yaml[toolchain] instanceof String) && !(jervis_yaml[toolchain] instanceof ArrayList)) {
                        throw new UnsupportedToolException("${toolchain}: ${jervis_yaml[toolchain]}")
                }
                if(jervis_yaml[toolchain] instanceof String) {
                    user_toolchain = [jervis_yaml[toolchain]]
                }
                else {
                    user_toolchain = jervis_yaml[toolchain]
                }
                //check if a matrix build
                if(user_toolchain.size() > 1) {
                }
                else {
                    //not a matrix build
                    if(!toolchain_obj.supportedTool(toolchain, user_toolchain[0])) {
                        throw new UnsupportedToolException("${toolchain}: ${user_toolchain[0]}")
                    }
                    if(user_toolchain[0] in toolchain_keys) {
                        output += toolchain_obj.toolchains[toolchain][user_toolchain[0]].join('\n') + '\n'
                    }
                    else {
                        //assume using "*" key
                        output += this.interpolate_ivalue(toolchain_obj.toolchains[toolchain]['*'], user_toolchain[0]).join('\n') + '\n'
                    }
                }
            }
            else {
                //falling back to default behavior in toolchains.json
                String default_ivalue = toolchain_obj.toolchains[toolchain].default_ivalue
                if(default_ivalue) {
                    if(default_ivalue in toolchain_keys) {
                        output += toolchain_obj.toolchains[toolchain][default_ivalue].join('\n') + '\n'
                    }
                    else {
                        //assume using "*" key
                        output += this.interpolate_ivalue(toolchain_obj.toolchains[toolchain]['*'], default_ivalue).join('\n') + '\n'
                    }
                }
            }
        }
        return output
    }

    /**
       A generic function to generate code for different sections of the build script.
       @param header  A header message for the section being generated.
       @param section A section from the build lifecycle.  e.g. before_install, install, before_script, script, etc.
       @return        Code generated from that section in the Jervis YAML, default from the lifecycles file, or returns an empty String.
     */
    private String generateSection(String section) {
        String output = "#\n# ${section.toUpperCase()} SECTION\n#\n"
        def my_lifecycle = lifecycle_obj.lifecycles[yaml_language][lifecycle_key]
        String[] my_lifecycle_keys = my_lifecycle.keySet() as String[]
        if(!(section in yaml_keys)) {
            //take the default
            if(section in my_lifecycle_keys) {
                if(my_lifecycle[section] instanceof ArrayList) {
                    output += my_lifecycle[section].join('\n') + '\n'
                }
                else {
                    output += my_lifecycle[section] + '\n'
                }
            }
            else {
                output = ""
            }
        }
        else if(jervis_yaml[section] instanceof ArrayList) {
            output += jervis_yaml[section].join('\n') + '\n'
        }
        else {
            //must be a String instance
            output += jervis_yaml[section] + '\n'
        }
        return output
    }

    /**
      Generate the <tt>before_install</tt> shell script based on the Jervis YAML or
      taking defaults from the lifecycles file.
      @return A portion of a bash script preparing for installing dependencies.
     */
    public String generateBeforeInstall() {
        return this.generateSection('before_install')
    }

    /**
      Generate the <tt>install</tt> shell script based on the Jervis YAML or taking
      defaults from the lifecycles file.
      @return A portion of a bash script which will install dependencies.
     */
    public String generateInstall() {
        return this.generateSection('install')
    }

    /**
      Generate the <tt>before_script</tt> shell script based on the Jervis YAML or
      taking defaults from the lifecycles file.
      @return A portion of a bash script preparing the system for running unit tests.
     */
    public String generateBeforeScript() {
        return this.generateSection('before_script')
    }

    /**
      Generate the <tt>script</tt> shell script based on the Jervis YAML or taking
      defaults from the lifecycles file.
      @return A portion of a bash script running unit tests.
     */
    public String generateScript() {
        return this.generateSection('script')
    }

    /**
      Generate the <tt>after_success</tt> shell script based on the Jervis YAML or
      taking defaults from the lifecycles file.
      @return A shell script which is executed after a successful build.
     */
    public String generateAfterSuccess() {
        return this.generateSection('after_success')
    }

    /**
      Generate the <tt>after_failure</tt> shell script based on the Jervis YAML or
      taking defaults from the lifecycles file.
      @return A shell script which is executed after a failed build.
     */
    public String generateAfterFailure() {
        return this.generateSection('after_failure')
    }

    /**
      Generate the <tt>after_script</tt> shell script based on the Jervis YAML or
      taking defaults from the lifecycles file.
      @return A shell script which is executed after every build.
     */
    public String generateAfterScript() {
        return this.generateSection('after_script')
    }
    /**
      Generate the build script which would be used in the Jenkins step.  This
      function combines the output of: <tt>generateToolchainSection()</tt>,
      <tt>generateBeforeInstall()</tt>, <tt>generateInstall()</tt>,
      <tt>generateBeforeScript() </tt>, and <tt>generateScript()</tt>.
      @return A shell script which is used to build the application in Jervis.
     */
    public String generateAll() {
        ArrayList script = [
            generateToolchainSection(),
            generateBeforeInstall(),
            generateInstall(),
            generateBeforeScript(),
            generateScript()
            ]
        return script.grep().join('\n')
    }
}
