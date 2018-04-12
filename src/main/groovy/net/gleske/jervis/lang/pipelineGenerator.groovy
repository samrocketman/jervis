/*
   Copyright 2014-2018 Sam Gleske - https://github.com/samrocketman/jervis

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

import java.util.regex.Pattern
import net.gleske.jervis.exceptions.PipelineGeneratorException
import static net.gleske.jervis.lang.lifecycleGenerator.getObjectValue

/**
  This class offers helper forunctions for using Jervis in the context of a
  Jenkins <a href="https://jenkins.io/doc/book/pipeline/shared-libraries/" target=_blank>pipeline global shared library</a>.

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><tt>import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.lang.pipelineGenerator

def generator = new lifecycleGenerator()
generator.loadLifecyclesString(new File('resources/lifecycles-ubuntu1604-stable.json').text)
generator.loadToolchainsString(new File('resources/toolchains-ubuntu1604-stable.json').text)

generator.loadYamlString('''
language: groovy
env: ["GROOVY_VERSION=1.8.9", "GROOVY_VERSION=2.4.12"]
jdk:
  - openjdk8
  - openjdk9
jenkins:
  stash:
    - name: artifacts
      allow_empty: true
      includes: build/lib/*.jar
      matrix_axis:
        env: GROOVY_VERSION=2.4.12
        jdk: openjdk8
  collect:
    artifacts: build/lib/*.jar
'''.trim())
def pipeline_generator = new pipelineGenerator(generator)
pipeline_generator.supported_collections = ['artifacts']
pipeline_generator.getBuildableMatrixAxes().each { axis ->
    if(pipeline_generator.getStashMap(axis)) {
        println "stash ${axis}  --->  ${pipeline_generator.getStashMap(axis)}"
    }
}
println "Buildable matrices: " + pipeline_generator.getBuildableMatrixAxes().size()</tt></pre>
 */
class pipelineGenerator implements Serializable {

    /**
      A lifecycle generator which has already been instantiated and processed
      lifecycle, toolchains, and platforms JSON as well as Jervis YAML.
     */
    private def generator

    /**
      A set of collections which a global pipeline library might support.
      Examples of collections include: cobertura reports, artifacts, or junit
      tests.
     */
    Set<String> supported_collections

    /**
      A list of items to collect from the build.
     */
    private Map collect_items = [:]

    /**
      A list of stash maps collected from the Jervis YAML.
     */
    private List<Map> stashes

    /**
      This is a <tt>Map</tt> of default settings for the YAML key
      <tt>jenkins.collect</tt>.  Key names in this map are similar to the keys
      in <tt>jenkins.collect</tt> if specifying default settings for a
      <tt>jenkins.collect</tt> item.  By a collect item being set with defaults
      we are stating that the settings should be defined by the user and if not
      the default value from this map is selected.  This allows providing more
      advanced options to users but allowing sane defaults to be defined if a
      user chooses not to define more advanced options in YAML.
     */
    Map collect_settings_defaults = [:]

    /**
      This is a <tt>Map</tt> of validation for settings defined in the YAML key
      <tt>jenkins.collect</tt>.  Sometimes, when specifying plugins validation
      of values is required.  Validation of values can take the form of a list
      of acceptable regex patterns or a single regex pattern to validate what
      the user defined.  Some settings, an admin may desire, to limit user
      input if it's a String value.  Validation can only be defined for
      Strings.
     */
    Map collect_settings_validation = [:]

    /**
      Some settings defined in <tt>{@link #collect_settings_defaults}</tt> support
      <a href="http://ant.apache.org/manual/Types/fileset.html" target=_blank>Ant filesets</a>
      and this <tt>Map</tt> adds support for those settings.
     */
    Map collect_settings_filesets = [:]

    /**
      Customize the processing of stashmaps for stash names.  A stashmap
      preprocessor can be used to customize how stashing is calculated for
      custom publishers.  For example, this is necessary for publishers.  All
      keys must be a String.  All values must be a Closure which takes a single
      argument that is a Map.  The following is an example.

      <pre><tt>import net.gleske.jervis.lang.lifecycleGenerator
import net.gleske.jervis.lang.pipelineGenerator

String yaml = '''
language: groovy
jenkins:
  collect:
    html: build/docs/groovydoc
'''.trim()

def generator = new lifecycleGenerator()
generator.loadLifecyclesString(new File('resources/lifecycles-ubuntu1604-stable.json').text)
generator.loadToolchainsString(new File('resources/toolchains-ubuntu1604-stable.json').text)

generator.loadYamlString(yaml)
def pipeline_generator = new pipelineGenerator(generator)
pipeline_generator.supported_collections = ['html']
pipeline_generator.collect_settings_filesets = [html: ['includes']]
pipeline_generator.collect_settings_defaults = [html: [includes: 'foo']]
pipeline_generator.collect_settings_validation = [html: [path: '''^[^,\\:*?"'<>|]+$''']]
pipeline_generator.stashmap_preprocessor = [
    html: { Map settings ->
      settings['includes']?.tokenize(',').collect {
           "${settings['path']  -~ '/$' -~ '^/'}/${it}"
        }.join(',').toString()
    }
]

//should return "build/docs/groovydoc/foo"
pipeline_generator.stashMap['html']['includes']</tt></pre>
     */
    Map stashmap_preprocessor = [:]

    /**
      This filter ensures an admin only sets proper closures for the
      <tt>{@link #stashmap_processor}</tt>.  A <tt>stashmap_processor</tt> is
      for <tt>jenkins.collect</tt> items in user YAML.  Sometimes a publisher
      needs to customize how it stashes files.  This preprocessor allows an
      admin how a
      <a href="https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#code-stash-code-stash-some-files-to-be-used-later-in-the-build">stash "includes" file pattern</a>
      is determined from the settings of an item.
     */
    void setStashmap_preprocessor(Map m) {
        //Stashmap processors are required to take only a single argument and return a String
        stashmap_preprocessor = m.findAll { k, v ->
            (k in String) &&
            (v in Closure) &&
            v.maximumNumberOfParameters == 1 &&
            (Map in v.parameterTypes)
        }
    }


    /**
      This holds the user defined jenkins.collect item maps so we don't have to reference them.
     */
    private Map user_defined_collect_settings = [:]

    /**
      Instantiates this class with a <tt>{@link lifecycleGenerator}</tt> which
      is used for helper functions when creating a pipeline job designed to
      support Jervis.
     */
    def pipelineGenerator(lifecycleGenerator generator) {
        this.generator = generator
        def stashes = (getObjectValue(generator.jervis_yaml, 'jenkins.stash', []))?: getObjectValue(generator.jervis_yaml, 'jenkins.stash', [:])
        this.stashes = (stashes instanceof List)? stashes : [stashes]
        processCollectItems()
    }

    /**
      This method merges <tt>Map m</tt> with the existing map <tt>{@link #collect_settings_defaults}</tt>.
     */
    void setCollect_settings_defaults(Map m) {
        Map tmp = m.findAll { k, v -> k && v instanceof Map && v }
        if(tmp) {
            this.collect_settings_defaults << tmp
        }
    }

    /**
      Process each value from a Jenkins collect items map so that it supports multiple types.
     */
    private String processCollectValue(def v, String k) {
        if(v in List) {
            v.join(',')
        }
        else if(v in Map) {
            try {
                processCollectValue((v['path'])?: '', k)
            }
            catch(StackOverflowError e) {
                throw new PipelineGeneratorException("Infinite loop error in YAML key: jenkins.collect.${k}\n\nAre you using YAML anchors and aliases and accidentally circled a loop?")
            }
        }
        else {
            v
        }
    }

    /**
      Processes <tt>jenkins.collect</tt> items from Jervis YAML.
     */
    private void processCollectItems() {
        Map tmp = getObjectValue(generator.jervis_yaml, 'jenkins.collect', [:])
        if(tmp) {
            this.collect_items = tmp.collect { k, v ->
                if(v in Map) {
                    user_defined_collect_settings[k] = v
                }
                [(k): processCollectValue(v, k)]
            }.sum().findAll { k, v -> k && v }
        }

        if(!generator.isMatrixBuild()) {
            //append the items to collect to the end of the list of stashes (overrides prior entries)
            this.stashes += this.collect_items.collect { k, v ->
                [name: k, includes: v]
                /*
                if((k in stashmap_preprocessor) && (getPublishable(k) in Map)) {
                    if(!(value in String)) {
                        throw new PipelineGeneratorException("stashmap_preprocessor for collect item '${k}' must return a String but does not.  This issue can only be resolved by an admin of the pipeline shared library.")
                    }
                    println "value is ${value}"
                    return [
                        name: k,
                        includes: value,
                    ]
                }
                else {
                    return [name: k, includes: v]
                }*/
            }
        }
    }

    /**
      Returns a list of maps which are buildable matrices in a matrix build.  This
      method takes into account that there are matrix exclusions and white lists in
      the YAML configuration.
     */
    List getBuildableMatrixAxes() {
        List matrix_axis_maps = generator.yaml_matrix_axes.collect { axis ->
            generator.matrixGetAxisValue(axis).split().collect {
                [(axis): it]
            }
        }
        if(generator.yaml_matrix_axes.size() < 2) {
            matrix_axis_maps = matrix_axis_maps[0]
        }
        else {
            //creates a list of lists which contain maps to be summed into one list of maps with every possible matrix combination
            //create a groovy cartesian product of the maps and then sum each list of maps together
            matrix_axis_maps = matrix_axis_maps.combinations()*.sum()
        }
        //return all maps (or some maps allowed via filter)
        matrix_axis_maps.findAll {
            if(generator.matrixExcludeFilter()) {
                Binding binding = new Binding()
                it.each { k, v ->
                    binding.setVariable(k, v)
                }
                //filter out the combinations (returns a boolean true or false)
                new GroovyShell(binding).evaluate(generator.matrixExcludeFilter())
            }
            else {
                //if there's no matrix exclude filter then include everything
                true
            }
        }
    }

    /**
      Returns a list of stashes from Jervis YAML to be stashed either serially or
      in this matrix axis for matrix builds.

     */
    Map getStashMap(Map matrix_axis = [:]) {
        boolean isMatrix = generator.isMatrixBuild()
        Map stash_map = [:]
        stashes.each { s ->
            if((s instanceof Map) &&
                    ('name' in s) &&
                    getObjectValue(collect_items, s['name'], '')) {
                s['includes'] = getObjectValue(collect_items, s['name'], '')
            }
            if((s instanceof Map) &&
                    ('name' in s) &&
                    getObjectValue(s, 'name', '') &&
                    ('includes' in s) &&
                    getObjectValue(s, 'includes', '') &&
                    (!isMatrix || getObjectValue(s, 'matrix_axis', [:])) &&
                    (!isMatrix || (getObjectValue(s, 'matrix_axis', [:]) == convertMatrixAxis(matrix_axis)))) {
                    String name = getObjectValue(s, 'name', '')
                    String includes = getObjectValue(s, 'includes', '')
                    if((name in stashmap_preprocessor) && (getPublishable(name) in Map)) {
                        def result
                        try {
                            result = stashmap_preprocessor[name](getPublishable(name))
                        }
                        catch(Exception e) {
                            throw new PipelineGeneratorException("stashmap_preprocessor for collect item '${name}' must return a String but does not.  This issue can only be resolved by an admin of the pipeline shared library.\nSTART Preprocessor Exception:\n${e.toString()}\n    ${e.getStackTrace()*.toString().join('\n    ')}\n\nEND Preprocessor Exception")
                        }
                        if(!(result in String)) {
                            throw new PipelineGeneratorException("stashmap_preprocessor for collect item '${name}' must return a String but does not.  This issue can only be resolved by an admin of the pipeline shared library.")
                        }
                        includes = result
                    }
                stash_map[name] = [
                    'includes': includes,
                    'excludes': getObjectValue(s, 'excludes', ''),
                    'use_default_excludes': getObjectValue(s, 'use_default_excludes', true),
                    'allow_empty': getObjectValue(s, 'allow_empty', false),
                    'matrix_axis': getObjectValue(s, 'matrix_axis', [:])
                    ]
            }
        }
        stash_map
    }

    /**
      Get a default 'includes' string for a matrix build.  If the includes is
      missing then it should get it from the publishable item.

    private String getDefaultIncludes(Map stash) {
        ''
    }
     */

    /**
      Convert a matrix axis to use unfriendly names for stash comparison.
     */
    private Map convertMatrixAxis(Map matrix_axis) {
        Map new_axis = [:]
        matrix_axis.each { k, v ->
            new_axis[k] = (generator.matrix_fullName_by_friendly[v]?:v) - ~/^${k}:/
        }
        new_axis
    }

    /**
      Processes secret properties from <tt>.jervis.yml</tt> into two lists.
      The first list of maps is meant to be passed to
      <tt>MaskPasswordsBuildWrapper</tt> for masking password output.  The
      second list contains a list of strings meant to be passed as an argument
      to <tt>withEnv</tt> to inject environment variables into a build runtime.
      The combination of the two allows one to inject secret variables into a
      build runtime but mask the output of a password in the build output of
      the Jenkins job.  Used by <tt>withEnvSecretWrapper()</tt> method.
     */
    List getSecretPairsEnv() {
        List<Map> secretPairs = []
        List<String> secretEnv = []
        generator.plainmap.each { k, v ->
            secretPairs << [var: k, password: v]
            secretEnv << "${k}=${v}"
        }
        //return a list of lists
        [secretPairs, secretEnv]
    }

    /**
      Get a list of publishable items which show up in <tt>.jervis.yml</tt>.
      This is determined from the known items in Jervis YAML
      <tt>jenkins.collect</tt> items.
     */
    List getPublishableItems() {
        Set known_items = collect_items.keySet() as Set
        if(!supported_collections) {
            throw new PipelineGeneratorException('Calling getPublishableItems() without setting supported_collections.  This issue can only be resolved by an admin of the pipeline shared library.')
        }
        (supported_collections.intersect(known_items) as List).sort()
        //(supported_collections.intersect(known_items) as List).findAll { getPubishable(it) as Boolean }.sort()
    }

    /**
      Check to see if user input is valid when a customized collction is defined.

      @param item The "jenkins &gt; collect" YAML key to check; e.g.
                  <tt>artifact</tt>.
      @param setting The setting of the collected item to validate against.
      @param input   User input defined by an end user in their Jervis YAML.
      @return        Returns <tt>true</tt> if an admin hasn't defined any
                     validation.  Returns <tt>true</tt> if an admin defined a
                     validation method and the user passed validation.  Returns
                     <tt>false</tt> if the admin defined input validation and
                     the user input failed to pass it.
      */
    private boolean isCollectUserInputValid(String item, String setting, def input) {
        if((item in collect_settings_validation) && (setting in collect_settings_validation[item])) {
            def validator = collect_settings_validation[item][setting]
            if((!(validator in  String) && !(validator in List)) ||
                    ((validator in List) && (false in validator.collect { it in String }))) {
                throw new PipelineGeneratorException("Global shared pipeline library Admin did not properly define collect_settings_validation for key ${item}.${setting}.  It must be a String or List of Strings.  This is an invalid configuration in the global shared pipeline library in collect_settings_validation.")
            }
            if(!(input in String)) {
                throw new PipelineGeneratorException("Global pipeline library Admin has not properly matched validation for key ${item}.${setting}.  Admin is attempting to validate a type that isn't a String.  This is an invalid configuration in the global shared pipeline library in collect_settings_validation.")
            }
            //Admin has properly defined settings so let's proceed with validating the value provided by the user.
            String regex = (validator in List)? validator.join('|') : validator
            Pattern.compile(regex).matcher(input).matches()
        }
        else {
            true
        }
    }

    /**
      Get a publishable item from the list of publishable items.  The end
      result could be a String or a Map.

      @param item A key from <tt>jenkins.collect</tt> in user defined YAML.

      @return Returns a <tt>{@link Map}</tt> or a <tt>{@link String}</tt> of
              settings for the publishable item from <tt>jenkins.collect</tt>
              YAML key.
     */
    def getPublishable(String item) {
        String path = (collect_items[item])?: ''
        if(item in collect_settings_defaults) {
            Map tmp = collect_settings_defaults[item].collect { k, v ->
                def setting = getObjectValue((user_defined_collect_settings[item])?: [:], k, v)
                if(item in collect_settings_filesets && k in collect_settings_filesets[item]) {
                    if(k in (user_defined_collect_settings[item]?: [:])) {
                        setting = processCollectValue(getObjectValue((user_defined_collect_settings[item])?: [:], k, new Object()), k)
                    }
                    if(!setting) {
                        setting = v
                    }
                }
                //check if user input matches admin required format (if any)
                if(!isCollectUserInputValid(item, k, setting)) {
                    setting = v
                }
                [(k): setting]
            }.sum()
            tmp << ['path': path]
            return isCollectUserInputValid(item, 'path', path)? tmp : [:]
        }
        else {
            return path
        }
    }
}
