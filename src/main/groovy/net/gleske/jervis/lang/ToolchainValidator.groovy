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

import net.gleske.jervis.exceptions.ToolchainBadValueInKeyException
import net.gleske.jervis.exceptions.ToolchainMissingKeyException
import net.gleske.jervis.exceptions.ToolchainValidationException
import net.gleske.jervis.tools.YamlOperator

/**
  Validates the contents of a
  <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file</a>
  and provides quick access to supported matrices.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>
  <p><b>Please note</b>, if you are writing Job DSL plugin groovy scripts you
  should not use the relative file paths to access files in the repository
  where your DSL scripts reside.  Instead, use the
  <a href="https://github.com/samrocketman/jervis/issues/43" target="_blank"><tt>readFileFromWorkspace</tt></a>
  method provided by the Job DSL plugin in Jenkins.</p>
<pre><code>
import net.gleske.jervis.lang.ToolchainValidator

def toolchains = new ToolchainValidator()
toolchains.loadYamlFile('resources/toolchains-ubuntu2204-stable.yaml')
println 'Does the file validate? ' + toolchains.validate()
println 'Supported build matrices by language include:'
toolchains.languages.each { language -&gt;
    print "    ${language}:\n        "
    println toolchains.toolchain_list.findAll { tool -&gt;
        (tool == 'toolchains')? false : toolchains.supportedMatrix(language, tool)
    }.join('\n        ')
}
null
</code></pre>
 */
class ToolchainValidator implements Serializable {
    /**
      A list of fields to exclude when getting available tool versions.
      */
    private static List nonToolValueFields = [
        '*',
        'comment',
        'default_ivalue',
        'friendlyLabel',
        'matrix'
    ]
    /**
      Check if there is an unstable toolchains object available.
      */
    private Boolean isUnstable(Boolean unstable) {
        unstable && this.unstable_toolchains
    }

    /**
      A <tt>{@link Map}</tt> of the parsed toolchains file.
     */
    Map toolchains

    /**
      Returns a copy of a parsed toolchains file for either stable toolchains
      or unstable toolchains.
      @param unstable Request unstable toolchains instead of stable.
      @return A parsed toolchains file.
      */
    Map getToolchains(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_toolchains : this.@toolchains
    }

    /**
      A <tt>{@link Map}</tt> of the parsed unstable toolchains file.
     */
    Map unstable_toolchains

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      toolchains in the toolchains file.  This is just a list of the keys in
      <tt>{@link #toolchains}</tt>.
     */
    String[] toolchain_list
    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      toolchains in the toolchains file.  This is just a list of the keys in
      <tt>{@link #toolchains}</tt>.
      @param unstable Request unstable instead of stable.
      @return Returns a toolchain list.
     */
    String[] getToolchain_list(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_toolchain_list : this.@toolchain_list
    }

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      toolchains in the unstable toolchains file.  This is just a list of the
      keys in <tt>{@link #unstable_toolchains}</tt>.
     */
    String[] unstable_toolchain_list

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      toolchains in the toolchains file which are capable of matrix building.
      This is just a list of the keys in <tt>{@link #toolchains}</tt>.
     */
    List matrix_toolchain_list

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      toolchains in the toolchains file which are capable of matrix building.
      This is just a list of the keys in <tt>{@link #toolchains}</tt>.
      @param unstable Request unstable instead of stable.
      @return Returns a matrix toolchain list.
     */
    List getMatrix_toolchain_list(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_matrix_toolchain_list : this.@matrix_toolchain_list
    }

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of toolchains in the toolchains file which are capable of matrix building.  This is just a list of the keys in <tt>{@link #toolchains}</tt>.
     */
    List unstable_matrix_toolchain_list

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the lifecycles file.  This is just a list of the
      keys in {@link #lifecycles}.
     */
    String[] languages

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the lifecycles file.  This is just a list of the
      keys in {@link #lifecycles}.
      @param unstable Request unstable instead of stable.
      @return Returns a languages list.
     */
    String[] getLanguages(Boolean unstable = false) {
        this.isUnstable(unstable) ?
            this.unstable_languages : this.@languages
    }

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of
      supported languages in the unstable toolchains file.  This is just a list
      of the keys in {@link #unstable_toolchains}.
     */
    String[] unstable_languages

    /**
      Load the YAML of a toolchains file and parse it.  This should be the first
      function called after class instantiation.  It populates
      <tt>{@link #toolchains}</tt>, <tt>{@link #toolchain_list}</tt>, and
      <tt>{@link #languages}</tt>.
      @param file A <tt>String</tt> which is a path to a toolchains file.
     */
    public void loadYamlFile(String file, Boolean unstable = false) {
        loadYamlString(new File(file).text, unstable)
    }

    /**
      Parse the YAML which is the contents of a toolchains file.  It populates
      <tt>{@link #toolchains}</tt>, <tt>{@link #toolchain_list}</tt>, and
      <tt>{@link #languages}</tt>.  This is required in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job.
      DSL Plugin.
      @param yaml A <tt>String</tt> the contents of a toolchains file.
     */
    public void loadYamlString(String yaml, Boolean unstable = false) {
        if(unstable) {
            // merge toolchains (preserving a merge with child key named
            // 'toolchains')
            this.unstable_toolchains = YamlOperator.deepCopy(this.@toolchains)
            Map tempUnstableToolchains = YamlOperator.loadYamlFrom(yaml) ?: [:]
            Map toolsByLanguage = YamlOperator.deepCopy(this.@toolchains.toolchains)
            toolsByLanguage.putAll(tempUnstableToolchains.toolchains ?: [:])
            this.unstable_toolchains.putAll(tempUnstableToolchains)
            this.unstable_toolchains.toolchains = toolsByLanguage
            // end merge toolchains
            this.unstable_toolchain_list = this.unstable_toolchains.keySet() as String[]
            this.unstable_matrix_toolchain_list = this.unstable_toolchain_list.findAll { String toolchain ->
                toolchainType(toolchain, true) != 'disabled'
            }
            if('toolchains' in this.unstable_toolchain_list) {
                this.unstable_languages = this.unstable_toolchains.toolchains.keySet() as String[]
            }
        }
        else {
            this.toolchains = YamlOperator.loadYamlFrom(yaml) ?: [:]
            this.toolchain_list = this.@toolchains.keySet() as String[]
            this.matrix_toolchain_list = this.toolchain_list.findAll { String toolchain ->
                toolchainType(toolchain) != 'disabled'
            }
            if('toolchains' in this.toolchain_list) {
                this.languages = this.@toolchains.toolchains.keySet() as String[]
            }
        }
    }

    /**
      Checks to see if a language is a supported language based on the toolchains file.
      @param lang A <tt>String</tt> which is a language to look up based on the keys in the toolchains file.
      @return     <tt>true</tt> if the language is supported or <tt>false</tt> if the language is not supported.
     */
    public Boolean supportedLanguage(String lang, Boolean unstable = false) {
        lang in this.getLanguages(unstable)
    }

    /**
      Checks to see if a value is a supported toolchain based on the toolchains file.

      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the
                       keys in the toolchains file.

      @return          <tt>true</tt> if the toolchain is supported or <tt>false</tt>
                       if the toolchain is not supported.  Note: it can exist as a
                       toolchain but not be supported as a matrix builder.
     */
    public Boolean supportedToolchain(String toolchain, Boolean unstable = false) {
        toolchain in this.getToolchain_list(unstable)
    }

    /**
      Checks to see what type a toolchain is.  This function assumes successful
      validation and will not throw an exeption for a toolchain which does not exist.
      If the toolchain does not exist it will return <tt>simple</tt>.

      @return A <tt>String</tt> which has one of three values: <tt>advanced</tt>,
              <tt>simple</tt>, <tt>disabled</tt>.
     */
    public String toolchainType(String toolchain, Boolean unstable = false) {
        if('matrix' in this.getToolchains(unstable)[toolchain]) {
            this.getToolchains(unstable)[toolchain]['matrix']
        }
        else {
            'simple'
        }
    }
    /**
      Get a list of values for a given toolchain.  If the returns List is
      empty, then it is likely allows any value due to wildcard <tt>*</tt>.
      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the
                       keys in the toolchains file.
      */
    List toolValues(String toolchain, Boolean unstable = false) {
        this.getToolchains(unstable)[toolchain].keySet().toList() - this.nonToolValueFields
    }

    /**
      Check to see if a given <tt>tool</tt> is supported in the <tt>toolchain</tt>.  A
      <tt>tool</tt> is supported if there is a key for it in the toolchains file.  If
      there is no key for it then it is still supported if there is a <tt>*</tt> key.
      If there is no key for it and no <tt>*</tt> key for that toolchain then the
      <tt>tool</tt> is not supported.  There is no checking to see if a
      <tt>toolchain</tt> is actually valid.
      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the
                       keys in the toolchains file.
      @param tool A value intended for the existing toolchain to check against
                  allowed values for support.
      @return <tt>true</tt> if the <tt>tool</tt> is supported or <tt>false</tt> if it is not supported.
     */
    public Boolean supportedTool(String toolchain, String tool, Boolean unstable = false) {
        return (tool in toolValues(toolchain, unstable)) || ('*' in this.getToolchains(unstable)[toolchain].keySet().toList())
    }

    /**
      Checks to see if a toolchain is a supported build matrix based on a specific language.
      @param lang      A <tt>String</tt> which is a language to look up in the toolchains file.
      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the <tt>lang</tt> to see if it is a matrix building attribute.
      @return          <tt>true</tt> if the toolchain is a matrix builder or <tt>false</tt> if the matrix build is not supported for that language.  Note: it can exist as a toolchain but not be supported as a matrix builder.
     */
    public Boolean supportedMatrix(String lang, String toolchain, Boolean unstable = false) {
        (toolchain in this.getToolchains(unstable)['toolchains'][lang]) &&
            (toolchain in this.getMatrix_toolchain_list(unstable))
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the lifecycles file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate(true)
            this.validate(false)
            return true
        }
        catch(ToolchainValidationException E) {
            return false
        }
    }

    /**
      Validates the toolchains file.
      @return <tt>true</tt> if the toolchains file validates.  If the toolchains file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.ToolchainValidationException}</tt>.
     */
    public Boolean validate() throws ToolchainMissingKeyException {
        this.validate(false)
        this.validate(true)
    }
    /**
      Validates the toolchains file.
      @return <tt>true</tt> if the toolchains file validates.  If the toolchains file fails validation then it will throw a <tt>{@link net.gleske.jervis.exceptions.ToolchainValidationException}</tt>.
     */
    public Boolean validate(Boolean unstable) throws ToolchainMissingKeyException {
        Map toolchains = this.getToolchains(unstable)
        //check for toolchains key
        if(!this.supportedToolchain('toolchains', unstable)) {
            throw new ToolchainMissingKeyException('toolchains')
        }
        //check for "advanced" env missing the matrix key in toolchains.yaml; now a requirement
        if(('env' in toolchains) && !('matrix' in toolchains['env'])) {
            throw new ToolchainMissingKeyException('env.matrix; env must be updated to include a "matrix: advanced" key.')
        }
        //check all of the toolchains inside of the toolchains key
        (toolchains['toolchains'].keySet() as String[]).each{ language ->
            toolchains['toolchains'][language].each{ toolchain ->
                if(!this.supportedToolchain(toolchain, unstable)) {
                    throw new ToolchainMissingKeyException("toolchains.${language}.${toolchain}.  The toolchain for ${toolchain} is missing from the top level of the toolchains file.")
                }
            }
        }

        // validate toolchain keys, their script, and other fields
        toolchains.each { String tool, Map toolMap ->
            if(tool == 'toolchains') {
                return
            }
            List toolchain_ivalue = toolMap.keySet().toList()
            toolMap.each { toolVersion, script ->
                if(toolVersion == 'friendlyLabel') {
                    if(!(script in Boolean)) {
                        throw new ToolchainBadValueInKeyException("${tool}.friendlyLabel must be a String and must have one of three values: disabled, simple, advanced.")
                    }
                }
                else if(toolVersion == 'default_ivalue') {
                    def default_ivalue = script
                    // default_ivalue should exist as a tool or wildcard
                    if(!(default_ivalue in String) || !([default_ivalue, '*'].any { it in toolchain_ivalue })) {
                        throw new ToolchainMissingKeyException("${tool}.default_ivalue.${default_ivalue} is missing.  " +
                                "Must have one of the two following keys: ${tool}.${default_ivalue} or ${tool}.* (i.e. wildcard).")
                    }
                }
                else if(toolVersion == 'matrix') {
                    if(!([String, ['disabled', 'simple', 'advanced']].every { script in it })) {
                        throw new ToolchainBadValueInKeyException("${tool}.matrix must be a String and must have one of three values: disabled, simple, advanced.")
                    }
                }
                else {
                    // any remaining tool scripts including 'cleanup'
                    if(([String, List].every { !(script in it) }) || ((script in List) && !script.every { it in String })) {
                        throw new ToolchainBadValueInKeyException("${tool}.${toolVersion} must be a String or List of Strings.")
                    }
                }
            }
        }

        return true
    }

    /**
      Can a toolchain be used with a friendly label when Jenkins matrix axes are used?  This answers that question.

      @return Returns <tt>true</tt> if a friendly label can be used otherwise <tt>false</tt>
     */
    public boolean isFriendlyLabel(String toolchain, Boolean unstable = false) {
        if('friendlyLabel' in this.getToolchains(unstable)[toolchain].keySet()) {
            return this.getToolchains(unstable)[toolchain]['friendlyLabel'].toString().equals('true')
        }
        return false
    }
}
