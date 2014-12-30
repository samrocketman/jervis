package jervis.lang

import groovy.json.JsonSlurper
import jervis.exceptions.BadValueInKeyException
import jervis.exceptions.InfiniteLoopException
import jervis.exceptions.MissingKeyException
import jervis.exceptions.ValidationException


//import jervis.lang.lifecycleValidator
//URL url = new URL('file:///home/sam/git/github/jervis/src/resources/lifecycles.json')
//def x = new lifecycleValidator()
//x.load_JSON(url)
//x.validate()

/**
  Validates the contents of a lifecycle file and provides quick access to supported languages.

  <h2>Sample usage</h2>
<pre><tt>import jervis.lang.lifecycleValidator
import jervis.tools.scmGit
def git = new scmGit()
def lifecycles = new lifecycleValidator()
lifecycles.load_JSON(git.getRoot() + "/src/resources/lifecycles.json")
println "Does the file validate? " + lifecycles.validate()
println "Supported languages include:"
//print out a sorted ArrayList of supported languages
supported_languages = []
lifecycles.languages.each { supported_languages << lifecycles.lifecycles[it]["friendlyName"] }
Collections.sort(supported_languages)
supported_languages.each{ println it }</tt></pre>
 */
class lifecycleValidator {
    def lifecycles
    def languages
    def load_JSON(String file) {
        lifecycles = new groovy.json.JsonSlurper().parse(new File(file).newReader())
        languages = lifecycles.keySet() as String[];
    }
    def supportedLanguage(String lang) {
        lang in languages
    }
    def validate_asBool() {
        try {
            this.validate()
            return true
        }
        catch(ValidationException E) {
            return false
        }
    }
    def validate() {
        lifecycles.keySet().each {
            def tools = lifecycles[it].keySet() as String[]
            if(!("defaultKey" in tools)) {
                throw new MissingKeyException([it,"defaultKey"].join('.'))
            }
            if(!("friendlyName" in tools)) {
                throw new MissingKeyException([it,"friendlyName"].join('.'))
            }
            if(!(lifecycles[it]["defaultKey"] in tools)) {
                throw new MissingKeyException([it,"defaultKey",lifecycles[it]["defaultKey"]].join('.'))
            }
            def current_key = lifecycles[it]["defaultKey"]
            def count=0
            while(lifecycles[it][current_key] != null) {
                def cycles = lifecycles[it][current_key].keySet() as String[]
                if("fileExistsCondition" in cycles) {
                    //check for leading slash in the first element of fileExistsCondition
                    if(lifecycles[it][current_key]["fileExistsCondition"][0][0] != '/') {
                        throw new BadValueInKeyException([it,current_key,"fileExistsCondition","[0]"].join('.') + " first element does not begin with a '/'.")
                    }
                }
                if("fallbackKey" in cycles) {
                    if(!(lifecycles[it][current_key]["fallbackKey"] in tools)) {
                        throw new MissingKeyException([it,current_key,"fallbackKey",lifecycles[it][current_key]["fallbackKey"]].join('.'))
                    }
                    if(!("fileExistsCondition" in cycles)) {
                        throw new MissingKeyException([it,current_key,"fileExistsCondition"].join('.') + " required by " + [it,current_key,"fallbackKey"].join('.'))
                    }
                }
                count++
                if(count > 1000) {
                    throw new InfiniteLoopException([it,current_key].join('.'))
                }
                current_key = lifecycles[it][current_key]["fallbackKey"]
            }
        }
        return true
    }
}
