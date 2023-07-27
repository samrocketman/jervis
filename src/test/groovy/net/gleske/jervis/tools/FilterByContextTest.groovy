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

    /**
        Adding two maps together only creates a shallow copy.  For testing, a
        deep copy is necessary.  Deep copying is necessary for testing maps of
        maps like this use case.
      */
    Map deepcopy(Map orig) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(bos)
        oos.writeObject(orig); oos.flush()
        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray())
        ObjectInputStream ois = new ObjectInputStream(bin)
        return ois.readObject()
    }
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
    @Test public void test_FilterByContext_isBuilding_branch() {
        // isBuilding('branch')
        String filter = 'branch'
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_pr() {
        // isBuilding('pr')
        String filter = 'pr'
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_isBuilding_tag() {
        // isBuilding('tag')
        String filter = 'tag'
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_pr_comment() {
        // isBuilding('pr_comment')
        String filter = 'pr_comment'
        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_cron() {
        // isBuilding('cron')
        String filter = 'cron'
        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_manually() {
        // isBuilding('manually')
        String filter = 'manually'
        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_push() {
        // isBuilding('push')
        String filter = 'push'
        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_isBuilding_abort_pipeline_expression() {
        // isBuilding(branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/', tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/', pr: null)
        Map filter = [branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/', tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/', pr: null]
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // branch
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.branch = '1.2.3-hotfix'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.branch = 'myfeature'
        assert shouldFilter.allowBuild == false
        // tag
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.tag = '1.2.3.4'
        assert shouldFilter.allowBuild == false
        shouldFilter.context.metadata.tag = '1.2.3-suffix'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.tag = 'mytag'
        assert shouldFilter.allowBuild == false
        // pr
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == true
    }
    @Test public void test_FilterByContext_isBuilding_main_branch() {
        // isBuilding(branch: 'main')
        Map filter = [branch: 'main']
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.branch = 'myfeature'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_main_or_hotfix_branch() {
        // isBuilding(branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/')
        Map filter = [branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/']
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.branch = '1.2.3-hotfix'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.branch = 'myfeature'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_semver_tag() {
        // isBuilding(tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/')
        Map filter = [tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/']
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.tag = '1.2.3-hotfix'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.tag = '0.1'
        assert shouldFilter.allowBuild == false
        shouldFilter.context.metadata.tag = '0.1.0'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.tag = 'v0.1.0'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_manual_tag() {
        // isBuilding(['combined', 'tag', 'manually'])
        List filter = ['combined', 'tag', 'manually']
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        // manual tag
        shouldFilter.context.trigger = 'manually'
        assert shouldFilter.allowBuild == true
        // manual pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_pr_or_tag() {
        // isBuilding(['pr', 'tag'])
        List filter = ['pr', 'tag']
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == true
        // pushed pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == true
        // pushed branch
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.branch = 'main'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_manual_pr_or_tag() {
        // isBuilding([['combined', 'tag', 'manually'], ['combined', 'pr', 'manually']])
        List filter = [['combined', 'tag', 'manually'], ['combined', 'pr', 'manually']]
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        // manual tag
        shouldFilter.context.trigger = 'manually'
        assert shouldFilter.allowBuild == true
        // manual pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == true
        // manual branch
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_manual_pr_or_tag_alternate() {
        // isBuilding(['combined', ['pr', 'tag'], 'manually'])
        List filter = ['combined', ['pr', 'tag'], 'manually']
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        // manual tag
        shouldFilter.context.trigger = 'manually'
        assert shouldFilter.allowBuild == true
        // manual pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == true
        // manual branch
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_manual_pr_or_tag_false_branch() {
        // isBuilding(combined: true, manually: true, branch: false)
        Map filter = [combined: true, manually: true, branch: false]
        Map branchContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        // manual tag
        shouldFilter.context.trigger = 'manually'
        assert shouldFilter.allowBuild == true
        // manual pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == true
        // manual branch
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_inverse_branch() {
        // isBuilding(['inverse', 'branch'])
        List filter = ['inverse', 'branch']
        Map tagContext = [
            trigger: 'push',
            context: 'tag',
            metadata: [
                pr: false,
                branch: '',
                tag: '1.2.3',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pushed tag
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == true
        // manual tag
        shouldFilter.context.trigger = 'manually'
        assert shouldFilter.allowBuild == true
        // manual pr
        shouldFilter.context.context = 'pr'
        shouldFilter.context.metadata.pr = true
        shouldFilter.context.metadata.tag = ''
        assert shouldFilter.allowBuild == true
        // manual branch
        shouldFilter.context.context = 'branch'
        shouldFilter.context.metadata.pr = false
        shouldFilter.context.metadata.branch = 'main'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_inverse_push_pr() {
        // isBuilding(['combined', 'pr', ['inverse', 'push']])
        List filter = ['combined', 'pr', ['inverse', 'push']]
        Map pushContext = [
            trigger: 'push',
            context: 'pr',
            metadata: [
                pr: true,
                branch: '',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.metadata.pr_comment = 'retest this please'
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == true
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == true
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == true
        // check push and non-push branch
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter.context.trigger = 'manually'
        shouldFilter.context.metadata.push = false
        shouldFilter.context.metadata.manually = 'someuser'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_isBuilding_matrix_on_pr_comment() {
        // matrix on pr_comment: '/.*[Bb]uild +[Pp](ull +)?[Rr](equest)? +[Mm]atrix.*/'
        Map filter = [pr_comment: '/.*[Bb]uild +[Pp](ull +)?[Rr](equest)? +[Mm]atrix.*/']
        Map prCommentContext = [
            trigger: 'pr_comment',
            context: 'pr',
            metadata: [
                pr: true,
                branch: '',
                tag: '',
                push: false,
                cron: false,
                manually: '',
                pr_comment: 'build pr matrix'
            ]
        ]
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.pr_comment = 'Build Pull Request matrix'
        assert shouldFilter.allowBuild == true
        shouldFilter.context.metadata.pr_comment = 'retest this please'
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_never_build_string() {
        String filter = 'never'
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == false

        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_never_build_list() {
        List filter = ['never']
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == false

        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_never_build_map() {
        Map filter = [never:null]
        Map branchContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        Map tagContext = deepcopy(branchContext) + [context: 'tag']
        tagContext.metadata.branch = ''
        tagContext.metadata.tag = '1.2.3'
        Map prContext = deepcopy(branchContext) + [context: 'pr']
        prContext.metadata.branch = ''
        prContext.metadata.pr = true
        // check values across full git workflow
        shouldFilter = new FilterByContext(branchContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(tagContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prContext, filter)
        assert shouldFilter.allowBuild == false

        Map pushContext = [
            trigger: 'push',
            context: 'branch',
            metadata: [
                pr: false,
                branch: 'main',
                tag: '',
                push: true,
                cron: false,
                manually: '',
                pr_comment: ''
            ]
        ]
        // pr_comment
        Map prCommentContext = deepcopy(pushContext) + [trigger: 'pr_comment']
        prCommentContext.metadata.push = false
        prCommentContext.context = 'pr'
        prCommentContext.metadata.pr = true
        prCommentContext.metadata.pr_comment = 'retest this please'
        // manually
        Map manuallyContext = deepcopy(pushContext) + [trigger: 'manually']
        manuallyContext.metadata.push = false
        manuallyContext.metadata.manually = 'someuser'
        // cron
        Map cronContext = deepcopy(pushContext) + [trigger: 'cron']
        cronContext.metadata.push = false
        cronContext.metadata.cron = true
        // check triggers
        shouldFilter = new FilterByContext(cronContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(manuallyContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(prCommentContext, filter)
        assert shouldFilter.allowBuild == false
        shouldFilter = new FilterByContext(pushContext, filter)
        assert shouldFilter.allowBuild == false
    }
    @Test public void test_FilterByContext_default_constructor() {
        shouldFail(FilterByContextException) {
            new FilterByContext()
        }
    }
    @Test public void test_FilterByContext_validate_context_failures() {
        Map context
        shouldFail(FilterByContextException) {
            new FilterByContext([:])
        }
        shouldFail(FilterByContextException) {
            new FilterByContext([trigger: '', context: ''])
        }
        shouldFail(FilterByContextException) {
            new FilterByContext([trigger: '', metadata: [:]])
        }
        shouldFail(FilterByContextException) {
            new FilterByContext([metadata: [:], context: ''])
        }
        shouldFail(FilterByContextException) {
            context = deepcopy(defaultContext)
            context.trigger = 1
            new FilterByContext(context)
        }
        shouldFail(FilterByContextException) {
            context = deepcopy(defaultContext)
            context.context = 1
            new FilterByContext(context)
        }
        shouldFail(FilterByContextException) {
            context = deepcopy(defaultContext)
            context.metadata = ''
            new FilterByContext(context)
        }
        shouldFail(FilterByContextException) {
            context = deepcopy(defaultContext)
            context.metadata[3] = ''
            new FilterByContext(context)
        }
        shouldFail(FilterByContextException) {
            context = deepcopy(defaultContext)
            context.metadata.pr = 4
            new FilterByContext(context)
        }
        // no branch
        shouldFail(FilterByContextException) {
            context = [
                trigger: 'push',
                context: 'pr',
                metadata: [
                    push: true,
                    pr: true,
                    tag: ''
                ]
            ]
            new FilterByContext(context)
        }
        // no tag
        shouldFail(FilterByContextException) {
            context = [
                trigger: 'push',
                context: 'pr',
                metadata: [
                    push: true,
                    pr: true,
                    branch: '',
                ]
            ]
            new FilterByContext(context)
        }
        // no pr
        shouldFail(FilterByContextException) {
            context = [
                trigger: 'push',
                context: 'branch',
                metadata: [
                    push: true,
                    branch: 'main',
                    tag: ''
                ]
            ]
            new FilterByContext(context)
        }

    }
    @Test public void test_FilterByContext_validate_filter_failures() {
        shouldFail(FilterByContextException) {
            shouldFilter.filters = [[[[[[[[[[['never']]]]]]]]]]]
        }
        // do not fail
        shouldFilter.filters = [[[[[[[[[['never']]]]]]]]]]
        shouldFilter.maxRecursionDepth = 3
        shouldFail(FilterByContextException) {
            shouldFilter.filters = [[[[[[[[[['never']]]]]]]]]]
        }
        shouldFilter.filters = [[['never']]]
        shouldFail(FilterByContextException) {
            shouldFilter.filters = 'foo'
        }
        shouldFail(FilterByContextException) {
            shouldFilter.filters = 20
        }
        shouldFail(FilterByContextException) {
            shouldFilter.filters = [hello: 'world', pr: true]
        }
        shouldFail(FilterByContextException) {
            shouldFilter.filters = [pr: 3]
        }
    }
    @Test public void test_FilterByContext_allowBuild_with_filters_arg() {
        Map context = [
            trigger: 'pr_comment',
            context: 'pr',
            metadata: [
                pr: true,
                branch: 'main',
                tag: '',
                push: false,
                cron: false,
                manually: '',
                pr_comment: 'retest this please'
            ]
        ]

        // Build on push, tag, or manually triggered build.  OR if in a PR and
        // the user commented a specific comment.
        List filter = ['push', 'tag', 'manually', [pr_comment: '/.*test this please.*/']]
        shouldFilter = new FilterByContext(context, filter)
        assert shouldFilter.allowBuild == true
        assert shouldFilter.allowBuild(filter) == true
        assert shouldFilter.allowBuild('tag') == false
        assert shouldFilter.allowBuild('manually') == false
        assert shouldFilter.allowBuild('push') == false
        assert shouldFilter.allowBuild('pr_comment') == true
        assert shouldFilter.allowBuild('pr') == true
        assert shouldFilter.allowBuild(pr_comment: '/some other comment/') == false
        shouldFail(FilterByContextException) {
            shouldFilter.allowBuild('invalid')
        }
    }
}
