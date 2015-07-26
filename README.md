# Jervis - [![Build Status][status-build]][jervis-travis] [![Coverage Status][status-coverage]][jervis-coveralls] [![Dependency Status][status-versioneye]][jervis-versioneye]

* *Project status:* released.
* *Currently Targeted platforms:* Linux

Jervis is a combination of some letters in the words Jenkins and Travis: JEnkins
tRaVIS.  [Jenkins][jenkins] is a [continuous integration][wiki-ci] tool which is
typically installed on premises.  [Travis][travis] is a hosted, distributed
continuous integration system used by many [open source][wiki-os] projects.
Both Jenkins and Travis have paid and enterprise offerings.

Jervis uses Travis-like job generation using the [Job DSL
plugin][jenkins-plugin-job-dsl] and groovy scripts.  It reads the `.jervis.yml`
file of a project and generates a job in Jenkins based on it.  If `.jervis.yml`
doesn't exist then it will fall back to using the [`.travis.yml`][travis-yaml]
file.

For development planning and other documentation see the [Jervis
wiki][jervis-wiki].  If you wish to stay up to date with the latest Jervis news
then please feel free to [watch this repository][watch-repo] because I use the
issue tracking and wiki for planning.

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

Refer to the [Jervis Wiki][jervis-wiki] for a quickstart guide and rolling
Jervis out for production.

The library API is also fully documented.  To generate the docs do the
following.

    ./gradlew groovydoc

The documentation can be found in `build/docs/groovydoc`.

# Set up

To include this library for use in your Job DSL plugin scripts you only need
include it in your build tool.

#### Maven

```xml
<dependency>
  <groupId>net.gleske</groupId>
  <artifactId>jervis</artifactId>
  <version>0.2</version>
  <type>pom</type>
</dependency>
```

#### Gradle

```
dependencies {
    libs 'org.yaml:snakeyaml:1.15'
    libs 'net.gleske:jervis:0.2'
}

task libs(type: Copy) {
    into 'lib'
    from configurations.libs
}
```

Then execute `./gradlew libs` to assemble dependencies into the `lib` directory
of the Jenkins workspace.  Don't forget to add `lib` to the classpath.

# License

    Copyright 2014-2015 Sam Gleske

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[jenkins]: https://jenkins-ci.org/
[jenkins-plugin-job-dsl]: https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin
[jervis-coveralls]: https://coveralls.io/r/samrocketman/jervis?branch=master
[jervis-travis]: https://travis-ci.org/samrocketman/jervis
[jervis-versioneye]: https://www.versioneye.com/user/projects/54f2a1cc4f3108959a0007f1
[jervis-wiki]: https://github.com/samrocketman/jervis/wiki
[milestone-progress]: https://github.com/samrocketman/jervis/milestones
[status-build]: https://travis-ci.org/samrocketman/jervis.svg?branch=master
[status-coverage]: https://coveralls.io/repos/samrocketman/jervis/badge.svg?branch=master
[status-versioneye]: https://www.versioneye.com/user/projects/54f2a1cc4f3108959a0007f1/badge.svg?style=flat
[travis]: https://travis-ci.org/
[travis-yaml]: http://docs.travis-ci.com/user/build-configuration/
[watch-repo]: https://help.github.com/articles/watching-repositories/
[wiki-ci]: https://en.wikipedia.org/wiki/Continuous_integration
[wiki-os]: http://en.m.wikipedia.org/wiki/Open_source
