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
package net.gleske.jervis.tools
//the FilterByContextTest() class automatically sees the FilterByContext class because they're in the same package
import net.gleske.jervis.exceptions.FilterByContextException

import org.junit.After
import org.junit.Before
import org.junit.Test

class FilterByContextTest extends GroovyTestCase {
    FilterByContext shouldFilter
    final Map defaultContext = [
        trigger: 'push',
        context: 'pr',
        metadata: [
            push: true,
            pr: true,
            branch: '',
            tag: ''
        ]
    ]
    @Before protected void setUp() {
        shouldFilter = new FilterByContext(defaultContext, 'pr')
    }
    @After protected void tearDown() {
        shouldFilter = null
    }
    @Test public void test_FilterByContextTest_verifyDefaultContext_data() {
        // Because a lot of the tests make assumptions around this test data so
        // this is to ensure the test data is not changed by mistake
        assert defaultContext.trigger == 'push'
        assert defaultContext.context == 'pr'
        assert defaultContext.metadata.push == true
        assert defaultContext.metadata.pr == true
        assert defaultContext.metadata.branch == ''
        assert defaultContext.metadata.tag == ''
    }
    @Test public void test_FilterByContext_sample_usage() {
        assert shouldFilter.getAllowBuild() == true
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_update_basic_filters() {
        shouldFilter.filters = 'tag'
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = 'branch'
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = 'push'
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_update_map_filters() {
        shouldFilter.filters = [push: true]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_combined_behavior() {
        shouldFilter.filters = [branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [combined: false, branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [combined: false, pr: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [combined: true, branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [combined: true, pr: false, push: true]
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_inverse_behavior_list() {
        shouldFilter.filters = ['pr']
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = ['branch']
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = ['tag']
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = ['inverse', 'pr']
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = ['inverse', 'branch']
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = ['inverse', 'tag']
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_list_combined() {
        shouldFilter.filters = ['push', 'tag']
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = ['inverse', ['pr', 'tag']]
        assert shouldFilter.allowBuild == false

        shouldFilter.filters = ['combined', 'push', 'tag']
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = ['inverse', 'combined', 'push', 'tag']
        assert shouldFilter.allowBuild == true

        shouldFilter.filters = ['inverse', ['pr', 'tag']]
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_inverse_behavior_list_branch() {
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        shouldFilter.filters = ['pr', 'tag']
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = ['inverse', ['pr', 'tag']]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_list_pr() {
        shouldFilter.filters = ['pr', 'tag']
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = ['inverse', ['pr', 'tag']]
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_inverse_behavior_list_tag() {
        shouldFilter.context.context = 'tag'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.tag = '1.2.3'
        shouldFilter.filters = ['pr', 'tag']
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = ['inverse', ['pr', 'tag']]
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_inverse_behavior_map() {
        // Map testing
        shouldFilter.filters = [branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [inverse: true, branch: false, push: true]
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = [inverse: false, branch: false, push: true]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_map_combined() {
        shouldFilter.filters = [combined: false, branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [inverse: true, combined: false, branch: false, push: true]
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = [inverse: false, combined: false, branch: false, push: true]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_map_pr_false() {
        shouldFilter.filters = [combined: false, pr: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [inverse: true, combined: false, pr: false, push: true]
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = [inverse: false, combined: false, pr: false, push: true]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_map_branch_false() {
        shouldFilter.filters = [combined: true, branch: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [inverse: true, combined: true, branch: false, push: true]
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = [inverse: false, combined: true, branch: false, push: true]
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_inverse_behavior_map_pr_false_combined() {
        shouldFilter.filters = [combined: true, pr: false, push: true]
        assert shouldFilter.allowBuild == false
        shouldFilter.filters = [inverse: true, combined: true, pr: false, push: true]
        assert shouldFilter.allowBuild == true
        shouldFilter.filters = [inverse: false, combined: true, pr: false, push: true]
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_context_constructor() {
        shouldFilter = new FilterByContext(defaultContext)
        assert shouldFilter.filters == FilterByContext.getAlwaysBuildExpression()
    }
    @Test public void test_FilterByContext_getAlwaysBuildExpression() {
        assert shouldFilter.alwaysBuildExpression == ['pr', 'branch', 'tag']
        assert shouldFilter.alwaysBuildExpression == ['pr', 'branch', 'tag']
        assert shouldFilter.getAlwaysBuildExpression() == ['pr', 'branch', 'tag']
        assert FilterByContext.getAlwaysBuildExpression() == ['pr', 'branch', 'tag']
    }
    @Test public void test_FilterByContext_getAlwaysBuildExpression_pr() {
        shouldFilter.filters = FilterByContext.getAlwaysBuildExpression()
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_getAlwaysBuildExpression_branch() {
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        shouldFilter.filters = FilterByContext.getAlwaysBuildExpression()
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_getAlwaysBuildExpression_tag() {
        shouldFilter.context.context = 'tag'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.tag = '1.2.3'
        shouldFilter.filters = FilterByContext.getAlwaysBuildExpression()
        assert shouldFilter.allowBuild == true
    }
}
