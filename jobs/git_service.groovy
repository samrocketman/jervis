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

//this code should be at the beginning of every script included which requires bindings
require_bindings('jobs/git_service.groovy', ['git_service', 'system_creds'])

/*
   Configures the git_service binding using system_creds binding.
 */

import net.gleske.jervis.remotes.GitHub

switch(git_service) {
    case GitHub:
        //authenticate
        String gh_token = system_creds.getCredentials().find {
            it.class.simpleName == 'StringCredentialsImpl' && it.id == 'github-token'
        }.with {
            if(it) {
                it.secret
            }
        }
        if(gh_token) {
            println 'Found github-token credentials ID.'
            git_service.gh_token = gh_token
        }
        else if(System.getenv('GITHUB_TOKEN')) {
            println 'Found GITHUB_TOKEN environment variable.'
            git_service.gh_token = System.getenv('GITHUB_TOKEN')
        }
        //GitHub Enterprise web URL; otherwise it will simply be github.com
        if(System.getenv('GITHUB_URL')) {
            println 'Found GITHUB_URL environment variable.'
            git_service.gh_web = System.getenv('GITHUB_URL')
        }
}
