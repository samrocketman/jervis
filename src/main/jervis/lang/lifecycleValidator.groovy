package jervis.lang

import groovy.json.JsonSlurper
import jervis.exceptions.BadValueInKeyException
import jervis.exceptions.InfiniteLoopException
import jervis.exceptions.MissingKeyException


//import jervis.lang.lifecycleValidator
//URL url = new URL('file:///home/sam/git/github/jervis/src/resources/lifecycles.json')
//def x = new lifecycleValidator()
//x.load_JSON(url)
//x.validate()

class lifecycleValidator {
    def wiki_page = "https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file"
    def lifecycles
    def languages
    def load_JSON(URL url) {
        lifecycles = new groovy.json.JsonSlurper().parse(new File(url.getFile()).newReader())
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
        catch(Exception E) {
            return false
        }
    }
    def validate() {
        lifecycles.keySet().each {
            def tools = lifecycles[it].keySet() as String[]
            if(!("defaultKey" in tools)) {
                throw new MissingKeyException([it,"defaultKey"].join('.'))
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
