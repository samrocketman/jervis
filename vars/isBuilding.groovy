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
/**
  This step implements filtering for different build types (or many at once).
  */

import static net.gleske.jervis.tools.AutoRelease.isMatched

import hudson.model.Job
import jenkins.plugins.git.GitTagSCMHead
import jenkins.scm.api.SCMHead
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty

@NonCPS
Boolean isMatchedBranchBuild(Job build_parent, String branch, String expression) {
    if(isMatchedTagBuild(build_parent, '/.*/') || isMatchedPRBuild(build_parent, '/.*/')) {
        return false
    }
    isMatched(expression, branch)
}

@NonCPS
Boolean isMatchedTagBuild(Job build_parent, String expression) {
    SCMHead head = build_parent?.getProperty(BranchJobProperty)?.branch?.head
    if(!(head instanceof GitTagSCMHead)) {
        return false
    }
    isMatched(expression, head.name)
}

@NonCPS
Boolean isMatchedPRBuild(Job build_parent, String expression) {
    SCMHead head = build_parent.getProperty(BranchJobProperty).branch.head
    if(!(head in PullRequestSCMHead)) {
        return false
    }
    isMatched(expression, head.name)
}

@NonCPS
Boolean isTimerBuild(Job build_parent) {
    build_parent?.causes?.find {
      it instanceof TimerTrigger.TimerTriggerCause
    } as Boolean
}

/**
   This is the entrypoint for passing an expression to filtering builds.

   Examples:
     isBuilding(branch: 'master')
     isBuilding(branch: '/^\\Qmaster\\E$|^[0-9.]+-hotfix$')
     isBuilding(tag: '1.0')
     isBuilding(tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/') - only matches semantic version tags

   @param filters key-value filters to match build types.  e.g. branch, pr,
                  tag, schedule, and their associated filters to match.
   @return A map of the results for what is found.  If nothing is matched, then
           this will return an empty Map which is boolean falsey in groovy.
  */
Map call(Map filters) {
    Map results = [:]
    for(k : filters.keySet()) {
        Boolean result = false
        if(k == 'cron') {
            result = isTimerBuild(currentBuild.rawBuild.parent)
        }
        if(k == 'pr') {
            result = isMatchedPRBuild(currentBuild.rawBuild.parent, filters[k])
        }
        if(k == 'tag') {
            result = isMatchedTagBuild(currentBuild.rawBuild.parent, filters[k])
        }
        if(k == 'branch') {
            result = isMatchedBranchBuild(currentBuild.rawBuild.parent, env.BRANCH_NAME, filters[k])
        }
        if(result) {
            results[k] = result
        }
    }
    results
}

/**
   This is the entrypoint for filtering builds based on generic type.
   Examples:
     isBuilding('branch')
     isBuilding('pr')
     isBuilding('tag')
     isBuilding('cron')
  */
Boolean call(String filter) {
    call([(filter): '/.*/'])[filter]
}
