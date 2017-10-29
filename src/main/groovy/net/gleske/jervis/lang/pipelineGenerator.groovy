/*
   Copyright 2014-2017 Sam Gleske - https://github.com/samrocketman/jervis

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
    private Map collect_items

    /**
      A list of stash maps collected from the Jervis YAML.
     */
    private List<Map> stashes

    /**
      Instantiates this class with a <tt>{@link lifecycleGenerator}</tt> which
      is used for helper functions when creating a pipeline job designed to
      support Jervis.
     */
    def pipelineGenerator(lifecycleGenerator generator) {
        this.generator = generator
        processCollectItems()
        def stashes = (getObjectValue(generator.jervis_yaml, 'jenkins.stash', []))?: getObjectValue(generator.jervis_yaml, 'jenkins.stash', [:])
        this.stashes = (stashes instanceof List)? stashes : [stashes]
        if(!generator.isMatrixBuild()) {
            //append the items to collect to the end of the list of stashes (overrides prior entries)
            this.stashes += this.collect_items.collect { k, v ->
                [name: k, includes: v]
            }
        }
    }

    /**
      Process each value from a Jenkins collect items map so that it supports multiple types.
     */
    private String processCollectValue(def v) {
		if(v in List) {
			v.join(',')
		}
		else if(v in Map) {
			processCollectValue((v['path'])?: '')
		}
		else {
			v
		}
    }

    /**
      Processes jenkins.collect items from Jervis YAML.
     */
    void processCollectItems() {
        this.collect_items = getObjectValue(generator.jervis_yaml, 'jenkins.collect', [:]).collect { k, v ->
            try {
                [(k): processCollectValue(v)]
            }
            catch(StackOverflowError e) {
                throw new PipelineGeneratorException("Infinite loop error in YAML key: jenkins.collect.${k}\n\nAre you using YAML anchors and aliases and accidentally circled a loop?")
            }
        }.sum()
    }

    /**
      Returns a list of maps which are buildable matrices in a matrix build.  This
      method takes into account that there are matrix exclusions and white lists in
      the YAML configuration.
     */
    List getBuildableMatrixAxes() {
        List matrix_axis_maps = generator.yaml_matrix_axes.collect { axis ->
            //the following map hack is so the key is not an instance of GStringImpl
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
                stash_map[getObjectValue(s, 'name', '')] = [
                    'includes': getObjectValue(s, 'includes', ''),
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
        (supported_collections.intersect(known_items) as List).sort()
    }

    /**
      Get a publishable item from the list of publishable items.  The end
      result could be a String or a Map.
     */
    def getPublishable(String item) {
        collect_items[item]
    }
}
