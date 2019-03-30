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
//the GitHubTest() class automatically sees the GitHub() class because they're in the same package
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
        mockStaticUrl(url, URL, request_meta)
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
			|	jervisYaml:object(expression: "master:.jervis.yml") {
			|	  ...file
			|	}
			|	travisYaml:object(expression: "master:.travis.yml") {
			|	  ...file
			|	}
			|	rootFolder:object(expression: "master:") {
			|	  ...file
			|	}
			|  }
			|}
			|fragment file on GitObject {
			|  ... on Blob {
			|	text
			|  }
			|  ... on Tree {
			|	file:entries {
			|	  name
			|	}
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
    @Test public void test_GitHubGraphQL_sendGQL_custom_with_variables() {
        mygh.sendGQL('query { foo }', 'variables { bar }')
        assert request_meta['data'].toString() == '{"query":"query { foo }","variables":"variables { bar }"}'
    }
}
