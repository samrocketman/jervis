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
package net.gleske.jervis.remotes

import groovy.json.JsonBuilder
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.exceptions.JervisException
import static net.gleske.jervis.tools.AutoRelease.getScriptFromTemplate

/**
   A simple class to interact with the GitHub v4 GraphQL API.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a> with the classpath set up.</p>
   <p>For more GraphQL samples see <a href="https://github.com/samrocketman/jervis/issues/133" target="_blank">Jervis issue number 133</a>.</p>

<pre><code>
import net.gleske.jervis.remotes.GitHubGraphQL

GitHubGraphQL github = new GitHubGraphQL()
github.token = 'GitHub personal access token here with no scopes necessary'
String graphql = '''
query {
  repository(owner: "samrocketman", name: "jervis") {
    jervisYaml:object(expression: "main:.jervis.yml") {
      ...file
    }
    travisYaml:object(expression: "main:.travis.yml") {
      ...file
    }
    rootFolder:object(expression: "main:") {
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

    /**
      A method which returns the base URL for the GitHub v4 GraphQL API.  This
      method is not meant to be called by end users.

      @return Returns <tt>{@link #gh_api}</tt>.
      */
    @Override
    String baseUrl() {
        this.gh_api
    }

    /**
      A method which sets authentication headers.  This method is not meant to
      be called by end users.

      @return Returns HTTP header map with authentication headers set.
      */
    @Override
    Map header(Map http_headers = [:]) {
        if(this.getToken()) {
            http_headers['Authorization'] = "bearer ${this.getToken()}".toString()
        }
        http_headers
    }

    /**
      URL to the <a href="https://developer.github.com/v4/" target="_blank">GitHub v4 GraphQL API</a>.
      Default: <tt>https://api.github.com/graphql</tt>
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

    /**
      Transforms a GraphQL query and variables into data which can be submitted
      with a POST request.  This is used by <tt>sendGQL</tt> to submit
      transformed GraphQL to the GitHub API v4 GraphQL.

      @param query     A GraphQL query.
      @param variables GraphQL variables meant to be used by a GraphQL query.

      @return Stringified data which can be submitted directly to a remote HTTP
              service that accepts GraphQL.
      */
    String getGqlData(String query, String variables = '') {
        Map data = [ query: query ]
        if(variables) {
            data['variables'] = variables
        }
        (data as JsonBuilder).toString()
    }

    /**
      A method for calling the GitHub v4 GraphQL API with a GraphQL query and
      variables.

      @param graphql      A GraphQL query.
      @param variables    GraphQL variables meant to be used by a GraphQL query.
      @param http_method  Customize the method to be submitted to the GitHub
                          GraphQL API.  Typically <tt>POST</tt>, but could be
                          another method if performing a mutation.
      @param http_headers Add custom HTTP headers.  This does not normally need
                          to be called but is available for customization.
      @return A parsed response from the GitHub v4 GraphQL API.
      */
    public def sendGQL(String graphql, String variables = '', String http_method = 'POST', Map http_headers = [:]) {
        apiFetch('', http_headers, http_method, getGqlData(graphql, variables))
    }

    /**
      A method for calling the GitHub v4 GraphQL API with a GraphQL query and
      variables.

      @param graphql      A GraphQL query.
      @param variables    GraphQL variables meant to be used by a GraphQL query.
      @param http_method  Customize the method to be submitted to the GitHub
                          GraphQL API.  Typically <tt>POST</tt>, but could be
                          another method if performing a mutation.
      @param http_headers Add custom HTTP headers.  This does not normally need
                          to be called but is available for customization.
      @return A parsed response from the GitHub v4 GraphQL API.
      */
    public def sendGQL(String graphql, Map variables, String http_method = 'POST', Map http_headers = [:]) {
        sendGQL(graphql, (variables as JsonBuilder).toString(), http_method, http_headers)
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
                        <tt>['refs/heads/main']</tt>.
      @param yamlFiles  A list of YAML files to try getting the Jervis YAML
                        contents from.  By default, the value is
                        <tt>['.jervis.yml', '.travis.yml']</tt>.

      @return Returns a fully formed graphql response.  The following is an
              example when calling with defaults.

              <p>Call with default arguments</p>
<pre><code>
import net.gleske.jervis.remotes.GitHubGraphQL

GitHubGraphQL github = new GitHubGraphQL()
github.token = new File('../github_token').text.trim()


// Make an API call to GitHub
Map response = github.getJervisYamlFiles('samrocketman', 'jervis')
</code></pre>
              <p>Responds with parsed data</p>
<pre><code>
[
    gitRef0: [
        jervisYaml0: null,
        jervisYaml1: [
            text: 'language: groovy\n'
        ]
        rootFolder: [
            file: [
                [
                    name: ".travis.yml",
                    type: "blob"
                ],
                [
                    name: "src",
                    type: "tree"
                ]
            ]
        ]
    ]
]
</code></pre>
              <p>The above response indicates there was no
              <tt>.jervis.yml</tt>, but there was a <tt>.travis.yml</tt>
              file.</p>
              <p>Files returned are typically one of three types:</p>
              <ul>
                  <li><tt>blob</tt> - A file which has contents.</li>
                  <li><tt>tree</tt> - A folder which can be recursed into.</li>
                  <li><tt>commit</tt> - A Git submodule to a referenced repository.</li>
              </ul>
      */
    public Map getJervisYamlFiles(String owner,
            String repository,
            List gitRefs = ['refs/heads/main'],
            List yamlFiles = ['.jervis.yml', '.travis.yml']) {

        Map binding = [
            repoOwner: owner,
            repo: repository,
            gitRefs: gitRefs,
            yamlFiles: yamlFiles
        ]
        sendGQL(getScriptFromTemplate(graphql_expr_template, binding))?.get('data') ?: [:]
    }

    /**
      Get Jervis YAML from a remote repository.  It supports getting YAML from
      multiple branches at once (<tt>gitRefs</tt> and from multiple alternate
      YAML file locations (<tt>yamlFiles</tt>).

      @param repositoryWithOwner
                       A repository URL which includes the GitHub owner.  e.g.
                       <tt>samrocketman/jervis</tt>.
      @param gitRefs   A list of get references (branches, tags, commits, etc)
                       in order to retrieve files.  By default, the value
                       <tt>['refs/heads/main']</tt>.
      @param yamlFiles A list of YAML files to try getting the Jervis YAML
                       contents from.  By default, the value is
                       <tt>['.jervis.yml', '.travis.yml']</tt>.
      @return See other <a href="#getJervisYamlFiles(java.lang.String, java.lang.String, java.util.List, java.util.List)"><tt>getJervisYamlFiles</tt></a>
              which returns the same thing.  This method is just overloading
              the other.
      */
    public Map getJervisYamlFiles(String repositoryWithOwner,
            List gitRefs = ['refs/heads/main'],
            List yamlFiles = ['.jervis.yml', '.travis.yml']) {
        if(!repositoryWithOwner.contains('/') || (repositoryWithOwner.tokenize('/').size() > 2)) {
            throw new JervisException("ERROR: getJervisYamlFiles recieved a malformated repositoryWithOwner ${repositoryWithOwner}.")
        }
        repositoryWithOwner.tokenize('/').with {
            getJervisYamlFiles(it[0], it[1], gitRefs, yamlFiles)
        }
    }
}
