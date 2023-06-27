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
/*
   If a user has defined collecting results in their YAML then this stage will
   publish them.
 */

import net.gleske.jervis.lang.LifecycleGenerator
import net.gleske.jervis.lang.PipelineGenerator

/**
  Process default publishable items provided by this script.
 */
def processDefaultPublishable(def item, String publishable) {
    switch(publishable) {
        case 'artifacts':
            archiveArtifacts artifacts: item['path'], fingerprint: true,
                             excludes: item['excludes'],
                             allowEmptyArchive: item['allowEmptyArchive'],
                             defaultExcludes: item['defaultExcludes'],
                             caseSensitive: item['caseSensitive']
            break
        case 'cobertura':
            cobertura coberturaReportFile: item['path'],
                      autoUpdateHealth: item['autoUpdateHealth'],
                      autoUpdateStability: item['autoUpdateStability'],
                      failNoReports: item['failNoReports'],
                      failUnhealthy: item['failUnhealthy'],
                      failUnstable: item['failUnstable'],
                      maxNumberOfBuilds: item['maxNumberOfBuilds'],
                      onlyStable: item['onlyStable'],
                      sourceEncoding: item['sourceEncoding'],
                      zoomCoverageChart: item['zoomCoverageChart'],
                      methodCoverageTargets: item['methodCoverageTargets'],
                      lineCoverageTargets: item['lineCoverageTargets'],
                      conditionalCoverageTargets: item['conditionalCoverageTargets']
            break
        case 'html':
            publishHTML allowMissing: item['allowMissing'],
                        alwaysLinkToLastBuild: item['alwaysLinkToLastBuild'],
                        includes: item['includes'],
                        keepAll: item['keepAll'],
                        reportDir: item['path'],
                        reportFiles: item['reportFiles'],
                        reportName: item['reportName'],
                        reportTitles: item['reportTitles']
            break
        case 'junit':
            junit allowEmptyResults: item['allowEmptyResults'],
                  healthScaleFactor: item['healthScaleFactor'],
                  keepLongStdio: item['keepLongStdio'],
                  testResults: item['path']
            break
    }
}

def call(LifecycleGenerator generator, PipelineGenerator pipeline_generator) {
    List publishableItems = pipeline_generator.publishableItems
    if(publishableItems) {
        stage("Publish results") {
            //unstash and publish in parallel
            Map tasks = [failFast: true]
            for(String publishable : publishableItems) {
                // This "publish" variable is required because of how CPS
                // strangely behaves with tasks and looping.  Without it,
                // pipeline makes publishable null in tasks oddly.
                String publish = publishable
                tasks["${publish}"] = {
                    try {
                        unstash publish
                        processDefaultPublishable(pipeline_generator.getPublishable(publish), publish)
                    }
                    catch(e) {
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
            parallel(tasks)
        }
    }
}
