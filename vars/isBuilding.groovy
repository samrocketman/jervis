/*
   Copyright 2014-2021 Sam Gleske - https://github.com/samrocketman/jervis

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

   Examples:

     Abort a pipeline early if it is not a pull request, a matched branch, or a
     tag matching semantic versioning.
        if(!isBuilding(
                branch: '/^\\Qmaster\\E$|^[0-9.]+-hotfix$',
                tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/',
                pr: null)) {
            // abort the pipeline
            return
        }

     Check if building the master branch.
         isBuilding(branch: 'master')

     Check for master branch or hotfix branches (branch starts with a version
     number and ends with -hotfix)
         isBuilding(branch: '/^\\Qmaster\\E$|^[0-9.]+-hotfix$')

     Check for a tag, but only if it matches semantic versioning.
         isBuilding(tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/')

     Check if building any branch, pull request, tag, or a scheduled timer
     build (cron)
         isBuilding('branch')
         isBuilding('pr')
         isBuilding('tag')
         isBuilding('cron')
         isBuilding('manually')
  */

import static net.gleske.jervis.tools.AutoRelease.isMatched

import hudson.model.Cause
import hudson.model.Job
import hudson.model.Run
import hudson.triggers.TimerTrigger
import jenkins.plugins.git.GitTagSCMHead
import jenkins.scm.api.SCMHead
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty

@NonCPS
Boolean isMatchedBranchBuild(Job build_parent, String branch, String expression) {
    if(isMatchedTagBuild(build_parent, '/.*/') || isMatchedPRBuild(build_parent)) {
        return false
    }
    isMatched(expression, branch ?: '')
}

@NonCPS
Boolean isMatchedTagBuild(Job build_parent, String expression) {
    SCMHead head = build_parent?.getProperty(BranchJobProperty)?.branch?.head
    if(!(head instanceof GitTagSCMHead)) {
        return false
    }
    isMatched(expression, head?.name ?: '')
}

@NonCPS
Boolean isMatchedPRBuild(Job build_parent) {
    SCMHead head = build_parent.getProperty(BranchJobProperty).branch.head
    (head instanceof PullRequestSCMHead) ?: false
}

@NonCPS
Boolean isTimerBuild(Run build) {
    (build?.causes?.find {
      it instanceof TimerTrigger.TimerTriggerCause
    } as Boolean) ?: false
}

@NonCPS
String getUserCause(Run build) {
    build.causes.find { Cause c ->
        c instanceof Cause.UserIdCause
    }?.userId ?: ''
}

/**
   This is the entrypoint for passing an expression to filtering builds.

   Examples:
     isBuilding(branch: 'master')
     isBuilding(branch: '/^\\Qmaster\\E$|^[0-9.]+-hotfix$')
     isBuilding(tag: '1.0')
     isBuilding(tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/') - only matches semantic version tags
     isBuilding(manually: true, branch: true, combined: true) - return a single boolean of the overall status
     isBuilding(manually: 'samrocketman', combined: true) - return a single boolean if a specific user triggered the build.
     isBuilding(manually: false, combined: true) - returns true if the build was triggered by anything except a user, manually.

   @param filters key-value filters to match build types.  e.g. branch, pr,
                  tag, schedule, and their associated filters to match.
   @return A map of the results for what is found.  If nothing is matched, then
           this will return an empty Map which is boolean falsey in groovy.
           Can also return a Boolean if a user passes in the combined: true option.
  */
def call(Map filters) {
    Map results = [:]
    for(String k : filters.keySet()) {
        def result = false
        if(k == 'cron') {
            result = isTimerBuild(currentBuild.rawBuild)
        }
        if(k == 'pr') {
            result = isMatchedPRBuild(currentBuild.rawBuild.parent)
        }
        if(k == 'tag') {
            result = isMatchedTagBuild(currentBuild.rawBuild.parent, filters[k])
        }
        if(k == 'branch') {
            result = isMatchedBranchBuild(currentBuild.rawBuild.parent, env.BRANCH_NAME, filters[k])
        }
        if(k == 'manually') {
            result = getUserCause(currentBuild.rawBuild)
            if(filters[k] instanceof String) {
                // if user passes in a username then return a boolean for if
                // that user was the one who triggered it.
                result = (result == filters[k])
            }
            else if(!filters[k]) {
                // if user passes in manually: false
                result = !(result as Boolean)
            }
        }
        if(result) {
            results[k] = result
        }
    }
    // return a combined single boolean or a map of all the results
    if(filters?.get('combined', false)) {
        (results ?: ['': false]).every { k, v ->
            v
        }
    }
    else {
        results
    }
}

/**
   This is the entrypoint for filtering builds based on generic type.
   Examples:
     isBuilding('branch')
     isBuilding('pr')
     isBuilding('tag')
     isBuilding('cron')
     isBuilding('manually')

   @param filter a pre-defined filter such as branch, pr, tag, cron, or manually.
   @return a String (for manually triggered user ID) or boolean (for everything else)
  */
def call(String filter) {
    call([(filter): '/.*/'])?.get(filter) ?: false
}

/**
  This filtering entry point takes a list of items and passes it through
  isBuilding.  The list can contain Strings, Maps, or a combination of the two.

  If the String 'combined' is in the List then it will require that all items
  in the List match the filter.  Otherwise, if any item is true it will return
  true.

  Check for only manually built tags.
    isBuilding(['combined', 'tag', 'manually'])

  Match branches or pull requests.
    isBuilding(['pr', 'tag'])

  Match manually built pull requests or manually built tags.
    isBuilding([['combined', 'tag', 'manually'], ['combined', 'pr', 'manually']])

  Alternate syntax for manually built pull requests or manually built tags.
    isBuilding(['combined', ['pr', 'tag'], 'manually'])
  */

Boolean call(List filter) {
    if('combined' in filter) {
        (filter - ['combined']).every {
            call(it)
        }
    }
    else {
        filter.any {
            call(it)
        }
    }
}

void call() {
    throw new Exception('ERROR: this step must be called with arguments.  e.g. branch, pr, tag, cron, or manually')
}
