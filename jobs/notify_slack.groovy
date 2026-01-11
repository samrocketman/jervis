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
/*
   Send a slack notification to a channel from Job DSL scripts in Jervis.  This
   is a portable, self-contained, binding which has no dependencies on other
   bindings.

   Requirements:
       - The slack plugin is required.
       - Assumes Job DSL is being generated from Freestyle job type.
       - Load into your job DSL script one of the two ways:
             evaluate(readFileFromWorkspace('path/to/notify_slack.groovy'))
             evaluate(tryReadFile('jobs/notify_slack.groovy'))

   Valid slack colors documented at:
       https://plugins.jenkins.io/slack/#plugin-content-colors

   Minimal usage:
       notify_slack(channel: '#some_channel,#another_channel', message: 'Some message')

   Full usage including optional options:
       notify_slack(
           channel: '#some_channel',
           message: 'Some message',
           color: 'good',
           url_msg: 'Open build that created this message')
   */
import jenkins.model.Jenkins
//import jenkins.plugins.slack.SlackNotifier
//import jenkins.plugins.slack.StandardSlackService

notify_slack = null
notify_slack = { Map options ->
    // option parsing
    List message = []
    if(options.message) {
        message << options.message
    }
    String channels = options.channel ?: ''
    String url_msg = options.url_msg
    if(!url_msg) {
        String jobDisplayName = Jenkins.instance.getItemByFullName(JOB_NAME).displayName
        url_msg = "Open ${jobDisplayName} build"
    }
    String color = options.color ?: 'good'
    if(!(color in ['good', 'warning', 'danger']) && !color.startsWith('#')) {
        String oldColor = color
        color = 'warning'
        message += [
            '',
            "> Note: Slack message color was changed from `${oldColor}` to `${color}`.",
            '> See <https://plugins.jenkins.io/slack/#plugin-content-colors|Slack Plugin documentation> for valid colors.',
            ''
        ]
    }
    message << "(<${BUILD_URL}console|${url_msg}>)"

    // notify slack
    def notifier = Jenkins.instance.getExtensionList('jenkins.plugins.slack.SlackNotifier$DescriptorImpl')[0]
    def publisher = Jenkins.instance.pluginManager.uberClassLoader.loadClass('jenkins.plugins.slack.StandardSlackService')
        .constructors.find {
            Boolean found = true
            List desiredTypes = [String, String, String, String, Boolean, String]*.simpleName*.toLowerCase()
            it.parameterTypes.toList().with { typeList ->
                if(typeList.size() != desiredTypes.size()) {
                    found = false
                    return
                }
                typeList.eachWithIndex { item, index ->
                    found &= item.simpleName.toLowerCase() in desiredTypes[index]
                }
            }
            found
        }.newInstance(notifier.baseUrl,
                      notifier.teamDomain,
                      '',
                      notifier.tokenCredentialId,
                      notifier.botUser,
                      channels)
    publisher.publish(message.join('\n'), color)
}
