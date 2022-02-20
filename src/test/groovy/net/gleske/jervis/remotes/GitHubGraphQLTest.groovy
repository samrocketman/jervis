/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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
//the GitHubGraphQLTest() class automatically sees the GitHub() class because they're in the same package
import net.gleske.jervis.exceptions.JervisException
import org.junit.After
import org.junit.Before
import org.junit.Test
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

class GitHubGraphQLTest extends GroovyTestCase {
    def mygh
    def url
    Map request_meta = [:]

    //set up before every test
    @Before protected void setUp() {
        super.setUp()
        // mock GraphQL endpoint but reference local mocks by SHA-256 checksum
        // from the GraphQL query payload.
        mockStaticUrl(url, URL, request_meta, true, 'SHA-256')
        mygh = new GitHubGraphQL()
    }
    //tear down after every test
    @After protected void tearDown() {
        mygh = null
        request_meta = [:]
        super.tearDown()
    }
    //test GitHubGraphQL().sendGQL()
    @Test public void test_GitHubGraphQL_sendGQL() {
        String graphql = '''
            |query {
            |  repository(owner: "samrocketman", name: "jervis") {
            |    jervisYaml:object(expression: "master:.jervis.yml") {
            |      ...file
            |    }
            |    travisYaml:object(expression: "master:.travis.yml") {
            |      ...file
            |    }
            |    rootFolder:object(expression: "master:") {
            |      ...file
            |    }
            |  }
            |}
            |fragment file on GitObject {
            |  ... on Blob {
            |    text
            |  }
            |  ... on Tree {
            |    file:entries {
            |      name
            |    }
            |  }
            |}
            '''.stripMargin()
        mygh.token = 'foo'
        Map response = mygh.sendGQL(graphql)
        assert 'language: groovy' == response.data.repository.travisYaml.text
        assert ['.travis.yml'] == ['.jervis.yml', '.travis.yml'].intersect(response.data.repository.rootFolder?.file*.name)
        assert request_meta['headers']['Authorization'] == 'bearer foo'
        assert request_meta['method'] == 'POST'
    }
    @Test public void test_GitHubGraphQL_sendGQL_custom() {
        mygh.sendGQL('query { foo }')
        assert request_meta['data'].toString() == '{"query":"query { foo }"}'
    }
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_quotes() {
        mygh.sendGQL('query { foo(expr: "hello") }')
        assert request_meta['data'].toString() == '{"query":"query { foo(expr: \\"hello\\") }"}'
    }
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_variables() {
        mygh.sendGQL('query { foo }', 'variables { bar }')
        assert request_meta['data'].toString() == '{"query":"query { foo }","variables":"variables { bar }"}'
    }
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_variables_Map() {
        Map variables = [ myvar: 3 ]
        mygh.sendGQL('query { foo(expr: "hello") }', variables)
        assert request_meta['data'].toString() == '{"query":"query { foo(expr: \\"hello\\") }","variables":"{\\"myvar\\":3}"}'
    }
    @Test public void test_GitHubGraphQL_credentials_read() {
        mygh.credential = new CredentialsInterfaceHelper.ROCreds()
        assert mygh.token == 'ro secret'
        mygh.sendGQL('stuff')
        assert request_meta['headers']['Authorization'] == 'bearer ro secret'
        mygh.token = 'foo'
        assert mygh.token == 'ro secret'
        mygh.sendGQL('stuff')
        assert request_meta['headers']['Authorization'] == 'bearer ro secret'
    }
    @Test public void test_GitHubGraphQL_credentials_write() {
        mygh.credential = new CredentialsInterfaceHelper.RWCreds()
        assert mygh.token == 'rw secret'
        mygh.sendGQL('stuff')
        assert request_meta['headers']['Authorization'] == 'bearer rw secret'
        mygh.token = 'foo'
        assert mygh.token == 'foo'
        mygh.sendGQL('stuff')
        assert request_meta['headers']['Authorization'] == 'bearer foo'
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock response data'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman', 'jervis')
        assert shouldRespond.keySet() == response.keySet()
        assert shouldRespond['gitRef0'].keySet() == response['gitRef0'].keySet()
        assert shouldRespond['gitRef0']['jervisYaml0'] == response['gitRef0']['jervisYaml0']
        assert shouldRespond['gitRef0']['jervisYaml1'] == response['gitRef0']['jervisYaml1']
        assert shouldRespond['gitRef0']['rootFolder']['file']*.name == response['gitRef0']['rootFolder']['file']*.name
        assert shouldRespond['gitRef0']['rootFolder']['file']*.type == response['gitRef0']['rootFolder']['file']*.type
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_two_branches_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 1'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]], 'gitRef1':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 2'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'CHANGELOG.md', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman', 'jervis', ['refs/heads/master', 'refs/heads/jervis_simple'])
        assert shouldRespond['gitRef0'].keySet() == response['gitRef0'].keySet()
        assert shouldRespond['gitRef0']['jervisYaml0'] == response['gitRef0']['jervisYaml0']
        assert shouldRespond['gitRef0']['jervisYaml1'] == response['gitRef0']['jervisYaml1']
        assert shouldRespond['gitRef0']['rootFolder']['file']*.name == response['gitRef0']['rootFolder']['file']*.name
        assert shouldRespond['gitRef0']['rootFolder']['file']*.type == response['gitRef0']['rootFolder']['file']*.type
        assert shouldRespond['gitRef1'].keySet() == response['gitRef1'].keySet()
        assert shouldRespond['gitRef1']['jervisYaml0'] == response['gitRef1']['jervisYaml0']
        assert shouldRespond['gitRef1']['jervisYaml1'] == response['gitRef1']['jervisYaml1']
        assert shouldRespond['gitRef1']['rootFolder']['file']*.name == response['gitRef1']['rootFolder']['file']*.name
        assert shouldRespond['gitRef1']['rootFolder']['file']*.type == response['gitRef1']['rootFolder']['file']*.type
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_invalid() {
        Map response = mygh.getJervisYamlFiles('invalid', 'invalid')
        assert response instanceof Map
        assert response == [:]
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock response data'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman/jervis')
        assert shouldRespond.keySet() == response.keySet()
        assert shouldRespond['gitRef0'].keySet() == response['gitRef0'].keySet()
        assert shouldRespond['gitRef0']['jervisYaml0'] == response['gitRef0']['jervisYaml0']
        assert shouldRespond['gitRef0']['jervisYaml1'] == response['gitRef0']['jervisYaml1']
        assert shouldRespond['gitRef0']['rootFolder']['file']*.name == response['gitRef0']['rootFolder']['file']*.name
        assert shouldRespond['gitRef0']['rootFolder']['file']*.type == response['gitRef0']['rootFolder']['file']*.type
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_two_branches_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 1'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]], 'gitRef1':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 2'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'CHANGELOG.md', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman/jervis', ['refs/heads/master', 'refs/heads/jervis_simple'])
        assert shouldRespond['gitRef0'].keySet() == response['gitRef0'].keySet()
        assert shouldRespond['gitRef0']['jervisYaml0'] == response['gitRef0']['jervisYaml0']
        assert shouldRespond['gitRef0']['jervisYaml1'] == response['gitRef0']['jervisYaml1']
        assert shouldRespond['gitRef0']['rootFolder']['file']*.name == response['gitRef0']['rootFolder']['file']*.name
        assert shouldRespond['gitRef0']['rootFolder']['file']*.type == response['gitRef0']['rootFolder']['file']*.type
        assert shouldRespond['gitRef1'].keySet() == response['gitRef1'].keySet()
        assert shouldRespond['gitRef1']['jervisYaml0'] == response['gitRef1']['jervisYaml0']
        assert shouldRespond['gitRef1']['jervisYaml1'] == response['gitRef1']['jervisYaml1']
        assert shouldRespond['gitRef1']['rootFolder']['file']*.name == response['gitRef1']['rootFolder']['file']*.name
        assert shouldRespond['gitRef1']['rootFolder']['file']*.type == response['gitRef1']['rootFolder']['file']*.type
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_invalid_apiErr() {
        Map response = mygh.getJervisYamlFiles('invalid/invalid')
        assert response instanceof Map
        assert response == [:]
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_invalid() {
        shouldFail(JervisException) {
            mygh.getJervisYamlFiles('invalid')
        }
    }
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_invalid_tooMany() {
        shouldFail(JervisException) {
            mygh.getJervisYamlFiles('invalid/invalid/invalid')
        }
    }
}
