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

package net.gleske.jervis.beta

import static net.gleske.jervis.tools.AutoRelease.getScriptFromTemplate
import net.gleske.jervis.exceptions.JervisException
import net.gleske.jervis.tools.YamlOperator

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

<pre><code>
import net.gleske.jervis.tools.SupportDocumentationGenerator as Doc

Doc docs = new Doc()

println docs.supportByOS
</code></pre>
  */
class SupportDocumentationGenerator {

    /**
      Contains the parsed content of all Jervis JSON files such as platforms,
      lifecycles, and toolchains for every OS.  Each key is the name of the
      JSON file but without <tt>.json</tt> extension, without
      <tt>-stable.json</tt>, or without <tt>-unstable.json</tt> depending on
      default stability.
      */
    Map jsonFiles = [:]
    Map supportByOS = [:]

    /**
      Groovy templates for generating documenation.

      <p>The following templates are supported.</p>
      <ul>
          <li>
              <tt>lifecycle-introduction</tt> contains the contents of
              <tt>lifecycle-introduction.tmpl.md</tt>.  This is the documentation
              at the top of each language for its lifecycles documentation.  For
              each build tool supported for default lifecycles refer to the
              <tt>lifecycle</tt> template.
          </li>
          <li>
              <tt>lifecycle</tt> contains the contents of
              <tt>lifecycle.tmpl.md</tt>.  This template is for documenting
              file detection and fallback behavior for different build tools in
              the lifecycle phase of a supported language.
          </li>
      </ul>
      */
    Map templates = [:]


    String serviceName = 'Jenkins'

    def SupportDocumentationGenerator() {
        this(
            templateDir: 'src/main/resources/net/gleske/jervis/doctemplates/',
            jsonDir: 'resources/',
            outputDir: '/tmp/doc')
    }

    def SupportDocumentationGenerator(Map args) {
        if(this.validateArgs(args)) {
            throw new JervisException('\n\nERRORS with SupportDocumentationGenerator arguments:\n    ' + this.validateArgs(args).join('\n    ') + '\n\n')
        }
        this.parseJsonFiles(args.jsonDir -~ '/$')
        this.parseTemplates((args.templateDir ?: '') -~ '/$')
    }

    /**
      */
    private void parseTemplates(String templateDir) {
        ['lifecycle.tmpl.md', 'lifecycle-introduction.tmpl.md'].each {
            templates[(it -~ '\\.tmpl\\.md$').toString()] = new File(templateDir + "/${it}").text
        }
    }

    /**
      Validates the argument list provided to the constructor.
      */
    private List validateArgs(Map args) {
        List errors = []
        if(!(new File((args?.jsonDir -~ '/$') + '/platforms.json' ).exists())) {
            errors << '- jsonDir: The directory must contain a platforms.json file and its associated lifecycles and toolchains JSON files.'
        }
        errors
    }

    private void parseJsonFiles(String jsonDir) {
        this.jsonFiles['platforms'] = YamlOperator.loadYamlFrom(new File(jsonDir + '/platforms.json'))
        String stability = this.jsonFiles.platforms.defaults.stability
        this.jsonFiles.platforms.supported_platforms.each { k, platform ->
            platform.each { os, v ->
                this.jsonFiles["lifecycles-${os}".toString()] = YamlOperator.loadYamlFrom(new File(jsonDir + "/lifecycles-${os}-${stability}.json"))
                this.jsonFiles["toolchains-${os}".toString()] = YamlOperator.loadYamlFrom(new File(jsonDir + "/toolchains-${os}-${stability}.json"))
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
        this.supportByOS[os].languages.collect { language ->
            this.getLifecycleDocumentation(os, language)
        }.join('\n\n')
    }

    /**
      Return markdown documentation for a given OS supporting a language.
      */
    String getLifecycleDocumentation(String os, String language) {
        if(!(language in supportByOS[os].languages)) {
            throw new JervisException("Language ${language} is not supported by ${os}.")
        }
        Map binding = [
            defaultKey: this.jsonFiles."lifecycles-${os}"."${language}".defaultKey,
            friendlyName: this.jsonFiles."lifecycles-${os}"."${language}".friendlyName,
            jsonFiles: this.jsonFiles,
            language: language,
            lifecycle: this.jsonFiles."lifecycles-${os}"."${language}",
            os: os,
            serviceName: serviceName,
            supportByOS: this.supportByOS
        ]
        List documentation = [getScriptFromTemplate(templates.'lifecycle-introduction', binding).trim()]

        // get all lifecycles sorted by their order in which Jervis will detect
        // build tools and fall back.
        binding.lifecycle.findAll { k, v ->
            v instanceof Map
        }.sort { a, b ->
            if(a.key == binding.defaultKey || b.key == a.value?.fallbackKey) {
                -1
            } else {
                1
            }
        }.each { k, v ->
            String nextFile = (binding.lifecycle."${v.fallbackKey}"?.fileExistsCondition) ?: ''
            Boolean onlyEntry = (!nextFile && !v.fallbackKey && k == binding.defaultKey)
            Boolean lastEntry = (!v.fallbackKey)
            Map secondBinding = [
                buildtool: v,
                lastEntry: lastEntry,
                nextFile: nextFile,
                onlyEntry: onlyEntry
            ]
            documentation << getScriptFromTemplate(templates.'lifecycle', binding + secondBinding).trim()
        }
        documentation.join('\n\n')
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
