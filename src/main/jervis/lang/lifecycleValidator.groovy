package jervis.lang

import groovy.json.JsonSlurper

class lifecycleValidator {
    def lifecycles
    def load_JSON(URL url) {
        lifecycles = new groovy.json.JsonSlurper().parse(new File(url.getFile()).newReader())
    }
    def validate() {
        languages = lifecycles.findAll{true}.collect { it.key }
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
