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

package net.gleske.jervis.tools

import static net.gleske.jervis.tools.AutoRelease.isMatched
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

<pre><code>
Map context = [
    trigger: '',
    context: '',
    metadata: [:]
]
</code></pre>

  Valid values for trigger include: manually, cron, pr_comment, push.

  Valid values for context include: pr, branch, tag.

  The following HashMap is the supported keys.  You can see an example of this
  in the
  <a target=_blank href="https://github.com/samrocketman/jervis/blob/main/vars/getBuildContextMap.groovy"><tt>getBuildContextMap()</tt></a>
  Jenkins pipeline step.
<pre><code>
Map context = [
    trigger: 'push',
    context: 'pr',
    metadata: [
        pr: false,
        branch: ''
        tag: '',
        push: false,
        cron: false,
        manually: '',
        pr_comment: ''
    ]
]
</code></pre>

  The metadata values are defined as:

  <ul>
    <li>
      <tt>pr</tt>: A <tt>Boolean</tt>.  <tt>true</tt> if the current build is happening
      from a pull request.
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
      <tt>push</tt>: A <tt>Boolean</tt>.  A build by webhook or any other event
      which does not match other trigger types. Examples of other event types
      include: opening a pull request, pushing a tag.
    </li>
    <li>
      <tt>cron</tt>: A <tt>Boolean</tt>.  A periodic build by schedule
      configured in job triggers.  It is redundant with the <tt>trigger</tt>
      root key being set to <tt>cron</tt> and is not normally read during
      filtering.
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

  <h2>Sample usage</h2>
  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.tools.FilterByContext

Map context = [
    trigger: 'push',
    context: 'pr',
    metadata: [
        push: true,
        pr: true,
        branch: '',
        tag: ''
    ]
]

def filters = 'pr'
FilterByContext filterShould = new FilterByContext(context, filters)

// returns true because it is a pull request build
filterShould.allowBuild
</code></pre>
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
      These filters will force the getAllowBuild method to always return true.

      Default value is the following.
<pre><code class="language-groovy">
['pr', 'branch', 'tag']
</code></pre>
      */
    private static List alwaysAllowFilters = ['pr', 'branch', 'tag']

    /**
      It is an error to instantiate this class without a build environment
      context and a user-provided filter.
      */
    public FilterByContext() {
        throw new FilterByContextException('Must provide a build environment context and a user-provided filter.  This is likely a bug introduced by the admin and should be fixed by admin.')
    }

    /**
      Instantiate a default FilterByContext instance which will always allow
      the build to proceed.

      @see #alwaysAllowFilters
      @param context A context map defining the current state of the build
                     environment and how it was triggered.
      */
    public FilterByContext(Map context) {
        this(context, alwaysAllowFilters)
    }

    /**
      Instantiate a context and provide filters for potentially skipping builds
      depending on context.

      @param context A context map defining the current state of the build
                     environment and how it was triggered.
      @param filters A list of filters.  A filter might be a String, Map, List,
                     or a mix of the three.
      */
    public FilterByContext(Map context, List filters) {
        List requiredArgs = ['trigger', 'context', 'metadata'] - context.keySet().toList()
        if(requiredArgs) {
            throw new FilterByContextException("Context is missing required keys provided by admin: ${requiredArgs.join(', ')}")
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
        List allowedKeys = (context.metadata.keySet().toList() + ['combined', 'inverse', 'never']).sort()
        if(!(['pr', 'branch', 'tag'].every { it in allowedKeys})) {
            throw new FilterByContextException('context.metadata must have pr, branch, and tag as a metadata key with an entry.')
        }
        this.context = context
        this.allowedKeys = allowedKeys
        setFilters(filters)
    }

    public FilterByContext(Map context, String filter) {
        this(context, [filter])
    }

    public FilterByContext(Map context, Map filter) {
        this(context, [filter])
    }

    /**
      Sets a filter to be evaluated against a context.  Provides filter
      validation and throws an exception if validation fails.
      */
    public void setFilters(def filters) throws FilterByContextException {
        this.filters = (filters in List) ? filters : [filters]
        validateFilters(this.filters)
    }


    /**
      Ensure filters are valid for before attempting to process them.
      */
    private void validateFilters(def filter, int depth = 0) throws FilterByContextException {
        if(depth > maxRecursionDepth) {
            throw new FilterByContextException('When trying to read filters the recursion limit was reached.')
        }
        if(filter in List) {
            filter.each { validateFilters(it, depth + 1) }
            return
        }
        if(filter in String) {
            if(!(filter in allowedKeys)) {
                throw new FilterByContextException("Unknown filter encountered.  Found ${filter} but must be one of the following: ${allowedKeys.join(', ')}")
            }
            return
        }
        if(!(filter in Map)) {
            throw new FilterByContextException("Unknown filter data type has been encountered: ${filter.getClass()}")
        }

        // From this point onward validaing the contents of a filter map.
        List invalidKeys = filter.keySet().toList() - allowedKeys
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
        if(userExpression == null) {
            return (context == filterKey)
        }
        else if((this.context.metadata[filterKey] in Boolean) || (userExpression in Boolean)) {
            return (userExpression == (context == filterKey))
        }
        else if(context != filterKey) {
            return false
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
        if('never' in filter.keySet()) {
            return false
        }

        Boolean combined = filter?.combined ?: false
        Boolean inverse = filter?.inverse ?: false
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
                if(it == 'inverse') {
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
    static List getAlwaysBuildExpression() {
        return alwaysAllowFilters
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

    /**
      Evaluate the full list of filters provided as an argument instead of
      relying on the <tt>{@link #filters}</tt> property.

      @param filters A list of user-provided filters where a filter can be a
                     String, Map, or a List (containing a list of objects of
                     type List, String, or Map therein).
      @return True if the current environment <tt>{@link #context}</tt>
              evaluates against the provided filters.
      */
    Boolean allowBuild(def filters) {
        List providedFilters = (filters in List) ? filters : [filters]
        validateFilters(providedFilters)
        checkFilter(providedFilters)
    }
}
