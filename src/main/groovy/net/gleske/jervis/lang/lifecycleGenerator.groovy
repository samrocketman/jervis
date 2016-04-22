/*
   Copyright 2014-2016 Sam Gleske - https://github.com/samrocketman/jervis

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

import java.util.regex.Pattern
import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.exceptions.PlatformValidationException
import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.exceptions.UnsupportedLanguageException
import net.gleske.jervis.exceptions.UnsupportedToolException
import net.gleske.jervis.lang.lifecycleValidator
import net.gleske.jervis.lang.platformValidator
import net.gleske.jervis.lang.toolchainValidator
import net.gleske.jervis.tools.securityIO
import org.yaml.snakeyaml.Yaml

/**
  Generates the build scripts from the Jervis YAML.

  <h2>Sample usage</h2>
<pre><tt>import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.tools.scmGit
def git = new scmGit()
def generator = new lifecycleGenerator()
generator.loadPlatforms(git.getRoot() + '/src/main/resources/platforms.json')
generator.loadLifecycles(git.getRoot() + '/src/main/resources/lifecycles.json')
generator.loadToolchains(git.getRoot() + '/src/main/resources/toolchains.json')
String yaml = """
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
jenkins:
    sudo: false
    unstable: true
"""
generator.preloadYamlString(yaml)
generator.loadYamlString(yaml)
generator.folder_listing = ['Gemfile.lock']
println 'Exclude filter is...'
println generator.matrixExcludeFilter()
println 'Matrix axis value for env is...'
println generator.matrixGetAxisValue('env')
println 'Generating the matrix build script.'
println generator.generateAll()
print 'Labels: '
println generator.getLabels()</tt></pre>
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
    List yaml_matrix_axes

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.lifecycleValidator}</tt> class which has loaded a lifecycles file.
     */
    def lifecycle_obj

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.toolchainValidator}</tt> class which as loaded a toolchains file.
     */
    def toolchain_obj

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.platformValidator}</tt> class which as loaded a platforms file.
     */
    def platform_obj

    /**
      This is a folder listing of the root of the repository so that scripts can be
      conditionally generated depending on build tool is being used.  This way we can
      do neat things like generate different script output depending on if there's a
      <tt>build.gradle</tt>, <tt>pom.xml</tt>, or <tt>build.xml</tt>.
      <tt>{@link #loadYamlString(java.lang.String)}</tt> should be called before this.
     */
    List folder_listing

    /**
      An optional label which is used by the <tt>{@link #getLabels()}</tt> function
      to generate advanced job labels across different platforms.  This is populated
      by <tt>{@link #preloadYamlString(java.lang.String)}</tt>.
     */
    String label_platform

    /**
      An optional label which is used by the <tt>{@link #getLabels()}</tt> function
      to generate advanced job labels across different operating systems.  This is
      populated by <tt>{@link #preloadYamlString(java.lang.String)}</tt>.
     */
    String label_os

    /**
      An optional label which is used by the <tt>{@link #getLabels()}</tt> function
      to generate advanced job labels for agent stability.  This is populated by
      <tt>{@link #preloadYamlString(java.lang.String)}</tt>.

      The value is always <tt>stable</tt> or <tt>unstable</tt>.
     */
    String label_stability

    /**
      An optional label which is used by the <tt>{@link #getLabels()}</tt> function
      to generate advanced job labels for agents with sudo or nosudo access.  This is
      populated by <tt>{@link #preloadYamlString(java.lang.String)}</tt>.

      The value is always <tt>sudo</tt> or <tt>nosudo</tt>.
     */
    String label_sudo

    /**
      The value is the key to be looked up in the lifecycles file by default when
      determining how to generate scripts.  This is set when a list of files is set in
      the <tt>{@link #folder_listing}</tt>.
     */
    String lifecycle_key

    /**
      A list of secrets loaded from the YAML file.
     */
    List cipherlist = [] as ArrayList

    /**
      A list of decrypted values from the <tt>{@link #cipherlist}</tt>.
     */
    List plainlist = [] as ArrayList

    /**
      A utility for decrypting RSA encrypted strings in YAML files.  This is an
      instance of the <tt>{@link net.gleske.jervis.tools.securityIO}</tt>.
     */
    def secret_util

    /**
      This function sets the <tt>{@link #folder_listing}</tt> and based on the
      <tt>listing</tt> conditionally sets <tt>{@link #lifecycle_key}</tt>.  This uses
      the <tt>fileExistsCondition</tt> and <tt>fallbackKey</tt> from the lifecycles
      file to determine the contents of <tt>lifecycle_key</tt>.
      <tt>{@link #loadYamlString(java.lang.String)}</tt> should be called before this.

      @param listing An <tt>List</tt> which is a list of files from a directory
                     path in a repository.
     */
    void setFolder_listing(List listing) throws JervisException {
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
      This function sets the <tt>{@link #label_stability}</tt> property.  It forces
      the value to always be <tt>stable</tt> or <tt>unstable</tt>.

      @param stability A <tt>String</tt> which determines the stability.  A value of
                       <tt>unstable</tt> or <tt>true</tt> will set the property as
                       <tt>unstable</tt>.  All other values set the property as
                       <tt>stable</tt>.
     */
    public void setLabel_stability(String stability) {
        if(stability == 'unstable' || stability == 'true') {
            label_stability = 'unstable'
        }
        else {
            label_stability = 'stable'
        }
    }

    /**
      This function sets the <tt>{@link #label_sudo}</tt> property.  It forces the
      value to always be <tt>sudo</tt> or <tt>nosudo</tt>.

      @param sudo A <tt>String</tt> which determines sudo access required.  A value of
                  <tt>sudo</tt> or <tt>true</tt> will set the property as
                  <tt>sudo</tt>.  All other values set the property as
                  <tt>nosudo</tt>.
     */
    public void setLabel_sudo(String sudo) {
        if(sudo == 'sudo' || sudo == 'true' || sudo == 'required') {
            label_sudo = 'sudo'
        }
        else {
            label_sudo = 'nosudo'
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
        //remove any empty YAML keys to fix null key bug
        def iterator = jervis_yaml.entrySet().iterator()
        while(iterator.hasNext()) {
            if(iterator.next().value == null) {
                iterator.remove()
            }
        }
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
        //load encrypted properties
        cipherlist = getObjectValue(jervis_yaml, 'jenkins.secrets', [] as ArrayList)
        //avoid throwing a NullPointer exception if the user forgets to call obj.folder_listing to load a list of files.
        //just load an empty file list by default initially that can then be overridden.
        this.setFolder_listing([])
        //configure the matrix axes if it is a matrix build i.e. set yaml_matrix_axes
        if(this.isMatrixBuild()) {
            yaml_matrix_axes = []
            toolchain_obj.toolchains["toolchains"][yaml_language].each {
                if((jervis_yaml[it] instanceof List) && (jervis_yaml[it].size() > 1)) {
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
                if(jervis_yaml[it] instanceof List && jervis_yaml[it].size() > 1) {
                     result=true
                }
                else if(('env' == it) && (jervis_yaml[it] instanceof Map) && ('matrix' in jervis_yaml[it]) && (jervis_yaml[it]['matrix'] instanceof List) && (jervis_yaml[it]['matrix'].size() > 1)) {
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
                            first_in_expr = false
                        }
                        else {
                            temp += " && "
                        }
                        if(('env' == k) && (jervis_yaml[k] instanceof Map)) {
                            temp += "${k} == '${k}${jervis_yaml[k]['matrix'].indexOf(v)}'"
                        }
                        else {
                            if(toolchain_obj.isFriendlyLabel(k)) {
                                temp += "${k} == '${k}:${v}'"
                            }
                            else {
                                temp += "${k} == '${k}${jervis_yaml[k].indexOf(v)}'"
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
      <a href="https://github.com/samrocketman/jervis/wiki/Matrix-job-support#matrix-exclusion" target="_blank">matrix exclusion</a>
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
                boolean friendly = toolchain_obj.isFriendlyLabel(axis)
                jervis_yaml[axis].each {
                    if(friendly) {
                        result += " ${axis}:${it}"
                    }
                    else {
                        result += " ${axis}${counter}"
                        counter++
                    }
                }
            }
            return result.trim()
        }
        else {
            return ''
        }
    }

    /**
      Interpolate <tt>${jervis_toolchain_ivalue}</tt> on an List of strings.
      This is mostly used by the <tt>{@link #generateToolchainSection()}</tt> function.
      @param  cmds   A list of strings which contain bash commands.
      @param  ivalue A value which will be string interpolated on the <tt>cmds</tt>
      @return        A list of strings which contain bash commands that have had string interpolation done.
     */
    private List interpolate_ivalue(List cmds, String ivalue) {
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
    private String toolchainBuilder(String toolchain, String[] toolchain_keys, List chain, Boolean matrix) throws UnsupportedToolException {
        String output = ''
        if(matrix) {
            output += "case \${${toolchain}} in\n"
            for(int i=0; i < chain.size(); i++) {
                String tempchain = chain[i].toString()
                if(!toolchain_obj.supportedTool(toolchain, tempchain)) {
                    throw new UnsupportedToolException("${toolchain}: ${tempchain}")
                }
                if(toolchain_obj.isFriendlyLabel(toolchain)) {
                    output += "  ${toolchain}:${tempchain})\n"
                }
                else {
                    output += "  ${toolchain}${i})\n"
                }
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
        String output = '#\n# TOOLCHAINS SECTION\n#\nset +x\necho \'# TOOLCHAINS SECTION\'\nset -x\n'
        toolchains_order.each {
            def toolchain = it
            String[] toolchain_keys = toolchain_obj.toolchains[toolchain].keySet() as String[]
            output += "#${toolchain} toolchain section\n"
            if(toolchain in yaml_keys) {
                //do non-default stuff
                def user_toolchain
                //toolchain must be an instance of String, List, or (in the case of only env) Map.
                if(!(jervis_yaml[toolchain] instanceof String) && !(jervis_yaml[toolchain] instanceof List) && !(('env' == toolchain) && (jervis_yaml['env'] instanceof Map))) {
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
                            else if(env['global'] instanceof List) {
                                env['global'].each {
                                    output += this.toolchainBuilder(toolchain, toolchain_keys, [it], false)
                                }
                            }
                            else {
                                throw new UnsupportedToolException("${toolchain}: global.${env['global']}")
                            }
                        }
                        if('matrix' in env) {
                            if(env['matrix'] instanceof List) {
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
                            else if(user_toolchain['global'] instanceof List) {
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
       Typical sections include: before_install, install, before_script, script
       @param section A section from the build lifecycle.  e.g. before_install, install, before_script, script, etc.
       @return        Code generated from that section in the Jervis YAML, default from the lifecycles file, or returns an empty String.
     */
    public String generateSection(String section) {
        String output = "#\n# ${section.toUpperCase()} SECTION\n#\nset +x\necho '# ${section.toUpperCase()} SECTION'\nset -x\n"
        def my_lifecycle = lifecycle_obj.lifecycles[yaml_language][lifecycle_key]
        String[] my_lifecycle_keys = my_lifecycle.keySet() as String[]
        if(!(section in yaml_keys)) {
            //take the default
            if(section in my_lifecycle_keys) {
                if(my_lifecycle[section] instanceof List) {
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
        else if(jervis_yaml[section] instanceof List) {
            output += jervis_yaml[section].join('\n') + '\n'
        }
        else {
            //must be a String instance
            output += jervis_yaml[section].toString() + '\n'
        }
        return output
    }

    /**
      Generate the build script which would be used in the Jenkins step.  This
      function combines the output of: <tt>generateToolchainSection()</tt>,
      <tt>generateBeforeInstall()</tt>, <tt>generateInstall()</tt>,
      <tt>generateBeforeScript() </tt>, and <tt>generateScript()</tt>.
      @return A shell script which is used to build the application in Jervis.
     */
    public String generateAll() {
        List script = [
            generateToolchainSection(),
            generateSection('before_install'),
            generateSection('install'),
            generateSection('before_script'),
            generateSection('script')
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
        if(('branches' in jervis_yaml)) {
            if(jervis_yaml['branches'] instanceof List) {
                List tmp = jervis_yaml['branches']
                jervis_yaml['branches'] = ['only': tmp]
            }
            if(jervis_yaml['branches'] instanceof Map) {
                if('only' in jervis_yaml['branches']) {
                    //set a new default result
                    result=false
                    jervis_yaml['branches']['only'].each {
                        if(result) {
                            //skip to the end because a result has been found
                            return
                        }
                        if(it[0] == '/' && it[-1] == '/') {
                            //regular expression detected
                            Pattern pattern = Pattern.compile(it[1..-2])
                            if(pattern.matcher(branch).matches()) {
                                result = true
                            }
                        }
                        else if(it == branch) {
                            result = true
                        }
                    }
                }
                else if('except' in jervis_yaml['branches']) {
                    //result is true by default
                    jervis_yaml['branches']['except'].each {
                        if(!result) {
                            //skip to the end because a result has been found
                            return
                        }
                        if(it[0] == '/' && it[-1] == '/') {
                            //regular expression detected
                            Pattern pattern = Pattern.compile(it[1..-2])
                            if(pattern.matcher(branch).matches()) {
                                result = false
                            }
                        }
                        else if(it == branch) {
                            result = false
                        }
                    }
                }
            }
        }
        return result
    }

    /**
      Get an object from a <tt>Map</tt> or return any object from
      <tt>defaultValue</tt>.  Guarantees that what is returned is the same type as
      <tt>defaultValue</tt>.  This is used to get optional keys from YAML or JSON
      files.

      @param object A <tt>Map</tt> which was likely created from a YAML or JSON file.
      @param key A <tt>String</tt> with keys and subkeys separated by periods which is
                 used to search the <tt>object</tt> for a possible value.
      @param defaultValue A default value and type that should be returned.
      @return Returns the value of the key or a <tt>defaultValue</tt> which is of the
              same type as <tt>defaultValue</tt>.
     */
    public Object getObjectValue(Map object, String key, Object defaultValue) {
        if(key.indexOf('.') >= 0) {
            String key1 = key.split('\\.', 2)[0]
            String key2 = key.split('\\.', 2)[1]
            if(object.get(key1) != null && object.get(key1) instanceof Map) {
                return getObjectValue(object.get(key1), key2, defaultValue)
            }
            else {
                return defaultValue
            }
        }

        //try returning the value casted as the same type as defaultValue
        try {
            if(object.get(key) != null) {
                return object.get(key).asType(defaultValue.getClass())
            }
        }
        catch(Exception e) {}

        //nothing worked so just return default value
        return defaultValue
    }

    /**
      Load a platforms file so that advanced labels can be generated for multiple
      platforms.  A platform could be a local datacenter or a cloud providor.
      The platforms file allows labels to be generated which include stability, sudo
      access, and even operating system.  This could be used to load lifecycles and
      toolchains by platform and OS.  This project comes with a platforms file.  The
      platforms file in this repository relative to the repository root is
      <tt>/src/main/resources/platforms.json</tt>.

      @param file A path to a platforms file.
     */
    public void loadPlatforms(String file) {
        this.platform_obj = new platformValidator()
        this.platform_obj.load_JSON(file)
        this.platform_obj.validate()
    }

    /**
      Load a platforms JSON <tt>String</tt> so that advanced labels can be generated
      for multiple platforms.  A platform could be a local datacenter or a cloud
      providor.  The platforms file allows labels to be generated which include
      stability, sudo access, and even operating system.  This could be used to load
      lifecycles and toolchains by platform and OS.  This project comes with a
      platforms file.  The platforms file in this repository relative to the
      repository root is <tt>/src/main/resources/platforms.json</tt>.

      @param json A <tt>String</tt> containing JSON which is from a platforms file.
     */
    public void loadPlatformsString(String json) {
        this.platform_obj = new platformValidator()
        this.platform_obj.load_JSONString(json)
        this.platform_obj.validate()
    }

    /**
      Preload Jervis YAML for the purpose of loading lifecycles files for other
      platforms and operating systems.  Please note: you must call
      <tt>{@link #loadPlatformsString(java.lang.String)}</tt> or
      <tt>{@link #loadPlatforms(java.lang.String)}</tt> before calling this function.

      @param raw_yaml A <tt>String</tt> which contains Jervis YAML to be parsed.
     */
    public void preloadYamlString(String raw_yaml) throws JervisException {
        if(!platform_obj) {
            throw new PlatformValidationException('Must load the platforms file first.')
        }
        def yaml = new Yaml()
        jervis_yaml = yaml.load(raw_yaml)
        this.label_platform = getObjectValue(jervis_yaml, 'jenkins.platform', platform_obj.platforms['defaults']['platform'])
        this.label_os = getObjectValue(jervis_yaml, 'jenkins.os', platform_obj.platforms['defaults']['os'])
        setLabel_stability(getObjectValue(jervis_yaml, 'jenkins.unstable', platform_obj.platforms['defaults']['stability']))
        setLabel_sudo(getObjectValue(jervis_yaml, 'jenkins.sudo', platform_obj.platforms['defaults']['sudo']))
    }

    /**
      Returns a groovy expression which Jenkins would use to pin a job to specific
      nodes.

      @return A <tt>String</tt> which is a groovy expression of Jenkins node labels.
     */
    public String getLabels() {
        String labels = ["language:${yaml_language}", toolchain_obj.toolchains['toolchains'][yaml_language].join('&&')].join('&&')
        if(platform_obj) {
            labels = [this.label_stability, this.label_platform, this.label_os, this.label_sudo, labels].join('&&')
        }
        return labels
    }

    /**
      Given a project combination of <tt>org/project</tt> determine if a project is
      allowed to be generated for a platform combination.  The platform combination is
      determined by <tt>{@link #label_platform}</tt> and <tt>{@link #label_os}</tt>.
      A Jenkins job should not be generated if it is restricted via the restrictions
      section of the platforms file.

      @return Returns <tt>true</tt> if not allowed or <tt>false</tt> if it is allowed.
              That is, it is not restricted.
     */
    public boolean isRestricted(String project) {
        Map restrictions = platform_obj.platforms['restrictions']
        String org = project.split('/')[0]
        boolean restricted = false
        if(restrictions.containsKey(label_platform)) {
            restricted = true
            if(restrictions[label_platform].containsKey('only_organizations') && (org in restrictions[label_platform]['only_organizations'])) {
                restricted = false
            }
            if(restrictions[label_platform].containsKey('only_projects') && (project in restrictions[label_platform]['only_projects'])) {
                restricted = false
            }
        }
        return restricted
    }

    /**
       Checks if a job that is using a platform and toolchain which is supported in the
       loaded platforms file.

       @return Returns <tt>true</tt> if a job can be generated from the requested
               platform.
     */
    public boolean isSupportedPlatform() {
        if(platform_obj) {
            String [] supported_platforms = platform_obj.platforms['supported_platforms'].keySet() as String[]
            if(label_platform in supported_platforms) {
                String[] supported_os = platform_obj.platforms['supported_platforms'][label_platform].keySet() as String[]
                if(label_os in supported_os) {
                    //check the support of toolchains
                    LinkedHashSet toolchains_set = [] as Set
                    //start with the requested toolchains
                    toolchains_set = toolchains_set.plus(toolchain_obj.toolchains['toolchains'][yaml_language])
                    //subtract from the list of supported tool chains
                    toolchains_set = toolchains_set.minus(platform_obj.platforms['supported_platforms'][label_platform][label_os]['toolchain'])
                    //toolchains_set should be empty if the platform supports all toolchains
                    List supported_languages = platform_obj.platforms['supported_platforms'][label_platform][label_os]['language']
                    if(!(yaml_language in supported_languages) || toolchains_set.size() > 0) {
                        return false
                    }
                }
                else {
                    return false
                }
            }
            else {
                return false
            }
        }
        return true
    }

    /**
      Decrypts encrypted values stored in <tt>{@link #cipherlist}</tt>.  All decrypted
      values will be stored in <tt>{@link #plainlist}</tt> which is where they should
      be accessed.  Any malformatted Hash Maps within <tt>cipherlist</tt> which don't
      have a <tt>secret</tt> and <tt>key</tt> item key in the <tt>HashMap</tt> will
      simply be ignored.  Decryption errors will throw an exception.
     */
    public void decryptSecrets() throws SecurityException {
        if(!secret_util) {
            throw new SecurityException("Call setPrivateKeyPath before loading secrets.")
        }
        cipherlist.each { item ->
            if(('key' in item.keySet()) && ('secret' in item.keySet())) {
                item['secret'] = secret_util.rsaDecrypt(item['secret'])
                //now that we've decrypted go ahead and append it to plainlist
                plainlist << item
            }
            //else skip unwanted keys
        }
    }

    /**
      Set the RSA private key path so secrets in a YAML file can be decrypted.  Call
      this function before <tt>{@link #decryptSecrets()}</tt> if
      expecting encrypted YAML keys.
     */
    public void setPrivateKeyPath(String path) {
        //public key path and key size do not matter because we're only decrypting and not generating nor encrypting.
        secret_util = new securityIO(path, '', 2048)
    }
}
