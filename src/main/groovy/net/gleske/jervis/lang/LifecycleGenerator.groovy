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

import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.exceptions.PlatformValidationException
import net.gleske.jervis.exceptions.SecurityException
import net.gleske.jervis.exceptions.UnsupportedLanguageException
import net.gleske.jervis.exceptions.UnsupportedToolException
import net.gleske.jervis.tools.SecurityIO
import net.gleske.jervis.tools.YamlOperator

import java.util.regex.Pattern

/**
  Generates the build scripts from the Jervis YAML.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
  <p><b>Please note</b>, if you are writing Job DSL plugin groovy scripts you
  should not use the relative file paths to access files in the repository
  where your DSL scripts reside.  Instead, use the
  <a href="https://github.com/samrocketman/jervis/issues/43" target="_blank"><tt>readFileFromWorkspace</tt></a>
  method provided by the Job DSL plugin in Jenkins.</p>
<pre><code class="language-groovy">
import net.gleske.jervis.lang.LifecycleGenerator

String yaml = """
language: ruby
env:
 - hello=world three=four
 - hello=test three=five
rvm: ["1.9.3", "2.1.0", "2.0.0"]
jdk: openjdk8
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
"""

def generator = new LifecycleGenerator()
generator.loadPlatformsFile('resources/platforms.yaml')
generator.preloadYamlString(yaml)
//os_stability requires preloadYamlString() to be called
def os_stability = "${generator.label_os}-${generator.label_stability}"
generator.loadLifecycles("resources/lifecycles-${os_stability}.yaml")
generator.loadToolchains("resources/toolchains-${os_stability}.yaml")
generator.loadYamlString(yaml)
generator.folder_listing = ['Gemfile.lock']
println 'Exclude filter is...'
println generator.matrixExcludeFilter()
println 'Matrix axis value for env is...'
println generator.matrixGetAxisValue('env')
println 'Generating the matrix build script.'
println generator.generateAll()
print 'Labels: '
println generator.getLabels()
</code></pre>
 */
class LifecycleGenerator implements Serializable {

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
      A variable set by any external system relaying to Jervis that this is a
      peer review build (commonly called a pull request).
     */
    Boolean is_pr = false

    /**
      A variable set by any external system relaying to Jervis that this is a
      build associated with a tag.
     */
    Boolean is_tag = false

    /**
      Defines the kind of branch filtering which is in effect.  Possible
      values: empty string, <tt>only</tt>, <tt>except</tt>.  <tt>only</tt> is
      used for whitelist filtering and <tt>except</tt> is used for blacklist
      filtering.
      */
    String filter_type = ''

    /**
      Set if this generator is a part of a multi-platform configuration.  This
      would support treating platforms and operating systems as matrix-capable
      fields.
      */
    Boolean multiPlatform = false

    /**
      A quick access variable for matrix build axes.
     */
    List yaml_matrix_axes

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.LifecycleValidator}</tt> class which has loaded a lifecycles file.
     */
    LifecycleValidator lifecycle_obj

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.ToolchainValidator}</tt> class which has loaded a toolchains file.
     */
    ToolchainValidator toolchain_obj

    /**
      An instance of the <tt>{@link net.gleske.jervis.lang.PlatformValidator}</tt> class which as loaded a platforms file.
     */
    PlatformValidator platform_obj

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
      This is a shortcut for <tt>label_stability</tt> being <tt>unstable</tt>.
      If <tt>isUnstable</tt> is <tt>true</tt>, then the requested toolchains or
      lifecycles will be considered unstable.  Default is <tt>false</tt>.
      @see #label_stability
     */
    Boolean isUnstable = false

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
      A map of secrets loaded from the YAML file.
     */
    Map ciphermap = [:] as HashMap

    /**
      A list of decrypted values from the <tt>{@link #cipherlist}</tt>.
     */
    List plainlist = [] as ArrayList

    /**
      A list of decrypted values from the <tt>{@link #ciphermap}</tt>.
     */
    Map plainmap = [:] as HashMap

    /**
      A utility for decrypting RSA encrypted strings in YAML files.  This is an
      instance of the <tt>{@link net.gleske.jervis.tools.SecurityIO}</tt>.
     */
    SecurityIO secret_util

    /**
      A map of friendly key names for matrix steps which can be reference to
      show a full key name as if the friendly name were not needed.  Basically,
      it allows the unfriendly name to be accessible via the friendly name.
     */
    Map matrix_fullName_by_friendly = [:]

    Map getMatrix_fullName_by_friendly() {
        if(!matrix_fullName_by_friendly) {
            generateToolchainSection()
        }
        matrix_fullName_by_friendly
    }

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
        String current_key = this.lifecycle_obj.getLifecycles(this.isUnstable)[yaml_language].defaultKey
        while(current_key != null) {
            def cycles = this.lifecycle_obj.getLifecycles(this.isUnstable)[yaml_language][current_key].keySet() as String[]
            if('fileExistsCondition' in cycles) {
                if(this.lifecycle_obj.getLifecycles(this.isUnstable)[yaml_language][current_key]['fileExistsCondition'] in listing) {
                    lifecycle_key = current_key
                    current_key = null
                }
                else {
                    if('fallbackKey' in cycles) {
                        current_key = this.lifecycle_obj.getLifecycles(this.isUnstable)[yaml_language][current_key]['fallbackKey']
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
            this.label_stability = 'unstable'
        }
        else {
            this.label_stability = 'stable'
        }
        this.isUnstable = (this.label_stability == 'unstable')
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
      <tt>/src/main/resources/lifecycles.yaml</tt>.

      @param file A path to a lifecycles file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadLifecycles(String file, Boolean unstable = false) {
        if(!this.lifecycle_obj) {
            this.lifecycle_obj = new LifecycleValidator()
        }
        this.lifecycle_obj.loadYamlFile(file, unstable)
        this.lifecycle_obj.validate()
    }

    /**
      Load a lifecycles YAML <tt>String</tt> so that default scripts can be generated.
      Lifecycles provide the build portions of the script.  This project comes with a
      lifecycles file.  The lifecycles file in this repository relative to the
      repository root is <tt>/src/main/resources/lifecycles.yaml</tt>.

      @param yaml A <tt>String</tt> containing YAML which is from a lifecycles file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadLifecyclesString(String yaml, Boolean unstable = false) {
        if(!this.lifecycle_obj) {
            this.lifecycle_obj = new LifecycleValidator()
        }
        this.lifecycle_obj.loadYamlString(yaml, unstable)
        this.lifecycle_obj.validate()
    }

    /**
      Load a toolchains file so that default scripts can be generated.  Toolchains
      provide the default tool setup of the script (e.g. what version of Java will be
      used).  This project comes with a toolchains file.  The toolchains file in this
      repository relative to the repository root is
      <tt>/src/main/resources/toolchains.yaml</tt>.

      @param file A path to a toolchains file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadToolchains(String file, Boolean unstable = false) {
        if(!this.toolchain_obj) {
            this.toolchain_obj = new ToolchainValidator()
        }
        this.toolchain_obj.loadYamlFile(file, unstable)
        this.toolchain_obj.validate()
    }

    /**
      Load a toolchains YAML <tt>String</tt> so that default scripts can be generated.
      Toolchains provide the default tool setup of the script (e.g. what version of
      Java will be used).  This project comes with a toolchains file.  The toolchains
      file in this repository relative to the repository root is
      <tt>/src/main/resources/toolchains.yaml</tt>.

      @param yaml A <tt>String</tt> containing YAML which is from a toolchains file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadToolchainsString(String yaml, Boolean unstable = false) {
        if(!this.toolchain_obj) {
            this.toolchain_obj = new ToolchainValidator()
        }
        this.toolchain_obj.loadYamlString(yaml, unstable)
        this.toolchain_obj.validate()
    }

    /**
      Load Jervis YAML to be interpreted.  This YAML will be used to generate the
      build scripts and components of a Jenkins job.  <b>Please note</b>: you must call
      <tt>{@link #loadToolchains(java.lang.String)}</tt> and
      <tt>{@link #loadLifecycles(java.lang.String)}</tt> before calling this function.
      @param raw_yaml A <tt>String</tt> which contains Jervis YAML to be parsed.
     */
    public void loadYamlString(String raw_yaml) throws JervisException, UnsupportedLanguageException {
        jervis_yaml = YamlOperator.loadYamlFrom(raw_yaml) ?: [:]
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
        if(!this.lifecycle_obj) {
            throw new JervisException('ERROR: Must call LifecycleGenerator.loadLifecycles() or LifecycleGenerator.loadLifecyclesString() first.')
        }
        if(!this.toolchain_obj) {
            throw new JervisException('ERROR: Must call LifecycleGenerator.loadToolchains() or LifecycleGenerator.loadToolchainsString() first.')
        }
        if(!this.lifecycle_obj.supportedLanguage(this.yaml_language, this.isUnstable) || !this.toolchain_obj.supportedLanguage(this.yaml_language, this.isUnstable)) {
            throw new UnsupportedLanguageException(this.yaml_language)
        }
        def cipherobj = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.secrets', new Object())
        if(cipherobj instanceof List) {
            //load encrypted properties
            cipherobj.each { c ->
                if(c instanceof Map && 'key' in c && 'secret' in c) {
                    ciphermap[c['key']] = c['secret']
                }
            }
            cipherlist = ciphermap.collect { k, v ->
                [key: k, secret: v]
            }
        }
        else if (cipherobj instanceof Map) {
            ciphermap = cipherobj
            cipherlist = ciphermap.collect { k, v -> [key: k, secret: v] }
        }
        //avoid throwing a NullPointer exception if the user forgets to call obj.folder_listing to load a list of files.
        //just load an empty file list by default initially that can then be overridden.
        this.setFolder_listing([])

        //allow ordered loading additional toolchains into a language key
        List additional_toolchains = []
        this.toolchain_obj.getToolchains(this.isUnstable)["toolchains"][yaml_language].with { List toolchainList ->
            List yaml_additional_toolchains = YamlOperator.getObjectValue(jervis_yaml, 'additional_toolchains', [])
            if(YamlOperator.getObjectValue(jervis_yaml, 'additional_toolchains', '')) {
                yaml_additional_toolchains = [YamlOperator.getObjectValue(jervis_yaml, 'additional_toolchains', '')]
            }
            additional_toolchains += (yaml_additional_toolchains - toolchainList).findAll {
                it in this.toolchain_obj.getMatrix_toolchain_list(this.isUnstable)
            }
        }
        this.toolchain_obj.getToolchains(this.isUnstable)["toolchains"][yaml_language] += additional_toolchains

        // go through any toolchains that may be left; order is not guaranteed
        // but will likely remain the order in which they're in the YAML file.
        yaml_keys.each { key ->
            if((key in this.toolchain_obj.getToolchains(this.isUnstable)) && !(key in this.toolchain_obj.getToolchains(this.isUnstable)["toolchains"][yaml_language])) {
                this.toolchain_obj.getToolchains(this.isUnstable)["toolchains"][yaml_language] << key
            }
        }

        // determine which toolchains need to be built as a matrix build
        yaml_matrix_axes = this.toolchain_obj.getToolchains(this.isUnstable)["toolchains"][yaml_language].findAll { String toolchain ->
            this.toolchain_obj.supportedMatrix(yaml_language, toolchain, this.isUnstable) &&
            (
                (YamlOperator.getObjectValue(jervis_yaml, toolchain, []).size() > 1) ||
                (
                    this.toolchain_obj.toolchainType(toolchain, this.isUnstable) == 'advanced' &&
                    (YamlOperator.getObjectValue(jervis_yaml, "${toolchain}.matrix", []).size() > 1)
                )
            )
        } ?: []
        //populate unfriendly names being accessible by friendly name
        matrix_fullName_by_friendly = [:]
        //populate branch filtering keys removing extra information
        filter_type = ''
        if(jervis_yaml['branches'] instanceof List) {
            jervis_yaml['branches'] = ['only': jervis_yaml['branches']]
            filter_type = 'only'
        }
        if(jervis_yaml['branches'] instanceof Map) {
            if(('only' in jervis_yaml['branches']) || ('except' in jervis_yaml['branches'])) {
                if('only' in jervis_yaml['branches']) {
                    jervis_yaml['branches'] = ['only': jervis_yaml['branches']['only']]
                    filter_type = 'only'
                }
                else {
                    jervis_yaml['branches'] = ['except': jervis_yaml['branches']['except']]
                    filter_type = 'except'
                }
            }
        }
        if(filter_type) {
            if(!(jervis_yaml['branches'][filter_type] in List)) {
                //invalid filter so disable filtering (allow by default)
                filter_type = ''
            }
        }
        null
    }

    /**
      This will check if the loaded YAML is a matrix build.  The requirements for it
      to be a matrix build is that it must be a matrix specifically for the selected
      language and the array for the section must be greater than 1.

      <p>For example the following YAML would not produce a matrix build.</p>
<pre><code class="language-yaml">
language: groovy
env: foo=bar
</code></pre>
<pre><code class="language-yaml">
language: groovy
env:
  - foo=bar
</code></pre>
      <p>However, the following YAML will produce a matrix build.  This assumes that <tt>matrix: disabled</tt> is not set for <tt>env</tt> in the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file</a>.</p>
<pre><code class="language-yaml">
language: groovy
env:
  - foobar=foo
  - foobar=bar
</code></pre>

      @return <tt>true</tt> if a matrix build will be generated or <tt>false</tt> if it will just be a regular build.
     */
    public Boolean isMatrixBuild() {
        yaml_matrix_axes
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
        List matrix_platforms = (multiPlatform) ? ['platform', 'os'] : []
        if(filterType in matrix) {
            Boolean first_in_group = true
            for(int i=0; i < matrix[filterType].size(); i++) {
                String temp = '('
                Boolean first_in_expr = true
                if(inverse) {
                    temp = "!${temp}"
                }
                matrix[filterType][i].each { k, v ->
                    if(!(k in (yaml_matrix_axes + matrix_platforms))) {
                        //discard because something was nil
                        temp = '-1'
                        return
                    }
                    if(first_in_expr) {
                        first_in_expr = false
                    }
                    else {
                        temp += " && "
                    }

                    if('platform' == k) {
                        temp += "'${v}' == '${this.label_platform}'"
                    }
                    else if('os' == k) {
                        temp += "'${v}' == '${this.label_os}'"
                    }
                    else if(('env' == k) && (jervis_yaml[k] instanceof Map)) {
                        temp += "${k} == '${k}${jervis_yaml[k]['matrix'].indexOf(v)}'"
                    }
                    else {
                        if(this.toolchain_obj.isFriendlyLabel(k, this.isUnstable)) {
                            temp += "${k} == '${k}:${v}'"
                        }
                        else {
                            temp += "${k} == '${k}${jervis_yaml[k].indexOf(v)}'"
                        }
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
                boolean friendly = this.toolchain_obj.isFriendlyLabel(axis, this.isUnstable)
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
        cmds.collect {
            it.replace('${jervis_toolchain_ivalue}', ivalue)
        }
    }

    /**
      toolchainScript will always be a List or String.
      @param script A List or String from a toolchains YAML file.
      @return A List of Strings where the script is one line per item.
      */
    private List toolchainScript(def script) {
        (script in List) ? script : script.tokenize('\n')
    }

    /**
       This is an abstracted function to generate matrix and non-matrix toolchains.
       @param toolchain      A toolchain that comes from the matrix build toolchain order for a given language.
       @param toolchain_keys The known keys for a given toolchain to look up <tt>*</tt> or a given toolchain value.
       @param chain          The matrix list from the Jervis YAML for the given toolchain.
       @param matrix         Should the input be considered a matrix build?  If so then set to <tt>true</tt>.
     */
    private String toolchainBuilder(String toolchain, String[] toolchain_keys, List chain, Boolean matrix) throws UnsupportedToolException {
        String output = ''
        List toolchainScriptList
        if(matrix) {
            output += "case \${${toolchain}} in\n"
            for(int i=0; i < chain.size(); i++) {
                String tempchain = chain[i].toString()
                if(!this.toolchain_obj.supportedTool(toolchain, tempchain, this.isUnstable)) {
                    throw new UnsupportedToolException("${toolchain}: ${tempchain}")
                }
                if(this.toolchain_obj.isFriendlyLabel(toolchain, this.isUnstable)) {
                    output += "  ${toolchain}:${tempchain})\n"
                }
                else {
                    //allows the unfriendly name to be accessibly via friendly name
                    matrix_fullName_by_friendly["${toolchain}${i}".toString()] = "${toolchain}:${tempchain}".toString()
                    output += "  ${toolchain}${i})\n"
                }
                if(tempchain in toolchain_keys) {
                    toolchainScriptList = toolchainScript(YamlOperator.getObjectValue(this.toolchain_obj.getToolchains(this.isUnstable), "\"${toolchain}\".\"${tempchain}\"", [[], '']))
                    output += '    ' + toolchainScriptList.join('\n    ') + '\n    ;;\n'
                }
                else {
                    //assume using "*" key
                    toolchainScriptList = toolchainScript(YamlOperator.getObjectValue(this.toolchain_obj.getToolchains(this.isUnstable), "\"${toolchain}\".\\*", [[], '']))
                    output += '    ' + this.interpolate_ivalue(toolchainScriptList, tempchain).join('\n    ') + '\n    ;;\n'
                }
            }
            output += 'esac\n'
        }
        else {
            chain.each {
                if(!this.toolchain_obj.supportedTool(toolchain, it, this.isUnstable)) {
                    throw new UnsupportedToolException("${toolchain}: ${it}")
                }
                if(it in toolchain_keys) {
                    toolchainScriptList = toolchainScript(YamlOperator.getObjectValue(this.toolchain_obj.getToolchains(this.isUnstable), "\"${toolchain}\".\"${it}\"", [[], '']))
                    output += toolchainScriptList.join('\n') + '\n'
                }
                else {
                    //assume using "*" key
                    toolchainScriptList = toolchainScript(YamlOperator.getObjectValue(this.toolchain_obj.getToolchains(this.isUnstable), "\"${toolchain}\".\\*", [[], '']))
                    output += this.interpolate_ivalue(toolchainScriptList, it).join('\n') + '\n'
                }
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
        def toolchains_order = this.toolchain_obj.getToolchains(this.isUnstable)['toolchains'][yaml_language]
        HashMap cleanup = [:]
        String output = '#\n# TOOLCHAINS SECTION\n#\nset +x\necho \'# TOOLCHAINS SECTION\'\nset -x\n'
        List toolchainScriptList
        toolchains_order.each { toolchain ->
            String[] toolchain_keys = this.toolchain_obj.getToolchains(this.isUnstable)[toolchain].keySet() as String[]
            if('cleanup' in this.toolchain_obj.getToolchains(this.isUnstable)[toolchain]) {
                cleanup[toolchain] = this.toolchain_obj.getToolchains(this.isUnstable)[toolchain]['cleanup']
            }
            output += "#${toolchain} toolchain section\n"
            if(toolchain in yaml_keys) {
                //User wants to override default with a toolchain value in their YAML file.
                def user_toolchain
                //convert doubles and integers to strings fixing bug #85
                if(jervis_yaml[toolchain] instanceof Number) {
                    jervis_yaml[toolchain] = jervis_yaml[toolchain].toString()
                }
                //toolchain must be an instance of String, List, or (in the case of only advanced toolchains) Map.
                if(!isInstanceFromList(jervis_yaml[toolchain], [String, List]) &&
                        !(
                            (this.toolchain_obj.toolchainType(toolchain, this.isUnstable) == 'advanced') &&
                            (jervis_yaml[toolchain] instanceof Map)
                        )) {
                    throw new UnsupportedToolException("${toolchain}: ${jervis_yaml[toolchain]}")
                }
                if(jervis_yaml[toolchain] instanceof String) {
                    user_toolchain = [jervis_yaml[toolchain]]
                }
                else {
                    user_toolchain = jervis_yaml[toolchain]
                }
                //check if a matrix toolchain
                boolean matrix_toolchain = toolchain in yaml_matrix_axes
                if(user_toolchain instanceof Map) {
                    //because it is an instance of a Map we assume it is an advanced toolchain
                    //special advanced behavior for global and matrix values
                    ['global', 'matrix'].each { key ->
                        if(!(key in user_toolchain)) {
                            return
                        }
                        //convert doubles and integers to strings fixing bug #85
                        if(user_toolchain[key] instanceof Number) {
                            user_toolchain[key] = user_toolchain[key].toString()
                        }
                        if(user_toolchain[key] instanceof String) {
                            user_toolchain[key] = [user_toolchain[key]]
                        }

                        if(user_toolchain[key] instanceof List) {
                            output += toolchainBuilder(toolchain,
                                                       toolchain_keys,
                                                       user_toolchain[key]*.toString(),
                                                       (key == 'global')? false : matrix_toolchain)
                        }
                        else {
                            throw new UnsupportedToolException("${toolchain}: ${key}.${user_toolchain[key]}")
                        }
                    }
                }
                else {
                    //normal simple toolchain behavior
                    output += this.toolchainBuilder(toolchain, toolchain_keys, user_toolchain*.toString(), matrix_toolchain)
                }
            }
            else {
                //falling back to default behavior in toolchains.yaml because user has not defined it in their YAML.
                String default_ivalue = this.toolchain_obj.getToolchains(this.isUnstable)[toolchain].default_ivalue
                if(default_ivalue) {
                    if(default_ivalue in toolchain_keys) {
                        toolchainScriptList = toolchainScript(
                                YamlOperator.getObjectValue(
                                    this.toolchain_obj.getToolchains(this.isUnstable),
                                    "\"${toolchain}\".\"${default_ivalue}\"",
                                    [[], '']))
                        output += toolchainScriptList.join('\n') + '\n'
                    }
                    else {
                        //assume using "*" key
                        toolchainScriptList = toolchainScript(YamlOperator.getObjectValue(this.toolchain_obj.getToolchains(this.isUnstable), "\"${toolchain}\".\\*", [[], '']))
                        output += this.interpolate_ivalue(toolchainScriptList, default_ivalue).join('\n') + '\n'
                    }
                }
            }
        }
        //write out the cleanup steps at the end of the toolchains
        (cleanup.keySet() as String[]).each { toolchain ->
            output += "#cleanup toolchain section\nfunction ${toolchain}_cleanup_on() {\n  set +x"
            output += '\n  ' + cleanup[toolchain].join('\n  ')
            output += "\n}\ntrap ${toolchain}_cleanup_on EXIT\n"
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
        def my_lifecycle = this.lifecycle_obj.getLifecycles(this.isUnstable)[yaml_language][lifecycle_key]
        String[] my_lifecycle_keys = my_lifecycle.keySet() as String[]
        if(!(section in yaml_keys)) {
            //take the default
            if(section in my_lifecycle_keys) {
                if(my_lifecycle[section] instanceof List) {
                    output += my_lifecycle[section].collect{ (it == null)? '' : it }.join('\n') + '\n'
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
            output += jervis_yaml[section].collect{ (it == null)? '' : it }.join('\n') + '\n'
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
      Get a list of static branch names which is used for branch filtering.
      This is a simple literal list of Strings.

      @return A <tt>List</tt> of literal branch names meant to be filtered.
      */
    public List getFilteredBranchesList() {
        if(!filter_type) {
            return []
        }
        jervis_yaml['branches'][filter_type].findAll {
            String i=it.toString()
            (i[0] != '/' && i[-1] != '/' && i.size() > 0) || (i == '/' )
        }.collect {
            it.toString()
        }
    }

    /**
      This method makes it easy to detect if regular expressions are defined
      for branch filtering.

      @return <tt>true</tt> if a regex filter for branch filtering is available.
      */
    public Boolean hasRegexFilter() {
        getBranchRegexString() as Boolean
    }

    /**
      Detect if this branch is already covered by a regex filter.

      @param  branch The name of a branch to be compared with the regex.
      @return        <tt>true</tt> if a regex filter matches the <tt>branch</tt>.
      */
    public Boolean isFilteredByRegex(String branch) {
        if(!hasRegexFilter()) {
            return false
        }
        Pattern.compile(getBranchRegexString()).matcher(branch).matches()
    }

    /**
      Get the regular expression which is used for branch filtering.

      @return <tt>String</tt> which is compatible with
              <tt>{@link java.util.regex.Pattern}</tt>.  If no filters are
              found or the loaded Jervis YAML does not filter, then an empty
              String is returned.
     */
    public String getBranchRegexString() {
        if(!filter_type) {
            return ''
        }
        jervis_yaml['branches'][filter_type].findAll {
            String i=it.toString()
            i[0] == '/' && i[-1] == '/' && i.size() > 2
        }.collect {
            it[1..-2]
        }.join('|')
    }

    /**
      Get the regular expression for all whitelisted branches.  This is the
      combination of which is the combination of
      <tt>{@link #getBranchRegexString()}</tt> and <tt>branches</tt> into one
      regex to cover all cases.

      @param branches A list of branches.
      @return         A <tt>String</tt> which is a
                      <tt>{@link java.util.regex.Pattern}</tt>.  The
                      combination of internal regex filters with the branches.
      */
    public String getFullBranchRegexString(List branches) {
        String regex
        if(hasRegexFilter()) {
            regex = [getBranchRegexString(), branches.collect { Pattern.quote(it.toString()) }].flatten().join('|')
        }
        else {
            regex = branches.collect { Pattern.quote(it.toString()) }.join('|')
        }
        (regex)?: '.*'
    }

    /**
      Is this a branch which will generate a job?
      @param branch A branch to check if the job should be generated.
      @return       Returns <tt>true</tt> if the job should be generated or <tt>false</tt> if it should not.
     */
    public Boolean isGenerateBranch(String branch) {
        if(!filter_type) {
            return true
        }
        Boolean generate_branch = isFilteredByRegex(branch) || (branch in getFilteredBranchesList())
        //inverse if filter_type is 'except'
        (filter_type == 'only')? generate_branch : !generate_branch
    }

    /**
      Check if an object is an instance of any of the classes in a list.
      @param object Any kind of object.
      @param list   A list of classes deriving from type Class.
      @return       <tt>true</tt> if <tt>object</tt> is an instance of any one of the
                    items in the <tt>list</tt>.
     */
    public static final boolean isInstanceFromList(Object object, List<Class> list) {
        true in list*.isInstance(object)
    }

    /**
      Load a platforms file so that advanced labels can be generated for multiple
      platforms.  A platform could be a local datacenter or a cloud providor.
      The platforms file allows labels to be generated which include stability, sudo
      access, and even operating system.  This could be used to load lifecycles and
      toolchains by platform and OS.  This project comes with a platforms file.  The
      platforms file in this repository relative to the repository root is
      <tt>/src/main/resources/platforms.yaml</tt>.

      @param file A path to a platforms file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadPlatformsFile(String file, Boolean unstable = false) {
        if(!this.platform_obj) {
            this.platform_obj = new PlatformValidator()
        }
        this.platform_obj.loadYamlFile(file, unstable)
        this.platform_obj.validate()
    }

    /**
      Load a platforms YAML <tt>String</tt> so that advanced labels can be generated
      for multiple platforms.  A platform could be a local datacenter or a cloud
      providor.  The platforms file allows labels to be generated which include
      stability, sudo access, and even operating system.  This could be used to load
      lifecycles and toolchains by platform and OS.  This project comes with a
      platforms file.  The platforms file in this repository relative to the
      repository root is <tt>/src/main/resources/platforms.yaml</tt>.

      @param yaml A <tt>String</tt> containing YAML which is from a platforms file.
      @param unstable Load unstable, instead of stable.
     */
    public void loadPlatformsString(String yaml, Boolean unstable = false) {
        if(!this.platform_obj) {
            this.platform_obj = new PlatformValidator()
        }
        this.platform_obj.loadYamlString(yaml, unstable)
        this.platform_obj.validate()
    }

    /**
      Preload Jervis YAML for the purpose of loading lifecycles files for other
      platforms and operating systems.  <b>Please note</b>: you must call
      <tt>{@link #loadPlatformsString(java.lang.String)}</tt> or
      <tt>{@link #loadPlatformsFile(java.lang.String)}</tt> before calling this function.

      @param raw_yaml A <tt>String</tt> which contains Jervis YAML to be parsed.
     */
    public void preloadYamlString(String raw_yaml) throws JervisException {
        if(!this.platform_obj) {
            throw new PlatformValidationException('Must load the platforms file first.')
        }
        jervis_yaml = YamlOperator.loadYamlFrom(raw_yaml) ?: [:]
        // stability should always load first; load it twice to account for unstable defaults
        setLabel_stability(YamlOperator.getObjectValue(jervis_yaml, 'jenkins.unstable', this.platform_obj.platforms['defaults']['stability']))
        setLabel_stability(YamlOperator.getObjectValue(jervis_yaml, 'jenkins.unstable', this.platform_obj.getPlatforms(this.isUnstable)['defaults']['stability']))
        this.label_platform = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.platform', this.platform_obj.getPlatforms(this.isUnstable)['defaults']['platform'])
        this.label_os = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.os', this.platform_obj.getPlatforms(this.isUnstable)['defaults']['os'])
        setLabel_sudo(YamlOperator.getObjectValue(jervis_yaml, 'jenkins.sudo', this.platform_obj.getPlatforms(this.isUnstable)['defaults']['sudo']))
    }

    /**
      Returns a groovy expression which Jenkins would use to pin a job to specific
      nodes.

      @return A <tt>String</tt> which is a groovy expression of Jenkins node labels.
     */
    public String getLabels() {
        String labels = ["language:${yaml_language}", this.toolchain_obj.getToolchains(this.isUnstable)['toolchains'][yaml_language].join(' && ')].join(' && ')
        if(this.platform_obj) {
            labels = [this.label_stability, this.label_platform, this.label_os, this.label_sudo, labels].join(' && ')
        }
        //build on additional labels
        def additional_labels = YamlOperator.getObjectValue(jervis_yaml, 'jenkins.additional_labels', new Object())
        if(additional_labels instanceof String) {
            labels += " && ${additional_labels}"
        }
        else if(additional_labels instanceof List) {
            labels += " && " + additional_labels.findAll { it instanceof String }.join(' && ')
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
        Map restrictions = this.platform_obj.getPlatforms(this.isUnstable)['restrictions']
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
        if(this.platform_obj) {
            String [] supported_platforms = this.platform_obj.getPlatforms(this.isUnstable)['supported_platforms'].keySet() as String[]
            if(label_platform in supported_platforms) {
                String[] supported_os = this.platform_obj.getPlatforms(this.isUnstable)['supported_platforms'][label_platform].keySet() as String[]
                if(label_os in supported_os) {
                    //check the support of toolchains
                    LinkedHashSet toolchains_set = [] as Set
                    //start with the requested toolchains
                    toolchains_set = toolchains_set.plus(this.toolchain_obj.getToolchains(this.isUnstable)['toolchains'][yaml_language])
                    //subtract from the list of supported tool chains
                    toolchains_set = toolchains_set.minus(this.platform_obj.getPlatforms(this.isUnstable)['supported_platforms'][label_platform][label_os]['toolchain'])
                    //toolchains_set should be empty if the platform supports all toolchains
                    List supported_languages = this.platform_obj.getPlatforms(this.isUnstable)['supported_platforms'][label_platform][label_os]['language']
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
            throw new SecurityException("Call setPrivateKey before loading secrets.")
        }
        ciphermap.each { k, v ->
            plainmap[k] = secret_util.rsaDecrypt(v)
        }
        plainlist = plainmap.collect { k, v -> [key: k, secret: v] }
    }

    /**
      Load an RSA private key in-memory rather than depending on a file
      existing on disk.  This is required to decrypt secrets in a YAML file.
      Call this function before <tt>{@link #decryptSecrets()}</tt> if expecting
      encrypted YAML keys.

      @param pem A <tt>String</tt> whose contents is an X.509 PEM encoded RSA
                 private key.
     */
    public void setPrivateKey(String pem) {
        secret_util = new SecurityIO()
        secret_util.key_pair = pem
    }

    /**
      Determine if this instance is compatible with pipeline multibranch jobs.

      @return Returns <tt>true</tt> if this instance is compatible with pipeline multibranch jobs.
      @see #getJenkinsfile()
     */
    public boolean isPipelineJob() {
        YamlOperator.getObjectValue(jervis_yaml, 'jenkins.pipeline_jenkinsfile', '') || 'Jenkinsfile' in folder_listing
    }

    /**
      Get the <tt>Jenkinsfile</tt> for a pipeline.

      @return Returns the path to the <tt>Jenkinsfile</tt> the pipeline multibranch job will use.
      @see #isPipelineJob()
     */
    public String getJenkinsfile() {
        YamlOperator.getObjectValue(jervis_yaml, 'jenkins.pipeline_jenkinsfile', 'Jenkinsfile')
    }
}
