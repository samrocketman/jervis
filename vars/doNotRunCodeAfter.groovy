/*
   Copyright 2014-2026 Sam Gleske - https://github.com/samrocketman/jervis

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
  This conditional step will execute a code block until a certain date has
  passed.  This step is useful for tasks like code deprecation within
  pipelines, warning users, and then automatically preventing access.

  Two CPS pipeline examples:

      doNotRunCodeAfter('YYYY-MM-DD') {
          // custom code which will not run after provided date
      }

      doNotRunCodeAfter(date: 'YYYY-MM-DD', afterDate: {-> println 'runs after date' }) {
          // runs before date
      }

  Two NonCPS examples:

      Boolean shouldRun = doNotRunCodeAfter.date('YYYY-MM-DD')

      doNotRunCodeAfter.date('YYYY-MM-DD') {
          // NonCPS code will not run after date
      }
  */

@NonCPS
def date(String date, Closure body = null) {
    if(!isDate.valid(date)) {
        throw new Exception('doNotRunCodeAfter date format must be "YYYY-MM-DD"')
    }
    Boolean shouldRun = isDate(before: date)
    if(body) {
        if(shouldRun) {
            return body()
        }
        return
    }
    // return a plain boolean
    shouldRun
}

def call(String date, Closure body) {
    call([date: date], body)
}

def call(Map settings, Closure body) {
    if(!isDate.valid(settings.date ?: '')) {
        error 'doNotRunCodeAfter date format must be "YYYY-MM-DD"'
    }
    if(settings.afterDate && !(settings.afterDate in Closure)) {
        error 'doNotRunCodeAfter(afterDate: {}) option is specified but afterDate is not a closure.'
    }
    if(date(settings.date)) {
        // before date
        return body()
    } else {
        // after date
        if(settings.afterDate) {
            return settings.afterDate()
        }
    }
}

def call(Map additional_settings, Map settings, Closure body) {
    call(settings + additional_settings, body)
}
