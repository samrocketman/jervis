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
/**
  Get a build environment context map meant to be used with build trigger
  filtering by context.  This is used by the FilterByContext class typically.
  */

import hudson.model.Cause
import hudson.model.Job
import hudson.model.Run
import hudson.triggers.TimerTrigger
import jenkins.plugins.git.GitTagSCMHead
import jenkins.scm.api.SCMHead
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead
import org.jenkinsci.plugins.pipeline.github.trigger.IssueCommentCause

@NonCPS
String getPullRequestComment(Run build) {
    def cause = build.causes.find { it in IssueCommentCause }
    // username of commenter is stored in cause?.userLogin
    cause?.comment ?: ''
}

@NonCPS
Boolean getBranchName(Job build_parent) {
    SCMHead head = build_parent?.getProperty(BranchJobProperty)?.branch?.head
    if(!(head instanceof BranchSCMHead)) {
        return ''
    }
    head?.name ?: ''
}

@NonCPS
String getTagName(Job build_parent) {
    SCMHead head = build_parent?.getProperty(BranchJobProperty)?.branch?.head
    if(!(head instanceof GitTagSCMHead)) {
        return ''
    }
    head?.name ?: ''
}

@NonCPS
Boolean isPullRequest(Job build_parent) {
    SCMHead head = build_parent?.getProperty(BranchJobProperty)?.branch?.head
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
  Return a HashMap which includes the full cause and context of the build
  environment in Jenkins from which to act on with conditional logic.
  */
@NonCPS
Map call() {
    Map context = [
        trigger: '',
        context: '',
        metadata: [
            pr: false,
            branch: '',
            tag: '',
            push: false,
            cron: false,
            manually: '',
            pr_comment: ''
        ]
    ]

    // Determine the root cause (build trigger)
    context.metadata.manually = getUserCause(currentBuild.rawBuild)
    if(context.metadata.manually) {
        context.trigger = 'manually'
    }

    context.metadata.cron = isTimerBuild(currentBuild.rawBuild)
    if(context.metadata.cron) {
        context.trigger = 'cron'
    }

    context.metadata.pr_comment = getPullRequestComment(currentBuild.rawBuild)
    if(context.metadata.pr_comment) {
        context.trigger = 'pr_comment'
    }

    // auto could mean a build was pushed or a pull request was opened.  It
    // just means it didn't match other known trigger types.
    if(!context.trigger) {
        context.trigger = 'push'
        context.metadata.push = true
    } else {
        context.metadata.push = false
    }

    // Determine the context
    context.metadata.pr = isPullRequest(currentBuild.rawBuild.parent)
    if(context.metadata.pr) {
        context.context = 'pr'
    }
    context.metadata.tag = getTagName(currentBuild.rawBuild.parent)
    if(context.metadata.tag) {
        context.context = 'tag'
    }
    context.metadata.branch = getBranchName(currentBuild.rawBuild.parent)
    if(context.metadata.branch) {
        context.context = 'branch'
    }

    return context
}
