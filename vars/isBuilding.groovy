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

import net.gleske.jervis.tools.FilterByContext

/**
  This step implements filtering for different build types (or many at once).

  Examples:
      Check if building any branch, pull request, tag, or a scheduled timer
      build (cron).  Check if 'push' means no other trigger type such as a
      webhook created build by scm push.
          isBuilding('branch')
          isBuilding('pr')
          isBuilding('tag')
          isBuilding('cron')
          isBuilding('manually')
          isBuilding('pr_comment')
          isBuilding('push')

      Abort a pipeline early if it is not a pull request, a matched branch, or
      a tag matching semantic versioning.
          if(!isBuilding(
                  branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/',
                  tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/',
                  pr: null)) {
              // abort the pipeline
              return
          }

      Check if building the main branch.
          isBuilding(branch: 'main')

      Check for main branch or hotfix branches (branch starts with a version
      number and ends with -hotfix)
          isBuilding(branch: '/^\\Qmain\\E$|^[0-9.]+-hotfix$/')

      Check for a tag, but only if it matches semantic versioning.
          isBuilding(tag: '/([0-9]+\\.){2}[0-9]+(-.*)?$/')

      Match pull requests or tags.
          isBuilding(main['pr', 'tag'])

  If the String 'combined' is in the List then it will require that all items
  in the List match the filter.  Otherwise, if any item is true it will return
  true.

  Examples:
      Check for only manually built tags.
          isBuilding(['combined', 'tag', 'manually'])

      Match manually built pull requests or manually built tags.
          isBuilding([['combined', 'tag', 'manually'], ['combined', 'pr', 'manually']])

      Alternate syntax for manually built pull requests or manually built tags.
          isBuilding(['combined', ['pr', 'tag'], 'manually'])

      Alternate syntax for manually built pull requests or manually built tags.
          isBuilding(combined: true, manually: true, branch: false)

          // Above is a HashMap same as the following and is a Groovy DSL
          // feature.
          isBuilding([combined: true, manually: true, branch: false])

  Inverting the expression is also supported.  You can use the 'inverse' option
  to get the opposite result.

  Examples:
      Build a tag or PR.
          isBuilding(['inverse', 'branch'])

      On a pull request, match any build trigger type except for webhook
      pushed.  e.g. matches cron, manually, pr_comment but does not match when
      a user pushes a new commit.
          isBuilding(['combined', 'pr', ['inverse', 'push']])

  @param filters Can be an object for filtering based on build context.  This
                 can be a List of Lists, Strings, and Maps.  It can also be a
                 String or Map.

  @return Returns a String if filters is a String and the context is a String.
          Otherwise returns a boolean result of the evaluated filter.
*/

@NonCPS
def call(def filters) {
    Map context = getBuildContextMap()
    FilterByContext shouldFilter
    if(!filters) {
        shouldFilter = new FilterByContext(context)
    } else {
        shouldFilter = new FilterByContext(context, filters)
    }
    Boolean result = shouldFilter.allowBuild
    if(result && [filters, context.metadata[filters]].every { it in String }) {
        return context.metadata[filters]
    }
    result
}

@NonCPS
void call() {
    throw new Exception('ERROR: isBuilding() step must be called with arguments.  e.g. branch, pr, tag, cron, or manually')
}
