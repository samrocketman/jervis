/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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

import groovy.json.JsonSlurper
import net.gleske.jervis.exceptions.ToolchainMissingKeyException
import net.gleske.jervis.exceptions.ToolchainValidationException

/**
  Validates the contents of a
  <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file</a>
  and provides quick access to supported matrices.

  <h2>Sample usage</h2>
  <p>Please note, if you are writing Job DSL plugin groovy scripts you should not
  use the <tt>scmGit</tt> class to access files in the repository where your DSL
  scripts reside.  Instead, use the
  <a href="https://github.com/samrocketman/jervis/issues/43" target="_blank"><tt>readFileFromWorkspace</tt></a>
  method provided by the Job DSL plugin in Jenkins.</p>
<pre><tt>import net.gleske.jervis.lang.toolchainValidator
import net.gleske.jervis.tools.scmGit
def git = new scmGit()
def toolchains = new toolchainValidator()
toolchains.load_JSON(git.getRoot() + '/src/main/resources/toolchains.json')
println 'Does the file validate? ' + toolchains.validate()
println 'Supported matrices include:'
toolchains.languages.each {
    String language = it
    ArrayList supported = []
    toolchains.toolchain_list.each {
        if(toolchains.supportedMatrix(language, it)) {
            supported << it
        }
    }
    println "  ${language}: " + supported.join(', ')
}</tt></pre>
 */
class toolchainValidator {

    /**
      A <tt>{@link Map}</tt> of the parsed toolchains file.
     */

    Map toolchains

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of toolchains in the toolchains file.  This is just a list of the keys in <tt>{@link #toolchains}</tt>.
     */
    String[] toolchain_list

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of supported languages in the lifecycles file.  This is just a list of the keys in {@link #lifecycles}.
     */
    String[] languages

    /**
      Load the JSON of a toolchains file and parse it.  This should be the first
      function called after class instantiation.  It populates
      <tt>{@link #toolchains}</tt>, <tt>{@link #toolchain_list}</tt>, and
      <tt>{@link #languages}</tt>.
      @param file A <tt>String</tt> which is a path to a toolchains file.
     */
    public void load_JSON(String file) {
        toolchains = new groovy.json.JsonSlurper().parse(new File(file).newReader())
        toolchain_list = toolchains.keySet() as String[]
        if('toolchains' in toolchain_list) {
            languages = toolchains.toolchains.keySet() as String[]
        }
    }

    /**
      Parse the JSON which is the contents of a toolchains file.  It populates
      <tt>{@link #toolchains}</tt>, <tt>{@link #toolchain_list}</tt>, and
      <tt>{@link #languages}</tt>.  This is required in order to use the
      <a href="https://github.com/samrocketman/jervis/issues/43#issuecomment-73638215" target="_blank"><tt>readFileFromWorkspace</tt></a>
      method from the Jenkins Job.
      DSL Plugin.
      @param json A <tt>String</tt> the contents of a toolchains file.
     */
    public void load_JSONString(String json) {
        toolchains = new groovy.json.JsonSlurper().parseText(json)
        toolchain_list = toolchains.keySet() as String[]
        if('toolchains' in toolchain_list) {
            languages = toolchains.toolchains.keySet() as String[]
        }
    }

    /**
      Checks to see if a language is a supported language based on the toolchains file.
      @param lang A <tt>String</tt> which is a language to look up based on the keys in the toolchains file.
      @return     <tt>true</tt> if the language is supported or <tt>false</tt> if the language is not supported.
     */
    public Boolean supportedLanguage(String lang) {
        lang in languages
    }

    /**
      Checks to see if a value is a supported toolchain based on the toolchains file.
      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the keys in the toolchains file.
      @return     <tt>true</tt> if the toolchain is supported or <tt>false</tt> if the toolchain is not supported.  Note: it can exist as a toolchain but not be supported as a matrix builder.
     */
    public Boolean supportedToolchain(String toolchain) {
        toolchain in toolchain_list
    }

    /**
      Check to see if a given <tt>tool</tt> is supported in the <tt>toolchain</tt>.  A
      <tt>tool</tt> is supported if there is a key for it in the toolchains file.  If
      there is no key for it then it is still supported if there is a <tt>*</tt> key.
      If there is no key for it and no <tt>*</tt> key for that toolchain then the
      <tt>tool</tt> is not supported.  There is no checking to see if a
      <tt>toolchain</tt> is actually valid.
      @return <tt>true</tt> if the <tt>tool</tt> is supported or <tt>false</tt> if it is not supported.
     */
    public Boolean supportedTool(String toolchain, String tool) {
        def tools = toolchains[toolchain].keySet() as String[]
        return (tool in tools) || ('*' in tools)
    }

    /**
      Checks to see if a toolchain is a supported build matrix based on a specific language.
      @param lang      A <tt>String</tt> which is a language to look up in the toolchains file.
      @param toolchain A <tt>String</tt> which is a toolchain to look up based on the <tt>lang</tt> to see if it is a matrix building attribute.
      @return          <tt>true</tt> if the toolchain is a matrix builder or <tt>false</tt> if the matrix build is not supported for that language.  Note: it can exist as a toolchain but not be supported as a matrix builder.
     */
    public Boolean supportedMatrix(String lang, String toolchain) {
        toolchain in toolchains['toolchains'][lang]
    }

    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the lifecycles file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate()
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
        //check for toolchains key
        if(!this.supportedToolchain('toolchains')) {
            throw new ToolchainMissingKeyException('toolchains')
        }
        //check all of the toolchains inside of the toolchains key
        (toolchains['toolchains'].keySet() as String[]).each{
            def language = it
            toolchains['toolchains'][it].each{
                if(!this.supportedToolchain(it)) {
                    throw new ToolchainMissingKeyException("toolchains.${language}.${it}.  The toolchain for ${it} is missing from the top level of the toolchains file.")
                }
            }
        }
        for(int i=0; i<toolchain_list.size(); i++){
            if('toolchains' == toolchain_list[i]) {
                continue
            }
            def toolchain_ivalue = toolchains[toolchain_list[i]].keySet() as String[]
            if('default_ivalue' in toolchain_ivalue) {
                def default_ivalue = toolchains[toolchain_list[i]]['default_ivalue']
                if(!(default_ivalue in toolchain_ivalue) && !('*' in toolchain_ivalue)) {
                    throw new ToolchainMissingKeyException("${toolchain_list[i]}.default_ivalue.${default_ivalue} is missing.  " +
                            "Must have one of the two following keys: ${toolchain_list[i]}.${default_ivalue} or ${toolchain_list[i]}.*.")
                }
            }
        }
        return true
    }
}
