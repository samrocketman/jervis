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
/**
  Returns a list of matrix axes to build.  Based on what the user passes in for
  matrix building.  Filtering optional.

  TWO DIMENSIONAL EXAMPLES MAP:

    Map matrix_axes = [
        'PLATFORM': ['linux', 'mac', 'windows'],
        'BROWSER': ['chrome', 'edge', 'firefox', 'safari']
    ]

  TWO DIMENSIONAL EXAMPLES:

    Complex matrix with filtering for specific axis combinations
        List axes = getMatrixAxes(matrix_axes) { Map axis ->
            !(axis['BROWSER'] == 'safari' && axis['PLATFORM'] == 'linux') &&
            !(axis['BROWSER'] == 'edge' && axis['PLATFORM'] != 'windows')
        }

    Complex matrix with filtering and forcing a user prompt every time.
        List axes = getMatrixAxes(matrix_axes, user_prompt: true) { Map axis ->
            !(axis['BROWSER'] == 'safari' && axis['PLATFORM'] == 'linux') &&
            !(axis['BROWSER'] == 'edge' && axis['PLATFORM'] != 'windows')
        }

    Complex matrix with filtering and only prompting users on manually triggered builds.
        List axes = getMatrixAxes(matrix_axes, user_prompt: isBuilding('manually')) { Map axis ->
            !(axis['BROWSER'] == 'safari' && axis['PLATFORM'] == 'linux') &&
            !(axis['BROWSER'] == 'edge' && axis['PLATFORM'] != 'windows')
        }

  ONE DIMENSIONAL EXAMPLES MAP:
    Map matrix_axes = ['PLATFORM': ['linux', 'mac', 'windows']

  ONE DIMENSIONAL EXAMPLES:
    Get all matrix axes:
        List axes = getMatrixAxes(matrix_axes)

    Get all matrix axes and prompt user to specify all options or specific ones.
        List axes = getMatrixAxes(matrix_axes, user_prompt: true)

    Get all matrix axes and prompt user only on manually triggered builds.
        List axes = getMatrixAxes(matrix_axes, user_prompt: isBuilding('manually'))
  */
@NonCPS
List getMatrixAxes(Map matrix_axes) {
    List axes = []
    matrix_axes.each { axis, values ->
        List axisList = []
        values.each { value ->
            axisList << [(axis): value]
        }
        axes << axisList
    }
    // calculate cartesian product
    axes.combinations()*.sum()
}

Map askMatrixQuestion(boolean userPrompt, Map matrix_axes) {
	Map choices = [:]
	if(userPrompt) {
        stage("Choose combinations") {
            def choice = input(
                id: 'Platform',
                message: 'Customize your matrix build.',
                parameters: matrix_axes.collect { key, options ->
                    choice(
                        choices: ['all'] + options.sort(),
                        description: "Choose a single ${key.toLowerCase()} or all to run tests.",
                        name: key)
                })
            if(choice instanceof String) {
                // input step will return a simple String instead of a Map if
                // there's only one choice (i.e. one matrix axis).
                choices[matrix_axes.keySet().first()] = choice
            }
            else {
                choices = choice
            }
        }
    }
    else {
        // no user prompt so default to 'all' choices for every matrix
        // dimension
        matrix_axes.keySet().each { String key ->
            choices[key] = 'all'
        }
    }
    choices
}

List<Map> call(Map user_settings = [:], Map matrix_axes, Closure c) {
    Map response = askMatrixQuestion(user_settings.get('user_prompt', false) as Boolean, matrix_axes)
    List<Map> axes = getMatrixAxes(matrix_axes).findAll { Map axis ->
        // apply user filter or default to all if no user filter is provided
        // via closure
        c(axis) &&
        response.every { key, choice ->
            choice == 'all' || choice == axis[key]
        }
    }
    // return the list of matrix axes
    axes
}
List<Map> call(Map user_settings = [:], Map matrix_axes) {
    call(user_settings, matrix_axes) {
        true
    }
}
