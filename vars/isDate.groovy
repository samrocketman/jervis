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
import java.time.Instant

/**
  This step provides some date-based comparisions to pipeline conditionals.

  Example usage:
    if(isDate(before: '2026-01-01')) {
        // code runs if the current date is before the given date
        // never runs after the given date.
    }
    if(isDate(after: '2026-01-01')) {
        // code runs if the current date is after the given date
        // never runs before the given date.
    }
    if(isDate(date: '2025-01-01', before: '2026-01-01')) {
        // code always runs because specific date is before a given date.
    }
    if(isDate(date: '2025-01-01', after: '2026-01-01')) {
        // code never runs because specific date is before a given date.
    }

  Date validation examples:
    // true because the given date is a valid YYYY-MM-DD.
    Boolean isDateValid = isDate.valid('2026-01-01')

    // false because the given date is a not formatted as YYYY-MM-DD.
    Boolean isDateValid = isDate.valid('not a number')
  */

@NonCPS
String now() {
    Instant.now()
}

@NonCPS
Boolean valid(String date) {
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
Boolean call(Map settings) {
    // ISO 8601 date-time format is used for comparison
    String iso8601suffix = 'T00:00:00.0Z'
    Instant date = (settings.date) ? Instant.parse(settings.date + iso8601suffix) : now()
    if(settings.before) {
        date.isBefore(Instant.parse(date + iso8601suffix))
    } else if(settings.after) {
        date.isAfter(Instant.parse(date + iso8601suffix))
    } else {
        error 'isDate must be called with "before" or "after" date option.'
    }
}
