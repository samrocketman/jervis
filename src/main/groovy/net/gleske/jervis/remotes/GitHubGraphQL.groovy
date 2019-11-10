/*
   Copyright 2014-2019 Sam Gleske - https://github.com/samrocketman/jervis

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
package net.gleske.jervis.remotes

import groovy.json.JsonBuilder
import net.gleske.jervis.remotes.interfaces.TokenCredential
import static net.gleske.jervis.tools.AutoRelease.getScriptFromTemplate

/**
   A simple class to interact with the GitHub v4 API.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a> with the classpath set up.</p>
   <p>For more GraphQL samples see <a href="https://github.com/samrocketman/jervis/issues/133" target="_blank">Jervis issue number 133</a>.</p>

<pre><tt>import net.gleske.jervis.remotes.GitHubGraphQL

GitHubGraphQL github = new GitHubGraphQL()
github.token = 'GitHub personal access token here with no scopes necessary'
String graphql = '''
query {
  repository(owner: "samrocketman", name: "jervis") {
    jervisYaml:object(expression: "master:.jervis.yml") {
      ...file
    }
    travisYaml:object(expression: "master:.travis.yml") {
      ...file
    }
    rootFolder:object(expression: "master:") {
      ...file
    }
  }
}
fragment file on GitObject {
  ... on Blob {
    text
  }
  ... on Tree {
    file:entries {
      name
    }
  }
}
'''

// Make an API call to GitHub
Map response = github.sendGQL(graphql)

// Process the data returned from GitHub
if(['.jervis.yml', '.travis.yml'].intersect((response.data.repository.rootFolder?.file*.name) ?: [])) {
    println response.data.repository.with {
        it.jervisYaml?.text ?: it.travisYaml?.text
    }
}</pre></tt>
 */
class GitHubGraphQL implements SimpleRestServiceSupport {

    private static final String DEFAULT_URL = 'https://api.github.com/graphql'

    private static final graphql_expr_template = '''
        |query {
        |    <% gitRefs.eachWithIndex { String gitRef, refIndex -> %>gitRef${refIndex}: repository(owner: "${repoOwner}", name: "${repo}") {
        |        <% yamlFiles.eachWithIndex { yamlFileName, fileIndex -> %>jervisYaml${fileIndex}:object(expression: "${gitRef}:${yamlFileName}") {
        |            ...file
        |        }
        |        <% } %>
        |        rootFolder:object(expression: "${gitRef}:") {
        |          ...file
        |        }
        |    }
        |<% } %>
        |}
        |fragment file on GitObject {
        |    ... on Blob {
        |        text
        |    }
        |    ... on Tree {
        |        file:entries {
        |            name
        |            type
        |        }
        |    }
        |}
        '''.stripMargin().trim()

    @Override
    String baseUrl() {
        this.gh_api
    }

    @Override
    Map header(Map http_headers = [:]) {
        if(this.getToken()) {
            http_headers['Authorization'] = "bearer ${this.getToken()}".toString()
        }
        http_headers
    }

    /**
      URL to the <a href="https://developer.github.com/v4/" target="_blank">GitHub v4 API</a>.  Default: <tt>https://api.github.com/</tt>
     */
    String gh_api = DEFAULT_URL

    /**
      The <a href="https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line" target="_blank">API token</a>, which can be used to communicate with GitHub using authentication.  Default: <tt>null</tt>
     */
    String token

    /**
       A credential for interacting with an external credential store.  If this
       is defined or set, then <tt>{@link #token}</tt> is ignored and not used.
       Default: <tt>null</tt>
      */
    TokenCredential credential

    /**
       Retrieves the token used to authenticate with GitHub.  If
       <tt>{@link #credential}</tt> is set, then this will get the credential
       token, instead of <tt>{@link #token}</tt>.

       @return A personal access token or an OAuth access token typically.
      */
    String getToken() {
        (this.credential) ? this.credential.getToken() : this.token
    }

    /**
       Sets the token to be used by GitHub.  If <tt>{@link #credential}</tt> is
       set then this will set the credential token, instead of
       <tt>{@link #token}</tt>.

       @param token A personal access token or an OAuth access token typically.
      */
    void setToken(String token) {
        if(this.credential) {
            this.credential.setToken(token)
        }
        else {
            this.token = token
        }
    }

    String getGqlData(String query, String variables = '') {
        Map data = [ query: query ]
        if(variables) {
            data['variables'] = variables
        }
        (data as JsonBuilder).toString()
    }

    public def sendGQL(String graphql, String variables = '', String http_method = 'POST', Map http_headers = [:]) {
        println graphql
        apiFetch('', http_headers, http_method, getGqlData(graphql, variables))
    }

    /**
      Get Jervis YAML from a remote repository.  It supports getting YAML from
      multiple branches at once (<tt>gitRefs</tt> and from multiple alternate
      YAML file locations (<tt>yamlFiles</tt>).

      @param owner      GitHub repository owner such as an Organization or User.
                        e.g. GitHub user
                        <a href="https://github.com/samrocketman" target="_blank"><tt>samrocketman</tt></a>.
      @param repository The name of the repository.  e.g.
                        <a href="https://github.com/samrocketman/jervis" target="_blank"><tt>jervis</tt></a>.
      @param gitRefs    A list of get references (branches, tags, commits, etc)
                        in order to retrieve files.  By default, the value
                        <tt>['refs/heads/master']</tt>.
      @param yamlFiles  A list of YAML files to try getting the Jervis YAML
                        contents from.  By default, the value is
                        <tt>['.jervis.yml', '.travis.yml']</tt>.

      @return Returns a fully formed graphql response.  The following is an
              example when calling with defaults.

              <p>Call with default arguments</p>
<pre><tt>import net.gleske.jervis.remotes.GitHubGraphQL

GitHubGraphQL github = new GitHubGraphQL()
github.token = new File('../github_token').text.trim()


// Make an API call to GitHub
Map response = github.getJervisYamlFiles('samrocketman', 'jervis')</tt></pre>
              <p>Responds with parsed data</p>
<pre><tt>[
    gitRef0: [
        jervisYaml0: null,
        jervisYaml1: [
            text: 'language: groovy\n'
        ]
    ]
]</tt></pre>
              <p>The above response indicates there was no
              <tt>.jervis.yml</tt>, but there was a <tt>.travis.yml</tt>
              file.</p>
      */
    public Map getJervisYamlFiles(String owner,
            String repository,
            List gitRefs = ['refs/heads/master'],
            List yamlFiles = ['.jervis.yml', '.travis.yml']) {

        Map binding = [
            repoOwner: owner,
            repo: repository,
            gitRefs: gitRefs,
            yamlFiles: yamlFiles
        ]
        sendGQL(getScriptFromTemplate(graphql_expr_template, binding))?.get('data') ?: [:]
    }
}
