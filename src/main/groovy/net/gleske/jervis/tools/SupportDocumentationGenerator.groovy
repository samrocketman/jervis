/*
   Copyright 2014-2021 Sam Gleske - https://github.com/samrocketman/jervis

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

package net.gleske.jervis.tools

import net.gleske.jervis.exceptions.JervisException

import groovy.text.SimpleTemplateEngine
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
  This is a utility class for Jervis admins to generate documentation on the
  fly for their specific environment.  An admin will bring their own
  platforms.json, a JSON file for language lifecycles for each operating system
  they support, and a JSON file for language toolchains for each operating
  system they support.  Without this generator, managing documentation for a
  custom solution is very hard.  This documentation generator was developed to
  make managing docs a little easier for Jervis platforms and environments.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a> with the classpath set up.</p>

<pre><tt>import net.gleske.jervis.tools.SupportDocumentationGenerator as Doc

Doc docs = new Doc()

println docs.supportByOS
</tt></pre>
  */
class SupportDocumentationGenerator {

    static SimpleTemplateEngine engine = new SimpleTemplateEngine()

    Map jsonFiles = [:]
    Map supportByOS = [:]

    Map docByLanguage = [:]
    File outputDir

    String serviceName = 'Jenkins'

    def SupportDocumentationGenerator() {
        this(
            templateDir: 'src/main/resources/net/gleske/jervis/doctemplates/',
            jsonDir: 'resources/',
            outputDir: '/tmp/doc')
    }

    def SupportDocumentationGenerator(Map args) {
        if(this.validateArgs(args)) {
            throw new JervisException('\nERRORS with SupportDocumentationGenerator arguments:\n    ' + this.validateArgs(args).join('\n    '))
        }
        this.parseJsonFiles(args.jsonDir -~ '/$')
    }

    /**
      Validates the argument list provided to the constructor.
      */
    private List validateArgs(Map args) {
        []
    }

    void parseJsonFiles(String jsonDir) {
        def yaml = new Yaml(new SafeConstructor())
        this.jsonFiles['platforms'] = yaml.load(new File(jsonDir + '/platforms.json').text)
        String stability = this.jsonFiles.platforms.defaults.stability
        this.jsonFiles.platforms.supported_platforms.each { k, platform ->
            platform.each { os, v ->
                this.jsonFiles["lifecycles-${os}".toString()] = yaml.load(new File(jsonDir + "/lifecycles-${os}-${stability}.json").text)
                this.jsonFiles["toolchains-${os}".toString()] = yaml.load(new File(jsonDir + "/toolchains-${os}-${stability}.json").text)
                this.supportByOS[os] = [languages: this.getLanguages(os)]
                this.supportByOS[os]['toolchains'] = this.getToolchains(os)
            }
        }
    }

    /**
      Get the full supported language documentation for a given operating system.

      @param os An operating system supported in platforms, lifecycles, and toolchains JSON files.
      @return A rendered markdown document.
      */
    String getLifecycleDocumentation(String os = '') {
        if(!os) {
            os = this.jsonFiles.platforms.defaults.os
        }
        this.supportedByOS[os].languages.collect { language ->
            this.getLifecycleDocumentation(os, language)
        }*.trim().join('\n\n')
    }

    /**
      Return markdown documentation for a given OS supporting a language.
      */
    String getLifecycleDocumentation(String os, String language) {
        if(!(language in supportedByOS[os].languages)) {
            throw new JervisException("Language ${language} is not supported by ${os}.")
        }
        Map binding = [
            friendlyName: this.jsonFiles."lifecycles-${os}"."${language}".friendlyName,
            jsonFiles: this.jsonFiles,
            language: language,
            os: os,
            supportedByOS: this.supportedByOS
        ]
    }

    /**
      Get supported languages for a given OS.  A language is supported if it is
      in both the lifecycles and toolchains for a given OS.

      @param os The operating system to get supported languages.
      */
    private List getLanguages(String os) {
        this.jsonFiles."lifecycles-${os}".keySet().toList().sort().findAll {
            it in this.jsonFiles."toolchains-${os}".toolchains.keySet()
        }
    }

    /**
      Get supported toolchains for a given OS.

      @param os The operating system to get supported toolchains.
      */
    private List getToolchains(String os) {
        this.jsonFiles."toolchains-${os}".keySet().toList().findAll {
            it != 'toolchains'
        }
    }
}
