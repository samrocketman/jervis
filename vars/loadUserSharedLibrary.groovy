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
  This step is for users loading dynamic shared libraries.  This makes it a
  little more reader friendly when loading user shared libraries from an
  external library.

    Minimum Usage:
        loadUserSharedLibrary(repo: 'git@github.com:example/repo.git')

    Usage with all available options:
        loadUserSharedLibrary(
            repo: 'git@github.com:example/repo.git',
            branch: 'main',
            credentials_id: '',
            expose_changelog: false)
  */

import java.util.regex.Pattern

@NonCPS
String getUserCredentialsId(Map settings) {
    settings.credentials_id?.trim()
}

// if admins have a default credential ID (e.g. read-only, then it define a var
// in your shared pipeline library named
// adminDefaultUserSharedLibraryCredentials.groovy which returns a String when
// called.
String resolveCredentialsId(Map settings) {
    if(getCredentialsId(settings)) {
        getCredentialsId(settings)
    }
    else if(hasGlobalVar('adminDefaultUserSharedLibraryCredentials')) {
        adminDefaultUserSharedLibraryCredentials()
    }
    else {
        ''
    }
}

// if admins have a different default branch other than main, then define a
// global variable in your own shared library named
// adminDefaultUserSharedLibraryBranch.groovy which returns String meant to be
// a default branch for most of your organization projects.
@NonCPS
String getUserDefinedBranch(Map settings) {
    settings.branch?.trim()
}

String resolveBranch(Map settings) {
    if(getUserDefinedBranch(settings)) {
        getUserDefinedBranch(settings)
    }
    else if(hasGlobalVar('adminDefaultUserSharedLibraryBranch')) {
        adminDefaultUserSharedLibraryBranch()
    }
    else {
        'main'
    }
}

// determines the shared library name based on the remote Git repository URL.
@NonCPS
String resolveRepoName(Map settings) {
    if(settings.name?.trim()) {
        return settings.name?.trim()
    }
    String repo_url = settings.repo.trim()
    Pattern ssh_repo_pattern = ~/^[^@]+@[^:]+:(.+).git$/
    Pattern uri_repo_pattern = ~/^(https?|ssh|git):\/\/[^\/]+\/(.+).git$/
    String name = ''
    if(ssh_repo_pattern.matcher(repo_url).matches()) {
        name = ssh_repo_pattern.matcher(repo_url)[0][-1]
    }
    else if(uri_repo_pattern.matcher(repo_url).matches()) {
        name = uri_repo_pattern.matcher(repo_url)[0][-1]
    }
    else {
        // no pattern matched so throw an error
        name = 'error-no-name'
    }
    // implicit return
    name.replaceAll('/', '-')
}

@NonCPS
List checkForErrorsIn(Map settings) {
    List errors = []
    if(!settings.repo?.trim()) {
        errors << 'No repo defined.  e.g. repo: \'git@example.com:example/repo.git\''
    }
    if(resolveRepoName(settings) == 'error-no-name') {
        errors << 'Could not determine name from repo.  Add name for the shared library when it is loaded. e.g. name: \'your-repo-name\''
    }
}

@NonCPS
Boolean shouldResolveChangeLog(Map settings) {
    settings.expose_changelog ?: false
}

// this is the main entrypoint for the step
def call(Map settings = [:]) {
    if(checkForErrorsIn(settings)) {
        error('ERRORS FOUND: Update loadUserSharedLibrary arguments to fix the following.\n    ' + checkForErrorsIn(settings).join('\n    '))
    }
    library(
        changelog: shouldResolveChangeLog(settings),
        identifier: "${resolveRepoName(settings)}@${resolveBranch(settings)}",
        retriever: modernSCM(
                [$class: 'GitSCMSource', credentialsId: resolveCredentialsId(settings), remote: settings.repo.trim(), traits: [gitBranchDiscovery(), gitTagDiscovery()]]
            ) // end modernSCM()
        ) // end library()
}

def call(Map additional_settings, Map settings) {
    call(settings + additional_settings)
}
