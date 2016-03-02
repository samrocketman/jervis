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
