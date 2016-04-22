# jervis 0.7

* Bugfix urlencoding references.  This improves fetching branches with special
  characters.  [See issue #77][#77]
* Bugfix getObjectValue String vs Map.  [See issue #78][#78]
* Make more use of lifecycleGenerator.generateSection() method.  Fewer unit
  tests are required.
* Upgrade ASM to 5.1 so all dependencies are up-to-date.
* The changes in this version makes it easy to use the [Collapsing Console
  Sections Plugin][ccs-plugin] for Jenkins.  This visually creates sections.
  e.g.
  * Section name: `{1}`
  * Section starts with: `^\# ([^ ]+ [^ ]+)$`
  * Section ends with: `^\$ set \+x$`

# jervis 0.6

* `GitHub.fetch()` function is now public and supported as an API.
* `GitHub.isUser()` function is a new API function which checks if a user is in
  fact a user or an organization.
* Bugfix mock for GitHubTest class not properly throwing a 404 on missing files.

# jervis 0.5.2

* Bugfix IncompatibleClassChangeError exception on JDK7

# jervis 0.5.1

* Bugfix a blank yaml key causing the library to throw an exception.

# jervis 0.5

* Support for Mac OS X.
* Support for building with Java 1.8.
* Upgrade Gradle to 2.11.
* securityIO unit testing has been cleaned up.
* General improvements to securityIO class.
* Force Java 1.6 byte code so cobertura reports are accurate for all versions of
  groovy.

# jervis 0.4

* Better support for secure fields (encrypted values in YAML files).  [See issue
  #64][#64].
* Support for four new languages: `c`, `cpp`, `go`, and `node_js`.

# jervis 0.3

* Implement friendly matrix labels which allow Jenkins matrix jobs to have
  recognizable labels for matrix build project types.  [See issue #70][#70]
* Multi-OS support.  Toolchains and lifecycles files can be referenced in Job
  DSL scripts by platform and operating system.  [See issue #68][#68]

# jervis 0.2

* Renamed Java package from `jervis` to `net.gleske.jervis`.

# jervis 0.1

* Supported languages: `groovy`, `java`, `ruby`, and `python`.
* Matrix build support.
* RSA encrypted secure properties.
* Fully generated `groovydoc`.
* At least 80% test coverage.

[#64]: https://github.com/samrocketman/jervis/issues/64
[#68]: https://github.com/samrocketman/jervis/issues/68
[#70]: https://github.com/samrocketman/jervis/issues/70
[#77]: https://github.com/samrocketman/jervis/issues/77
[#78]: https://github.com/samrocketman/jervis/issues/78
[ccs-plugin]: https://wiki.jenkins-ci.org/display/JENKINS/Collapsing+Console+Sections+Plugin
