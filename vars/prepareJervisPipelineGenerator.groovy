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

import net.gleske.jervis.lang.PipelineGenerator

/**
  Define supported report collection by offloading most of the logic work to
  PipelineGenerator class.
 */
@NonCPS
void initializeGenerator(PipelineGenerator pipeline_generator) {
    // what collections are supported?
    pipeline_generator.supported_collections = [
        'artifacts',
        'cobertura',
        'html',
        'junit'
    ]
    // supporting optional list or string for filesets in default settings
    pipeline_generator.collect_settings_filesets = [
        artifacts: ['excludes'],
        html: ['includes']
    ]
    // defining default settings for supported publishers
    pipeline_generator.collect_settings_defaults = [
        artifacts: [
            allowEmptyArchive: false,
            caseSensitive: true,
            defaultExcludes: true,
            excludes: '',
            skip_on_pr: true
        ],
        cobertura: [
            autoUpdateHealth: false,
            autoUpdateStability: false,
            failNoReports: false,
            failUnhealthy: false,
            failUnstable: false,
            maxNumberOfBuilds: 0,
            onlyStable: false,
            sourceEncoding: 'ASCII',
            zoomCoverageChart: false,
            methodCoverageTargets: '80, 0, 0',
            lineCoverageTargets: '80, 0, 0',
            conditionalCoverageTargets: '70, 0, 0'
        ],
        html: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            includes: '**/*',
            keepAll: false,
            reportFiles: 'index.html',
            reportName: 'HTML Report',
            reportTitles: ''
        ],
        junit: [
            allowEmptyResults: false,
            healthScaleFactor: 1.0,
            keepLongStdio: false
        ]
    ]

    // stashes for HTML publisher need to be formatted in a way that report
    // collection can be stashed automatically across agents
    pipeline_generator.stashmap_preprocessor = [
        html: { Map settings ->
            settings['includes']?.tokenize(',').collect {
                "${settings['path']  -~ '/$' -~ '^/'}/${it}"
            }.join(',').toString()
        }
    ]

    // We need regex validation of specific jenkins.collect setings.
    // For cobertura:
    //   If a user fails the input validation, then it should fall back to the
    //   default option.
    // For HTML:
    //   If an invalid path is specified for HTML publisher then, do not
    //   attempt to collect.
    String cobertura_targets_regex = '([0-9]*\\.?[0-9]*,? *){3}[^,]$'
    pipeline_generator.collect_settings_validation = [
        cobertura: [
            methodCoverageTargets: cobertura_targets_regex,
            lineCoverageTargets: cobertura_targets_regex,
            conditionalCoverageTargets: cobertura_targets_regex
        ],
        html: [
            path: '''^[^,\\:*?"'<>|]+$'''
        ]
    ]
}

@NonCPS
void call(PipelineGenerator pipeline_generator) {
    initializeGenerator(pipeline_generator)
}
