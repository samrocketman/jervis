/*
   Copyright 2014-2020 Sam Gleske - https://github.com/samrocketman/jervis

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
/*
   This allows obtaining multiple independent resource locks from the lockable
   resources plugin.

   EXAMPLES

    Obtain two locks

        withLocks(['foo', 'bar']) {
            // some code runs after both foo and bar locks are obtained
        }

    Obtain one lock with parallel limits.  The index gets evaluated against the
    limit in order to limit parallelism with modulo operation.  Similar to
    workaround my color-lock example as documented in
    https://issues.jenkins-ci.org/browse/JENKINS-44085

    Note: if you specify multiple locks with limit and index, then the same
    limits apply to all locks.  The next example will show how to limit
    specific locks without setting limits for all locks.

        Map tasks = [failFast: true]
        for(int i = 0; i < 5; i++) {
            int taskInt = i
            tasks["Task ${taskInt}"] = {
                stage("Task ${taskInt}") {
                    withLocks(obtain_lock: 'foo', limit: 3, index: taskInt) {
                        echo 'This is an example task being executed'
                        sleep(30)
                    }
                    echo 'End of task execution.'
                }
            }
        }
        stage("Parallel tasks") {
            parallel(tasks)
        }

    Obtain obtain the foo and bar locks.  Only proceed if both locks have been
    obtained simultaneously.  However, set foo locks to be limited by 3
    simultaneous possible locks.  When specifying multiple locks you can pass
    in the setting with lock name plus _limit and _index to define behavior for
    just that lock.

    In the following scenario, the first three locks will race for foo lock
    with limits and wait on bar for execution.  The remaining two tasks will
    wait on just foo with limits.

        Map tasks = [failFast: true]
        for(int i = 0; i < 5; i++) {
            int taskInt = i
            tasks["Task ${taskInt}"] = {
                List locks = ['foo', 'bar']
                if(taskInt > 2) {
                    locks = ['foo']
                }
                stage("Task ${taskInt}") {
                    withLocks(obtain_lock: ['foo', 'bar'], foo_limit: 3, foo_index: taskInt) {
                        echo 'This is an example task being executed'
                        sleep(30)
                    }
                    echo 'End of task execution.'
                }
            }
        }
        stage("Parallel tasks") {
            parallel(tasks)
        }

    You may need to quote the setting depending on the characters used.  For
    example, if you have a lock named with a special character other than an
    underscore, then it must be quoted.

        withLocks(obtain_lock: ['hello-world'], 'hello-world_limit': 3, ...) ...
 */

@NonCPS
Map prepareAndCheckSettings(Map settings) {
    List errors = []
    if(settings['obtain_lock'] instanceof String) {
        settings['obtain_lock'] = [settings['obtain_lock']]
    }
    if(!(settings['obtain_lock'] instanceof List)) {
        errors << "obtain_lock must be a String or List of lock names to be obtained."
    }
    // find and test all integers
    settings.keySet().toList().findAll { key ->
        String possibleLockName = key -~ '_limit$' -~ '_index$'
        key == 'index' || key == 'limit' ||
        (
            (key.endsWith('_index') || key.endsWith('_limit')) &&
            possibleLockName in settings['obtain_lock']
        )
    }.each { key ->
        if(!(settings[key] instanceof Integer)) {
            errors << "${key} must be an Integer."
        }
        else {
            if(key.endsWith('limit')) {
                if(!(settings[key] > 0)) {
                    errors << "${key} must be greater than zero."
                }
            }
            else if(settings[key] < 0) {
                errors << "${key} must be an Integer greater than or equal to zero."
            }
        }
    }
    if(errors) {
        throw new Exception("\nwithLocks ERROR:\n    " + errors.join('\n    '))
    }
    settings
}

def call(Map settings, Closure body) {
    List locks = []
    settings = prepareAndCheckSettings(settings)
    List obtain_lock = settings['obtain_lock'] ?: []
    if(obtain_lock) {
        String lockName = obtain_lock.pop()
        int limit = settings["${lockName}_limit"] ?: (settings['limit'] ?: 1)
        int lockNameIndex = -1
        if(settings.containsKey("${lockName}_index")) {
            lockNameIndex = settings["${lockName}_index"]
        }
        else if(settings.containsKey('index')) {
            lockNameIndex = settings['index']
        }
        if(lockNameIndex >= 0 ) {
            // Set a parallel execution limit across all resources using modulo
            // operator.
            lockName += '-' + (lockNameIndex % limit)
        }
        lock(lockName) {
            withLocks(settings, obtain_lock: obtain_lock, body)
        }
    }
    else {
        body()
    }
}

def call(Map additional_settings, Map settings, Closure body) {
    call(settings + additional_settings, body)
}

def call(List obtain_lock, Closure body) {
    call(obtain_lock: obtain_lock, body)
}

