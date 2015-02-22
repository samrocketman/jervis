package jervis.lang

import jervis.exceptions.JervisException
import jervis.exceptions.UnsupportedLanguageException
import jervis.exceptions.UnsupportedToolException
import jervis.lang.lifecycleValidator
import jervis.lang.toolchainValidator
import org.yaml.snakeyaml.Yaml

/**
  Generates the build scripts from the Jervis YAML.

  <h2>Sample usage</h2>
<pre><tt>import jervis.lang.lifecycleGenerator
import jervis.tools.scmGit
def git = new scmGit()
def generator = new lifecycleGenerator()
generator.loadLifecycles(git.getRoot() + '/src/main/resources/lifecycles.json')
generator.loadToolchains(git.getRoot() + '/src/main/resources/toolchains.json')
generator.loadYamlString("""
language: ruby
env:
 - hello=world three=four
 - hello=test three=five
rvm: ["1.9.3", "2.1.0", "2.0.0"]
jdk: oraclejdk8
matrix:
  exclude:
    - env: hello=world three=four
      rvm: 2.1.0
    - rvm: 1.9.3
  include:
    - rvm: 2.1.0
    - rvm: 2.0.0
      env: hello=test three=five
""")
generator.folder_listing = ['Gemfile.lock']
println 'Exclude filter is...'
println generator.matrixExcludeFilter()
println 'Matrix axis value for env is...'
println generator.matrixGetAxisValue('env')
println 'Generating the matrix build script.'
println generator.generateAll()
</tt></pre>
 */
class lifecycleGenerator {

    /**
      Contains the Jervis YAML loaded as an object.
     */
    Map jervis_yaml

    /**
      A quick access variable for what language is selected for the loaded Jervis YAML.
     */
    String yaml_language

    /**
      A quick access variable for what root keys are in the loaded Jervis YAML.
     */
    String[] yaml_keys

    /**
      A quick access variable for matrix build axes.
     */
    ArrayList yaml_matrix_axes

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
      <tt>{@link #loadYamlString(java.lang.String)}</tt> should be called before this.
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
      <tt>{@link #loadYamlString(java.lang.String)}</tt> should be called before this.
      @param listing An <tt>ArrayList</tt> which is a list of files from a directory
                     path in a repository.
     */
    void setFolder_listing(ArrayList listing) throws JervisException {
        if(!yaml_language) {
            throw new JervisException('Must call loadYamlString() first.')
        }
        folder_listing = listing
        String current_key = lifecycle_obj.lifecycles[yaml_language].defaultKey
        while(current_key != null) {
            def cycles = lifecycle_obj.lifecycles[yaml_language][current_key].keySet() as String[]
            if('fileExistsCondition' in cycles) {
                if(lifecycle_obj.lifecycles[yaml_language][current_key]['fileExistsCondition'] in listing) {
                    lifecycle_key = current_key
                    current_key = null
                }
                else {
                    if('fallbackKey' in cycles) {
                        current_key = lifecycle_obj.lifecycles[yaml_language][current_key]['fallbackKey']
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
      Load Jervis YAML to be interpreted.  This YAML will be used to generate the
      build scripts and components of a Jenkins job.  Please note: you must call
      <tt>{@link #loadToolchains(java.lang.String)}</tt> and
      <tt>{@link #loadLifecycles(java.lang.String)}</tt> before calling this function.
      @param raw_yaml A <tt>String</tt> which contains Jervis YAML to be parsed.
     */
    public void loadYamlString(String raw_yaml) throws JervisException, UnsupportedLanguageException {
        def yaml = new Yaml()
        jervis_yaml = yaml.load(raw_yaml)
        yaml_keys = jervis_yaml.keySet() as String[]
        if(jervis_yaml['language']) {
            yaml_language = jervis_yaml['language']
        }
        if(!lifecycle_obj) {
            throw new JervisException('ERROR: Must call lifecycleGenerator.loadLifecycles() or lifecycleGenerator.loadLifecyclesString() first.')
        }
        if(!toolchain_obj) {
            throw new JervisException('ERROR: Must call lifecycleGenerator.loadToolchains() or lifecycleGenerator.loadToolchainsString() first.')
        }
        if(!lifecycle_obj.supportedLanguage(this.yaml_language) || !toolchain_obj.supportedLanguage(this.yaml_language)) {
            throw new UnsupportedLanguageException(this.yaml_language)
        }
        //avoid throwing a NullPointer exception if the user forgets to call obj.folder_listing to load a list of files.
        //just load an empty file list by default initially that can then be overridden.
        this.setFolder_listing([])
        //configure the matrix axes if it is a matrix build i.e. set yaml_matrix_axes
        if(this.isMatrixBuild()) {
            yaml_matrix_axes = []
            toolchain_obj.toolchains["toolchains"][yaml_language].each {
                if((jervis_yaml[it] instanceof ArrayList) && (jervis_yaml[it].size() > 1)) {
                    yaml_matrix_axes << it
                }
                else if((it == 'env') && (jervis_yaml[it] instanceof Map) && ('matrix' in jervis_yaml[it]) && (jervis_yaml[it]['matrix'].size() > 1)) {
                    yaml_matrix_axes << it
                }
            }
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
      <p>However, the following YAML will produce a matrix build.</p>
      <pre><tt>language: groovy
env:
  - foobar=foo
  - foobar=bar</tt></pre>

      @return <tt>true</tt> if a matrix build will be generated or <tt>false</tt> if it will just be a regular build.
     */
    public Boolean isMatrixBuild() {
        Boolean result=false
        yaml_keys.each{
            if(toolchain_obj.supportedMatrix(yaml_language, it)) {
                if(jervis_yaml[it] instanceof ArrayList && jervis_yaml[it].size() > 1) {
                     result=true
                }
                else if(('env' == it) && (jervis_yaml[it] instanceof Map) && ('matrix' in jervis_yaml[it]) && (jervis_yaml[it]['matrix'] instanceof ArrayList) && (jervis_yaml[it]['matrix'].size() > 1)) {
                     result=true
                }
            }
        }
        return result
    }

    /*
       This is a builder for groovy expressions to be used by the <tt>{@link #matrixExcludeFilter()}</tt>.
       @param filterType    The type of filter in the matrix.  Possible values: <tt>include</tt>, <tt>exclude</tt>.
       @param exprSeparator The separator between multiple expressions.  This is typically <tt>&amp;&amp;</tt> or <tt>||</tt>.
       @param inverse       Each expression can be inversed.  For example, <tt>!(a == b)</tt> is inverse of <tt>(a == b)</tt>.
       @param group         When all of the expressions are finished being build should they be grouped together?  If so then this should be <tt>true</tt>.
     */
    private String matrixExcludeFilterBuilder(String filterType, String exprSeparator, Boolean inverse, Boolean group) {
        Map matrix = jervis_yaml['matrix']
        String result = ''
        if(filterType in matrix) {
            Boolean first_in_group = true
            for(int i=0; i < matrix[filterType].size(); i++) {
                String temp = '('
                Boolean first_in_expr = true
                if(inverse) {
                    temp = "!${temp}"
                }
                matrix[filterType][i].each { k, v ->
                    if(k in yaml_matrix_axes) {
                        if(first_in_expr) {
                            if(('env' == k) && (jervis_yaml[k] instanceof Map)) {
                                temp += "${k} == '${k}${jervis_yaml[k]['matrix'].indexOf(v)}'"
                            }
                            else {
                                temp += "${k} == '${k}${jervis_yaml[k].indexOf(v)}'"
                            }
                            first_in_expr = false
                        }
                        else {
                            if(('env' == k) && (jervis_yaml[k] instanceof Map)) {
                                temp += " && ${k} == '${k}${jervis_yaml[k]['matrix'].indexOf(v)}'"
                            }
                            else {
                                temp += " && ${k} == '${k}${jervis_yaml[k].indexOf(v)}'"
                            }
                        }
                    }
                    else {
                        //discard because something was nil
                        temp = '-1'
                    }
                }
                temp += ')'
                if(temp.indexOf('-1') < 0) {
                    if(first_in_group) {
                        result += temp
                        first_in_group = false
                    }
                    else {
                        result += " ${exprSeparator} ${temp}"
                    }
                }
            }
            if(group) {
                if(result.indexOf(exprSeparator) >= 0) {
                    result = "(${result})"
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
    public String matrixExcludeFilter() {
        String result = ''
        String exclude = this.matrixExcludeFilterBuilder('exclude', '&&', true, false)
        String include = this.matrixExcludeFilterBuilder('include', '||', false, true)
        if(exclude.length() > 0 && include.length() > 0) {
            result = "${exclude} && ${include}"
        }
        else {
            result = exclude + include
        }
        return result
    }

    /**
      Get a value for a given axis for matrix building.  Each matrix job has multiple
      axes.  This function is designed to return the value of the axis if given a
      name.

      @param axis A matrix building axis.  e.g. <tt>env</tt>
      @return A <tt>String</tt> which is the value of the given axis in a matrix build.
     */
    public String matrixGetAxisValue(String axis) {
        String result = ''
        int counter = 0
        if(axis in yaml_matrix_axes) {
            if(('env' == axis) && (jervis_yaml[axis] instanceof Map)) {
                jervis_yaml[axis]['matrix'].each {
                    result += " ${axis}${counter}"
                    counter++
                }
            }
            else {
                jervis_yaml[axis].each {
                    result += " ${axis}${counter}"
                    counter++
                }
            }
            return result.trim()
        }
        else {
            return ''
        }
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

    /*
       This is an abstracted function to generate matrix and non-matrix toolchains.
       @param toolchain      A toolchain that comes from the matrix build toolchain order for a given language.
       @param toolchain_keys The known keys for a given toolchain to look up <tt>*</tt> or a given toolchain value.
       @param chain          The matrix list from the Jervis YAML for the given toolchain.
       @param matrix         Should the input be considered a matrix build?  If so then set to <tt>true</tt>.
     */
    private String toolchainBuilder(String toolchain, String[] toolchain_keys, ArrayList chain, Boolean matrix) throws UnsupportedToolException {
        String output = ''
        if(matrix) {
            output += "case \${${toolchain}} in\n"
            for(int i=0; i < chain.size(); i++) {
                String tempchain = chain[i].toString()
                if(!toolchain_obj.supportedTool(toolchain, tempchain)) {
                    throw new UnsupportedToolException("${toolchain}: ${tempchain}")
                }
                output += "  ${toolchain}${i})\n"
                if(tempchain in toolchain_keys) {
                    output += '    ' + toolchain_obj.toolchains[toolchain][tempchain].join('\n    ') + '\n    ;;\n'
                }
                else {
                    //assume using "*" key
                    output += '    ' + this.interpolate_ivalue(toolchain_obj.toolchains[toolchain]['*'], tempchain).join('\n    ') + '\n    ;;\n'
                }
            }
            output += 'esac\n'
        }
        else {
            if(!toolchain_obj.supportedTool(toolchain, chain[0])) {
                throw new UnsupportedToolException("${toolchain}: ${chain[0]}")
            }
            if(chain[0] in toolchain_keys) {
                output += toolchain_obj.toolchains[toolchain][chain[0]].join('\n') + '\n'
            }
            else {
                //assume using "*" key
                output += this.interpolate_ivalue(toolchain_obj.toolchains[toolchain]['*'], chain[0].toString()).join('\n') + '\n'
            }
        }
        return output
    }

    /**
      Generate the toolchains shell script based on the Jervis YAML or taking defaults
      from the toolchains file.
      @return A bash script setting up the toolchains for building.
     */
    public String generateToolchainSection() throws UnsupportedToolException {
        //get toolchain order for this language
        def toolchains_order = toolchain_obj.toolchains['toolchains'][yaml_language]
        String output = '#\n# TOOLCHAINS SECTION\n#\n'
        toolchains_order.each {
            def toolchain = it
            String[] toolchain_keys = toolchain_obj.toolchains[toolchain].keySet() as String[]
            output += "#${toolchain} toolchain section\n"
            if(toolchain in yaml_keys) {
                //do non-default stuff
                def user_toolchain
                //toolchain must be an instance of String, ArrayList, or (in the case of only env) Map.
                if(!(jervis_yaml[toolchain] instanceof String) && !(jervis_yaml[toolchain] instanceof ArrayList) && !(('env' == toolchain) && (jervis_yaml['env'] instanceof Map))) {
                    throw new UnsupportedToolException("${toolchain}: ${jervis_yaml[toolchain]}")
                }
                if(jervis_yaml[toolchain] instanceof String) {
                    user_toolchain = [jervis_yaml[toolchain]]
                }
                else {
                    user_toolchain = jervis_yaml[toolchain]
                }
                //check if a matrix build
                if(toolchain in yaml_matrix_axes) {
                    if(('env' == toolchain) && (user_toolchain instanceof Map)) {
                        //special env behavior for global and matrix values
                        def env = user_toolchain
                        if('global' in env) {
                            if(env['global'] instanceof String) {
                                output += this.toolchainBuilder(toolchain, toolchain_keys, [env['global']], false)
                            }
                            else if(env['global'] instanceof ArrayList) {
                                env['global'].each {
                                    output += this.toolchainBuilder(toolchain, toolchain_keys, [it], false)
                                }
                            }
                            else {
                                throw new UnsupportedToolException("${toolchain}: global.${env['global']}")
                            }
                        }
                        if('matrix' in env) {
                            if(env['matrix'] instanceof ArrayList) {
                                output += this.toolchainBuilder(toolchain, toolchain_keys, env['matrix'], true)
                            }
                            else {
                                throw new UnsupportedToolException("${toolchain}: matrix.${env['matrix']}")
                            }
                        }
                    }
                    else {
                        //normal toolchain behavior
                        output += this.toolchainBuilder(toolchain, toolchain_keys, user_toolchain, true)
                    }
                }
                else {
                    //not a matrix build
                    if(('env' == toolchain) && (user_toolchain instanceof Map)) {
                        if('global' in user_toolchain) {
                            if(user_toolchain['global'] instanceof String) {
                                output += this.toolchainBuilder(toolchain, toolchain_keys, [user_toolchain['global']], false)
                            }
                            else if(user_toolchain['global'] instanceof ArrayList) {
                                user_toolchain['global'].each {
                                    output += this.toolchainBuilder(toolchain, toolchain_keys, [it], false)
                                }
                            }
                            else {
                                    throw new UnsupportedToolException("${toolchain}: global.${user_toolchain['global']}")
                            }
                        }
                        if('matrix' in user_toolchain) {
                            if(user_toolchain['matrix'] instanceof String) {
                                output += this.toolchainBuilder(toolchain, toolchain_keys, [user_toolchain['matrix']], false)
                            }
                            else {
                                output += this.toolchainBuilder(toolchain, toolchain_keys, user_toolchain['matrix'], false)
                            }
                        }
                    }
                    else {
                        output += this.toolchainBuilder(toolchain, toolchain_keys, user_toolchain, false)
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
                output = ''
            }
        }
        else if(jervis_yaml[section] instanceof ArrayList) {
            output += jervis_yaml[section].join('\n') + '\n'
        }
        else {
            //must be a String instance
            output += jervis_yaml[section].toString() + '\n'
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

    /**
      Is this a branch which will generate a job?
      @param branch A branch to check if the job should be generated.
      @return       Returns <tt>true</tt> if the job should be generated or <tt>false</tt> if it should not.
     */
    public Boolean isGenerateBranch(String branch) {
        Boolean result=true
        if(('branches' in jervis_yaml) && (jervis_yaml['branches'] instanceof Map)) {
            if('only' in jervis_yaml['branches']) {
                //set a new default result
                result=false
                jervis_yaml['branches']['only'].each {
                    if(it == branch) {
                        result = true
                    }
                }
            }
            else if('except' in jervis_yaml['branches']) {
                //result is true by default
                jervis_yaml['branches']['except'].each {
                    if(it == branch) {
                        result = false
                    }
                }
            }
        }
        return result
    }
}
