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

    @Override
    String baseUrl() {
        this.gh_api
    }

    @Override
    Map header(Map http_headers = [:]) {
        if(this.token) {
            http_headers['Authorization'] = "bearer ${this.token}".toString()
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

    String getGqlData(String query, String variables = '') {
        Map data = [ query: query ]
        if(variables) {
            data['variables'] = variables
        }
        (data as JsonBuilder).toString()
    }

    public def sendGQL(String graphql, String variables = '', String http_method = 'POST', Map http_headers = [:]) {
        apiFetch('', http_headers, http_method, getGqlData(graphql, variables))
    }
}
