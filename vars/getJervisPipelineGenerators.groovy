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
/*
   This var takes two parameters: owner and repos.  repos can be a String or
   List.  It builds a HashMap of pipeline generator objects.
 */
import static net.gleske.jervis.tools.YamlOperator.getObjectValue
import static net.gleske.jervis.tools.AutoRelease.getScriptFromTemplate
import net.gleske.jervis.lang.LifecycleGenerator
import net.gleske.jervis.lang.PipelineGenerator
import net.gleske.jervis.remotes.GitHubGraphQL
// https://github.com/jenkinsci/scm-filter-jervis-plugin/blob/master/src/main/groovy/net/gleske/scmfilter/credential/GraphQLTokenCredential.groovy
import net.gleske.scmfilter.credential.GraphQLTokenCredential

/**
  A var which can query an organization for multiple repositories and return a
  HashMap of pipeline generator objects for the queried repositories.

  Sample usage:
      getJervisPipelineGenerators(owner: 'samrocketman', repos: ['repo1', 'repo2'])

    TODO: continue migration to support multiple branches and multiple YAML
          files in those branches.
  */
@NonCPS
def call(Map options) {
    getPipelineGenerators(options)
}

/**
  Get a list of PipelineGenerator objects for one or more projects.

  @param options An option map whichs hould only contain the projects key.  The
                 projects key can be a String or List of Strings.

  @return A List is returned if there's errors containing which projects threw
          the errors.  This is for displaying helpful error messages.  A
          successful query of all projects will return a Map where the key is
          the project name and the value is the PipelineGenerator object.
 */
@NonCPS
def getPipelineGenerators(Map options) {
    List requestedProjects
    List errors
    Map projects = [:]
    Map jsonFiles = [:]
    List refs = options.branches ?: ['refs/heads/main', 'refs/heads/master']
    // TODO: YAML files from scm-filter-jervis plugin
    List jervisYaml = ['.jervis.yaml', '.jervis.yml']
    if(options.projects in String) {
        requestedProjects = [options.repos]
    } else {
        requestedProjects = getObjectValue(options, 'repos', [])
    }
    errors = requestedProjects.findAll { !(it in String) || !(it =~ /^[A-Za-z0-9-_]+$/) }
    requestedProjects = requestedProjects.findAll { (it in String) && (it =~ /^[A-Za-z0-9-_]+$/) }
    String query_template = '''\
        query {
            <% projects.eachWithIndex { repository, index -> %>repo${index}: repository(owner: "${owner}", name: "${repository}") {
                name
                master: object(expression: "refs/heads/master:.jervis.yaml") {
                    ...file
                }
                main: object(expression: "refs/heads/main:.jervis.yaml") {
                    ...file
                }
            }
        <% } %>
        }
        fragment file on GitObject {
            ... on Blob {
                text
            }
        }
        '''.stripMargin().trim()
    def github = new GitHubGraphQL()
    github.credential = new GraphQLTokenCredential(currentBuild.rawBuild.parent, 'github-user-and-token')
    Map response = github.sendGQL(getScriptFromTemplate(query_template, [projects: requestedProjects, owner: options.owner]))
    Map projectYaml = [:]
    response.data.each { k, v ->
        if(!v) {
            // go to next iteration
            return
        }
        String yaml = getObjectValue(v, 'master.text', getObjectValue(v, 'main.text', ''))
        projectYaml[v.name] = yaml
    }
    errors += requestedProjects - projectYaml.keySet().toList()
    if(errors) {
        return errors
    }
    projectYaml.each { String project, String yaml ->
        def generator = new LifecycleGenerator()
        if(!('platforms.json' in jsonFiles)) {
            jsonFiles['platforms.json'] = loadCustomResource('platforms.json')
        }
        generator.loadPlatformsString(jsonFiles['platforms.json'])
        generator.preloadYamlString(yaml)
        String os_stability = "${generator.label_os}-${generator.label_stability}"
        String lifecyclesFile = "lifecycles-${os_stability}.json"
        String toolchainsFile = "toolchains-${os_stability}.json"
        if(!(lifecyclesFile in jsonFiles.keySet())) {
            jsonFiles[lifecyclesFile] = loadCustomResource(lifecyclesFile)
        }
        if(!(toolchainsFile in jsonFiles.keySet())) {
            jsonFiles[toolchainsFile] = loadCustomResource(toolchainsFile)
        }
        generator.loadLifecyclesString(jsonFiles[lifecyclesFile])
        generator.loadToolchainsString(jsonFiles[toolchainsFile])
        generator.loadYamlString(yaml)
        projects[project] = new PipelineGenerator(generator)
    }
    return projects
}
