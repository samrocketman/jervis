/*
   Copyright 2014-2025 Sam Gleske - https://github.com/samrocketman/jervis

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

import java.time.Instant

@NonCPS
Boolean dateIsValid(String date) {
    if(!(date =~ /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/)) {
        return false
    }
    date.tokenize('-').collect { Integer.parseInt(it) }.with { List dateItems ->
        if(dateItems.any { it < 1 }) {
            return false
        }
        // month and day
        if(dateItems[1] > 12 || dateItems[2] > 31) {
            return false
        }
        // date is valid
        true
    }
}

@NonCPS
def date(String date, Closure body = null) {
    if(!dateIsValid(date)) {
        throw new Exception('doNotRunCodeAfter date format must be "YYYY-MM-DD"')
    }
    // ISO 8601 date-time format is used for comparison
    String iso8601suffix = 'T00:00:00.0Z'
    Boolean shouldRun = Instant.now().isBefore(Instant.parse(date + iso8601suffix))
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
    if(!dateIsValid(settings.date ?: '')) {
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
