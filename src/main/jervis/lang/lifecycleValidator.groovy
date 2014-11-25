package jervis.lang

import groovy.json.JsonSlurper

//import jervis.lang.lifecycleValidator
//URL url = new URL('file:///home/sam/git/github/jervis/src/resources/lifecycles.json')
//def x = new lifecycleValidator()
//x.load_JSON(url)
//x.validate()

class lifecycleValidator {
    def wiki_page = "https://github.com/samrocketman/jervis/wiki/Default-commands-for-supported-languages-file"
    def lifecycles
    def languages
    def load_JSON(URL url) {
        lifecycles = new groovy.json.JsonSlurper().parse(new File(url.getFile()).newReader())
        languages = lifecycles.keySet() as String[];
    }
    def supportedLanguage(String lang) {
        lang in languages
    }
    def throw_key_exception(rootKey) {
        throw Exception("\nERROR: Lifecycle validation failed.  Missing key: " + rootKey + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n') )
    }
    def throw_value_exception(rootKey, message) {
        throw Exception("\nERROR: Lifecycle validation failed.  Bad value in key: " + rootKey + message + ["\n\nSee wiki page:", wiki_page,"\n"].join('\n') )
    }
    def validate() {
        lifecycles.keySet().each {
            println it
            println "this is a test"
            println lifecycles[it]["defaultKey"]
            def tools = lifecycles[it].keySet() as String[]
            if(!("defaultKey" in tools)) {
                this.throw_exception([it,"defaultKey"].join('.'))
            }
            if(!(lifecycles[it]["defaultKey"] in tools)) {
                this.throw_exception([it,"defaultKey",lifecycles[it]["defaultKey"]].join('.'))
            }
            def current_key = lifecycles[it]["defaultKey"]
            while(lifecycles[it][current_key] != null) {
                def cycles = lifecycles[it][current_key].keySet() as String[]
                if("fileExistsCondition" in cycles) {
                    //check for leading slash in the first element of fileExistsCondition
                    if(lifecycles[it][current_key]["fileExistsCondition"][0][0] != '/') {
                        this.throw_value_exception([it,current_key,"fileExistsCondition","[0]"].join('.'), " first element does not begin with a '/'.")
                    }
                }
            }
        }
/*
        lifecycles.findAll{true}.each { language, tools ->
            tools.findAll{true}.each { tool, cycles ->
                println language + '.' + tool
                println cycles
                println cycles.getClass()
                if(cycles instanceof java.lang.String) {
                    if(tool != 'defaultKey') {
                        throw Exception("ERROR: Lifecycle validation failed.  Unknown key at: " + [language, tool].join('.') )
                    }
                }
                else {
                    cycles.findAll{true}.each { cycle, command ->
                        println [language, tool, cycle].join('.')
                        println command
                        println command.getClass()
                    }
                }
            }
        }
*/
    }
}
