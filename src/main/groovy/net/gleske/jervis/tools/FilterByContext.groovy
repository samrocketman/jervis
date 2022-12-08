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
  which a project is being built combined with user-provided filters.  This is
  used by Jervis to improve overall flexibility in how builds should be
  filtered.

  <h2>What is a context?</h2>

  A context consists of two primary parts.
  <ul>
    <li>
      Trigger: How a build was triggered.  For example, manually, periodic cron job, or
      pull request comment.
    </li>
    <li>
      Context: Which pipeline a build is occuring in a Git workflow.  For
      example, pull request, branch, or tag pipeline build.
    </li>
  </ul>

  A context is a HashMap with three root keys which get fed into this class
  from a CI server.

<pre><tt>Map context = [
    trigger: '',
    context: '',
    metadata: [:]
]</tt></pre>

  Valid values for trigger include: manually, cron, pr_comment, auto.

  Valid values for context include: pr, branch, tag.

  The following HashMap is the supported keys.  You can see an example of this
  in the
  <a target=_blank href="https://github.com/samrocketman/jervis/blob/main/vars/getBuildContextMap.groovy"><tt>getBuildContextMap()</tt></a>
  Jenkins pipeline step.
<pre><tt>Map context = [
    trigger: 'auto',
    context: 'pr',
    metadata: [
        pr: false,
        branch: ''
        tag: '',
        cron: false,
        manually: '',
        pr_comment: ''
    ]
]</tt></pre>

  The metadata values are defined as:

  <ul>
    <li>
      <tt>pr</tt>: A <tt>Boolean</tt>.  It is redundant with the
      <tt>trigger</tt> root key being set to <tt>pr</tt> and is not normally
      read during filtering.
    </li>
    <li>
      <tt>branch</tt>: A <tt>String</tt>.  If the <tt>context</tt> root key is
      <tt>branch</tt>, then this metadata is read for the name of the branch
      when evaluating filters.
    </li>
    <li>
      <tt>tag</tt>: A <tt>String</tt>.  If the <tt>context</tt> root key is
      <tt>tag</tt>, then this metadata is read for the name of the tag when
      evaluating filters.
    </li>
    <li>
      <tt>cron</tt>: A <tt>Boolean</tt>.  It is redundant with the
      <tt>trigger</tt> root key being set to <tt>cron</tt> and is not normally
      read during filtering.
    </li>
    <li>
      <tt>manually</tt>: A <tt>String</tt>.  If the <tt>trigger</tt> root key
      is set to <tt>manually</tt>, then this metadata is read for the username
      of the build cause.
    </tt>
    <li>
      <tt>pr_comment</tt>: A <tt>String</tt>. If the <tt>trigger</tt> rot key
      is set to <tt>pr_comment</tt>, then this metadata is read for the
      contents of the comment.
    </li>
  </ul>
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
    List filters = []

    /**
      On instantiation this list of allowed keys is populated which user input
      validation is checked against via validateFilter method.
      */
    private List allowedKeys = []

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
            throw new FilterByContextException("Context is missing required keys provided by admin: ${requiredArgs.join('\n')}")
        }
        if(!(context.trigger in String)) {
            throw new FilterByContextException("context.trigger must be a String and is a bug introduced by the admin.  Found type: ${context.trigger.getClass()}")
        }
        if(!(context.context in String)) {
            throw new FilterByContextException("context.context must be a String and is a bug introduced by the admin.  Found type: ${context.context.getClass()}")
        }
        if(!(context.metadata in Map)) {
            throw new FilterByContextException("context.metadata must be a Map and is a bug introduced by the admin.  Found type: ${context.metadata.getClass()}")
        }
        context.metadata.each { k, v ->
            if(!(k in String)) {
                throw new FilterByContextException("All context.metadata keys must be a String.  Found type: ${k.getClass()}")
            }
            if(!([String, Boolean].any { v in it })) {
                throw new FilterByContextException("All context.metadata values must be a String or Boolean.  Found type: ${v.getClass()}")
            }
        }
        this.context = context
        this.filters = filters
        this.allowedKeys = (this.context.metadata.keySet().toList() + ['combined', 'inverse', 'never']).sort()
        validateFilters(this.filters)
    }

    public FilterByContext(Map context, String filter) {
        this(context, [[(filter): '/.*/']])
    }

    public FilterByContext(Map context, Map filter) {
        this(context, [filter])
    }


    /**
      Ensure filters are valid for before attempting to process them.
      */
    private void validateFilters(def filter, int depth = 0) throws FilterByContextException {
        if(depth >= maxRecursionDepth) {
            throw new FilterByContextException('When trying to read filters the recursion limit was reached.')
        }
        if(depth == 0) {
            if(!(['pr', 'branch', 'tag'].every { it in allowedKeys})) {
                throw new FilterByContextException('context.metadata must have pr, branch, and tag as a metadata key with an entry.')
            }
        }
        if(filter in List) {
            filter.each { validateFilters(it, depth + 1) }
            return
        }
        if(filter in String) {
            if(!(filter in ((this.context.metadata.keySet().toList() + ['combined', 'inverse', 'never']))))
            return
        }
        if(!(filter in Map)) {
            throw new FilterByContextException("Unknown filter data type has been encountered: ${filter.getClass()}")
        }

        // From this point onward validaing the contents of a filter map.
        List invalidKeys = filter.keySet().toList() - (this.context.metadata.keySet().toList() + ['combined', 'inverse', 'never'])
        if(invalidKeys) {
            throw new FilterByContextException("Unknown filters have been encountered: ${invalidKeys.join(', ')}")
        }
        filter.each { k, v ->
            if(v in String) {
                return
            }
            else if(v in Boolean) {
                return
            }
            else if(v == null) {
                return
            }
            throw new FilterByContextException("Filter key '${k}' must be either a String or Boolean but instead found ${v.getClass()}")
        }
    }

    private Boolean checkEntry(String filterKey, String context, def userExpression) {
        if((this.context.metadata[filterKey] in Boolean) || (userExpression in Boolean)) {
            return (userExpression == (context == k))
        }
        else if(context != k) {
            return false
        }
        else if(userExpression == null) {
            return true
        }
        // String is the only other case
        return isMatched(userExpression, this.context.metadata[filterKey])
    }

    /**
      If the filter is a String then we evaluate the entry based on context.
      */
    private Boolean checkFilter(String filter) {
        if(filter in ['pr', 'branch', 'tag']) {
            return checkEntry(filter, this.context.context, true)
        }
        return checkEntry(filter, this.context.trigger, true)
    }

    /**
      If the filter is a Map then we evaluate all entries based on context.
      */
    private Boolean checkFilter(Map filter) {
        if('never' in filter) {
            return false
        }

        Boolean combined = filter?.combined ?: false
        Boolean inverse = filter?.
        Map results = [:]
        filter.each { k, v ->
            if(k in ['combined', 'inverse']) {
                return
            }
            if(k in ['pr', 'branch', 'tag']) {
                results[k] = checkEntry(k, this.context.context, v)
            }
            else {
                results[k] = checkEntry(k, this.context.trigger, v)
            }
        }
        Boolean result = false
        if(combined) {
            result = results.every { k, v -> v }
        }
        else {
            result = results.any { k, v -> v }
        }
        // Use XOR logic to optionally inverse the result.
        inverse ^ result
    }

    /**
      Evaluate a complex list of filters using recursion.
      */
    private Boolean checkFilter(List filters) {
        if('never' in filters) {
            return false
        }
        Boolean result = false
        Boolean inverse = ('inverse' in filters)
        Boolean combined = ('combined' in filters)
        filters -= ['combined', 'inverse']

        if(combined) {
            result = filters.every {
                if(it in ['combined', 'inverse']) {
                    return true
                }
                checkFilter(it)
            }
        }
        else {
            result = filters.any {
                if(it in ['combined', 'inverse']) {
                    return false
                }
                checkFilter(it)
            }
        }
        // Use XOR logic to optionally inverse the result.
        inverse ^ result
    }

    /**
      Get an expression which will always result in allowBuild returning
      <tt>true</tt>.  This can return any valid object which could be found in
      a parsed YAML object.  Example types could be Map, List, String, Boolean.
      */
    def getAlwaysBuildExpression() {
        ['pr', 'branch', 'tag']
    }

    /**
      Evaluate the full list of complex filters provided by the user and
      provide a single result based on context of how a build was triggered and
      at what point in the Git workflow the job is building (pr, branch, or
      tag).

      @return True if the current environment context evaluates
      */
    Boolean getAllowBuild() {
        checkFilter(this.filters)
    }
}
