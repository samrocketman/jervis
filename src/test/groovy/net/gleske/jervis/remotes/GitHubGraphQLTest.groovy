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
//the GitHubGraphQLTest() class automatically sees the GitHub() class because they're in the same package
import static net.gleske.jervis.remotes.StaticMocking.mockStaticUrl

import net.gleske.jervis.exceptions.JervisException

import org.junit.After
import org.junit.Before
import org.junit.Test

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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_4dd006f5da3f86694db573aa66cece0650601e750299f969034e9b07cd2d7512
      */
    @Test public void test_GitHubGraphQL_sendGQL() {
        String graphql = '''
            |query {
            |  repository(owner: "samrocketman", name: "jervis") {
            |    jervisYaml:object(expression: "main:.jervis.yml") {
            |      ...file
            |    }
            |    travisYaml:object(expression: "main:.travis.yml") {
            |      ...file
            |    }
            |    rootFolder:object(expression: "main:") {
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_f5adbe19c74682353244cd4d03bfa5ad5fdc2d98d385f45ef881dd9c544124f6
      */
    @Test public void test_GitHubGraphQL_sendGQL_custom() {
        mygh.sendGQL('query { foo }')
        assert request_meta['data'].toString() == '{"query":"query { foo }"}'
    }
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_8d979296f2b89c1ca8394223195177dbb0e7fdf63d4b06a02a0a61ffedbb6552
      */
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_quotes() {
        mygh.sendGQL('query { foo(expr: "hello") }')
        assert request_meta['data'].toString() == '{"query":"query { foo(expr: \\"hello\\") }"}'
    }
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_37be24e4888e054916321189967ef4e5f42fe14569f5ee45229a27661e692618
      */
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_variables() {
        mygh.sendGQL('query { foo }', 'variables { bar }')
        assert request_meta['data'].toString() == '{"query":"query { foo }","variables":"variables { bar }"}'
    }
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_a163c471d4cdb6f7aa30f1540a27da77a2dbf6f9358137ac9fc42c380305008a
      */
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_variables_Map() {
        Map variables = [ myvar: 3 ]
        mygh.sendGQL('query { foo(expr: "hello") }', variables)
        assert request_meta['data'].toString() == '{"query":"query { foo(expr: \\"hello\\") }","variables":"{\\"myvar\\":3}"}'
    }
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_62903ca59bbd9758c8226d190b8b4e98960a39b20a3e93a511f9633aa4712f3e
      */
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_62903ca59bbd9758c8226d190b8b4e98960a39b20a3e93a511f9633aa4712f3e
      */
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_0f186475e5335fe3ff3cd4c7566ff0e66629ee234cf618c6cfec504f444d9f83
      */
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_11d22ccb4e46c2bee006552cb7634b0ed8a50ba0ad5b5f20a3ef4c7310d43b32
      */
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_two_branches_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 1'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]], 'gitRef1':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 2'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'CHANGELOG.md', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman', 'jervis', ['refs/heads/main', 'refs/heads/jervis_simple'])
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_ba375a80eda41359695b8003af9b7e13c6af250396d1dd5f843090459533063a
      */
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_invalid() {
        Map response = mygh.getJervisYamlFiles('invalid', 'invalid')
        assert response instanceof Map
        assert response == [:]
    }
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_0f186475e5335fe3ff3cd4c7566ff0e66629ee234cf618c6cfec504f444d9f83
      */
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_11d22ccb4e46c2bee006552cb7634b0ed8a50ba0ad5b5f20a3ef4c7310d43b32
      */
    @Test public void test_GitHubGraphQL_getJervisYamlFiles_repositoryWithOwner_two_branches_defaults() {
        Map shouldRespond = ['gitRef0':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 1'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'README.md', 'type':'blob']]]], 'gitRef1':['jervisYaml0':null, 'jervisYaml1':['text':'mock data 2'], 'rootFolder':['file':[['name':'.travis.yml', 'type':'blob'], ['name':'CHANGELOG.md', 'type':'blob'], ['name':'README.md', 'type':'blob']]]]]
        Map response = mygh.getJervisYamlFiles('samrocketman/jervis', ['refs/heads/main', 'refs/heads/jervis_simple'])
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
    /**
      * This test uses the following files.
      *
      * src/test/resources/mocks/api.github.com_graphql_ba375a80eda41359695b8003af9b7e13c6af250396d1dd5f843090459533063a
      */
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
