package jervis.lang

import groovy.json.JsonSlurper
import jervis.exceptions.ToolchainMissingKeyException
import jervis.exceptions.ToolchainValidationException

/**
  Validates the contents of a <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file</a> and provides quick access to supported matrices.

  <h2>Sample usage</h2>
<pre><tt>import jervis.lang.toolchainValidator
import jervis.tools.scmGit
def git = new scmGit()
def toolchains = new toolchainValidator()
toolchains.load_JSON(git.getRoot() + "/src/resources/toolchains.json")
println "Does the file validate? " + toolchains.validate()
println "Supported matrices include:"
(toolchains.toolchains['toolchains'].keySet() as String[]).each {
    String language = it
    ArrayList supported = []
    (toolchains.toolchains.keySet() as String[]).each {
        if(toolchains.supportedMatrix(language, it)) {
            supported << it
        }
    }
    println "  ${language}: " + supported.join(', ')
}</tt></pre>
 */
class toolchainValidator {

    /**
      A <tt>{@link HashMap}</tt> of the parsed toolchains file.
     */

    def toolchains

    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of toolchains in the toolchains file.  This is just a list of the keys in <tt>{@link #toolchains}</tt>.
     */
    def toolchain_list

    /**
      Load the JSON of a toolchains file and parse it.  This should be the first function called after class instantiation.  It populates <tt>{@link #toolchains}</tt> and <tt>{@link #toolchain_list}</tt>.
      @param file A <tt>String</tt> which is a path to a toolchains file.
     */
    public void load_JSON(String file) {
        toolchains = new groovy.json.JsonSlurper().parse(new File(file).newReader())
        toolchain_list = toolchains.keySet() as String[];
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
      @return <tt>true</tt> if the toolchains file validates.  If the toolchains file fails validation then it will throw a <tt>{@link jervis.exceptions.ToolchainValidationException}</tt>.
     */
    public Boolean validate() {
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
