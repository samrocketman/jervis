# About Jervis

* What is Jervis? A library for [Job DSL plugin][jenkins-plugin-job-dsl]
  scripts and [shared Jenkins pipeline libraries][pipeline].  It is used to
  augment the automation of generating Jenkins jobs.
* What is Jervis not?  Jervis is not a Jenkins plugin.
* See an [example Job DSL script using Jervis][dsl-example].

Jervis uses Travis-like job generation using the [Job DSL
plugin][jenkins-plugin-job-dsl], [shared Jenkins pipeline libraries][pipeline],
and groovy scripts.  It reads the `.jervis.yml` file of a project and generates
a job in Jenkins based on it.  If `.jervis.yml` doesn't exist then it will fall
back to using the [`.travis.yml`][travis-yaml] file.

For development planning and other documentation see the [Jervis
wiki][jervis-wiki] and [developer api documentation][jervis-api].  If you wish
to stay up to date with the latest Jervis news then please feel free to [watch
this repository][watch-repo] because I use the issue tracking and wiki for
planning.

## Why Jervis?

What is Jervis attempting to scale?  Let's talk about some scale bottlenecks
that have been overcome by Jenkins (formerly Hudson) and its community.

The scaling issue is a main bullet. The solution for the issue is in a
sub-bullet.

* Developers are challenged with building often and even deploying often.
  * Jenkins was invented.
* Jenkins infrastructure is strained when too many slaves are in one master and
  too many jobs are queued up on a daily basis.  A single master struggles to
  perform all requested builds in a timely manner.  Jenkins also suffers from
  single point of failure as a lone master.
  * Multi-master Jenkins was invented. This provides redundancy for the master.
    Throughput for daily build capacity is improved.
* Jenkins jobs suffer from a lot of duplicate code.  It is difficult to fix a
  bug in one job and have it propagate to other jobs.
  * Jenkins Job DSL plugin was invented.  Configuration through code is now
    possible.  Multiple jobs can be generated and regenerated with the same code
    using templates in a domain specific language.
* Onboarding new projects in a Jenkins installation can be difficult.  Typically
  engineers will get together and discuss the needs of the project and then
  configure a Jenkins job for the needs of the project.  For enterprises with a
  very large number of projects it is typically hard to scale number of build
  engineers to match with the large number of projects which require onboarding
  into the build ecosystem.
  * Jervis is being invented.  Job generation through convention over
    configuration.  Scaling the onboarding for a project by creating and abiding
    by conventions in how jobs are generated.  This is for large scale job
    generation and project onboarding.  Jervis is taking lessons learned from a
    seasoned build engineer and attempting to fill this gap in the Jenkins
    ecosystem.

# Documentation

* Quickstart: [jenkins-bootstrap-jervis][jbj]
* [Jervis Wiki][jervis-wiki]
  * [Build Overview][jervis-wiki-overview]
  * [Supported Languages][jervis-wiki-langs]
  * [Supported Build Tools][jervis-wiki-tools]

Refer to the [Jervis Wiki][jervis-wiki] for a quickstart guide and rolling
Jervis out for production.  There's also the [jenkins-bootstrap-jervis][jbj]
project to get up and running quickly.  For developing Job DSL scripts refer
to the [Jervis API developer documentation][jervis-api].

# License

    Copyright 2014-2020 Sam Gleske

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[dsl-example]: https://github.com/samrocketman/jervis/blob/master/jobs/firstjob_dsl.groovy
[jbj]: https://github.com/samrocketman/jenkins-bootstrap-jervis
[jenkins]: https://jenkins-ci.org/
[jenkins-plugin-job-dsl]: https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin
[jervis-api]: http://sam.gleske.net/jervis-api/
[jervis-coveralls]: https://coveralls.io/r/samrocketman/jervis?branch=master
[jervis-travis]: https://travis-ci.org/samrocketman/jervis
[jervis-versioneye]: https://www.versioneye.com/user/projects/54f2a1cc4f3108959a0007f1
[jervis-wiki]: https://github.com/samrocketman/jervis/wiki
[jervis-wiki-overview]: https://github.com/samrocketman/jervis/wiki/Build-overview
[jervis-wiki-langs]: https://github.com/samrocketman/jervis/wiki/Supported-Languages
[jervis-wiki-tools]: https://github.com/samrocketman/jervis/wiki/Supported-Tools
[milestone-progress]: https://github.com/samrocketman/jervis/milestones
[pipeline]: https://jenkins.io/doc/book/pipeline/shared-libraries/
[status-build]: https://travis-ci.org/samrocketman/jervis.svg?branch=master
[status-coverage]: https://coveralls.io/repos/samrocketman/jervis/badge.svg?branch=master
[status-versioneye]: https://www.versioneye.com/user/projects/54f2a1cc4f3108959a0007f1/badge.svg?style=flat
[travis]: https://travis-ci.org/
[travis-yaml]: http://docs.travis-ci.com/user/build-configuration/
[watch-repo]: https://help.github.com/articles/watching-repositories/
[wiki-ci]: https://en.wikipedia.org/wiki/Continuous_integration
[wiki-os]: http://en.m.wikipedia.org/wiki/Open_source
