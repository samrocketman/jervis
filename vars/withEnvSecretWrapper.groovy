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
  An environment wrapper which sets environment variables.  If available, also
  sets and masks decrypted properties from .jervis.yml.
 */

import net.gleske.jervis.lang.PipelineGenerator

def withEnvSecretWrapper(List envList, List secretPairs = [], Closure body) {
    if(secretPairs) {
        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: secretPairs]) {
            withEnv(envList) {
                body()
            }
        }
    }
    else {
        withEnv(envList) {
            body()
        }
    }
}

/**
  Jenkins Pipeline step withEnv requires a specific format for injecting
  variables into a shell environment.  This method simply converts the secrets
  Map into that expected key-value format.

  See also:
  https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#withenv-set-environment-variables
  */
@NonCPS
List<String> getEnvVars(Map secrets) {
    secrets.collect { k, v ->
        "${k}=${v}".toString()
    }
}

/**
  The Mask Passwords console wrapper requires key-value pairs to be a specific
  format.  The console wrapper is what filters passwords in the web UI and
  turns passwords into **** replacements.  This method simply converts the
  secrets Map into that expected key-value format.

  See also:
  https://wiki.jenkins.io/display/JENKINS/Mask+Passwords+Plugin
  */
@NonCPS
List<Map> getSecretPairs(Map secrets) {
    secrets.collect { k, v ->
        [var: k, password: v]
    }
}

def call(PipelineGenerator generator, List envList = [], Closure body) {
    List spe = generator.secretPairsEnv
    List secretPairs = spe[0]
    List secretEnv = spe[1]
    withEnvSecretWrapper(secretEnv + envList, secretPairs, body)
}

def call(List envList, List secretPairs = [], Closure body) {
    withEnvSecretWrapper(envList, secretPairs, body)
}

def call(Map secrets, Closure body) {
    withEnvSecretWrapper(getEnvVars(secrets), getSecretPairs(secrets), body)
}
