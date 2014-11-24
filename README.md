# Jervis - [![Build Status][status-build]][travis-jervis]

Jervis is a combination of some letters in the words Jenkins and Travis: JEnkins
tRaVIS.  [Jenkins][jenkins] is a [continuous integration][wiki-ci] tool which is
typically installed on premises.  [Travis][travis] is a hosted, distributed
continuous integration system used by many [open source][wiki-os] projects.
Both Jenkins and Travis have paid and enterprise offerings.

Jervis uses Travis-like job generation using the [Job DSL
plugin][jenkins-plugin-job-dsl] and groovy scripts.  It reads the
[.travis.yml][travis-yaml] file of a project and generates a job in Jenkins
based on it.

[jenkins]: https://jenkins-ci.org/
[jenkins-plugin-job-dsl]: https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin
[status-build]: https://travis-ci.org/samrocketman/jervis.svg?branch=master
[travis]: https://travis-ci.org/
[travis-jervis]: https://travis-ci.org/samrocketman/jervis
[travis-yaml]: http://docs.travis-ci.com/user/build-configuration/
[wiki-ci]: https://en.wikipedia.org/wiki/Continuous_integration
[wiki-os]: http://en.m.wikipedia.org/wiki/Open_source
