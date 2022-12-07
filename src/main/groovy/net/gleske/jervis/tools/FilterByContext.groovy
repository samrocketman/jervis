/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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

package net.gleske.jervis.tools

import net.gleske.jervis.exceptions.FilterByContextException

/**
  Filter by context can provide a boolean result based on the conditions in
  which a project is being built combined with user-provided filters.
  */
class FilterByContext {
    /**
      A build environment context necessary for implementing filters around
      whether or not a build should occur.
      */
    Map context = [:]

    /**
      A list of user-provided filters where a filter can be a single string or
      a filter map.
      */
    List filters

    /**
      The maximum depth recursion should be allowed when evaluating
      user-provided filters.  This is for protection against infinite loops.
      */
    int maxRecursionDepth = 10

    /**
      It is an error to instantiate this class without a build environment
      context and a user-provided filter.
      */
    public FilterByContext() {
        throw new FilterByContextException('Must provide a build environment context and a user-provided filter.  This is likely a bug introduced by the admin and should be fixed by admin.')
    }

    public FilterByContext(Map context, List filters) {
        List requiredArgs = ['trigger', 'context', 'metadata'] - context.keySet().toList()
        if(requiredArgs) {
            throw new FilterByContextException("Missing required keys provided by admin: ${requiredArgs.join('\n')}")
        }
        this.context = context
        this.filters = filter
    }
    public FilterByContext(Map context, String filter) {
        this(context, [filter])
    }

    public FilterByContext(Map context, Map filter) {
        this(context, [filter])
    }


    // validate list filter
    // validate map filter
    // validate string filter
    // this function can be recursive for list filters with a max depth
    // Check if string or map is a valid filter
    Boolean validFilter(def filter, int depth = 0) {
    }
}
