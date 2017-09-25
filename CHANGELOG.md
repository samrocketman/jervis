# Next release

- New Feature: `net.gleske.jervis.lang.*` is serializable so Jervis can now be
  used in pipeline scripts.
- Added an example pipeline library to `resources/` and `vars/`.

# jervis 0.13

Migrating Job DSL scripts from jervis 0.12 to future proof:

- In the root `_jervis_generator` job, the typical DSL Scripts path is
  `jobs/**/*.groovy`.  `firstjob_dsl.groovy` is no longer the only script under
  the `jobs/` folder.  The DSL Scripts path must now specify
  `jobs/firstjob_dsl.groovy`.
- It is recommended [migrate `lifecycleGenerator.setPrivateKeyPath(str)` to
  `lifecycleGenerator.setPrivateKey(new File(str).text)`][mig-01-ex] because
  `setPrivateKeyPath()` is deprecated.
- Instead of setting `securityIO.id_rsa_priv` with a file path, it is better to
  call `securityIO.setKey_pair()` because `id_rsa_priv` is deprecated.

The following methods are now deprecated and should not be used.  They will be
removed in the next release.

```
net.gleske.jervis.lang.lifecycleGenerator.setPrivateKeyPath()
net.gleske.jervis.tools.securityIO.checkPath(String path)
net.gleske.jervis.tools.securityIO.default_key_size
net.gleske.jervis.tools.securityIO.generate_rsa_pair()
net.gleske.jervis.tools.securityIO.generate_rsa_pair(String priv_key_file_path, String pub_key_file_path, int keysize)
net.gleske.jervis.tools.securityIO.id_rsa_priv
net.gleske.jervis.tools.securityIO.id_rsa_pub
net.gleske.jervis.tools.securityIO.securityIO(String path)
net.gleske.jervis.tools.securityIO.securityIO(String path, int keysize)
net.gleske.jervis.tools.securityIO.securityIO(String priv_key_file_path, String pub_key_file_path, int keysize)
net.gleske.jervis.tools.securityIO.securityIO(int keysize)
net.gleske.jervis.tools.securityIO.set_vars(String priv_key_file_path, String pub_key_file_path, int keysize)
```

Features and bugfixes:

- Feature: The YAML key `jenkins -> secrets` can now be a simple `Map` instead
  of a list of maps.  Both of the following are supported:
  ```yaml
  jenkins:
    secrets:
      - key: super_secret
        secret: <ciphertext>
  ```
  ```yaml
  jenkins:
    secrets:
      super_secret: <ciphertext>
  ```
- Improvement: Encryption and decryption now occur in the JVM runtime instead of
  forking an `openssl` cli process.
- Bugfix: since switching to bouncycastle, unit tests no longer throw
  `closeWithWarning()` warnings.

Job DSL script changes in `jobs/` folder:

- Support matrix building by integrating with the [Groovy label assignment
  plugin][gla-plugin].
- Load GitHub token from credentials plugin.
- Reorganize Job DSL scripts into separate files.  This makes them more readable
  and composable by taking advantage of groovy bindings.
- `jobs/get_folder_credentials.groovy` now makes use of the [Bouncy Castle API
  Plugin][bca-plugin] to decrypt private keys.  AES encryption is now supported
  in private keys.
- Some URLs now hyperlink.


# jervis 0.12

- Enhancement: YAML containing a string of `'false'` should evaluate to boolean
  `false`.  [See issue #90][#90]

# jervis 0.11

- Enhancement: cleanup added to Toolchains Specification.  [See issue #61][#61]
- New feature: additional labels can be specified.  [See issue #87][#87]
- New feature: additional toolchains can be set up.  [See issue #87][#87]
- Bugfix `null` from blank lines in sections.  [See issue #88][#88]
- Additional bug fixes.
- Cleaned up `firstjob_dsl.groovy` removing deprecated methods.  Removed
  credentials definition from folder because Job DSL plugin fixed it.

# jervis 0.10

- Bugfix when a toolchain is a number an unsupported toolchain exception is
  thrown.  [See issue #85][#85]
- Enhancement: Better matrix support for toolchains.  Now, any toolchain can be
  designated an `advanced` matrix or matrix support can be entirely `disabled`.
  The traditional behavior is known as `simple` matrix.  [See issue #84][#84]

Note: Edit your `toolchains.json` file and add `matrix: advanced` to the `env`
toolchain.  As a migration path, an exception will now be thrown if `env` does
not declare the type of matrix.  [See wiki for details][wiki-toolchains-spec].

# jervis 0.9

- Bugfix Exception number of constructors during runtime do not match by
  converting Groovy exceptions to Java.  [See issue #82][#82].

# jervis 0.8

- Bugfix NPE when Yaml returns null.  [See issue #80][#80]

# jervis 0.7

- Bugfix urlencoding references.  This improves fetching branches with special
  characters.  [See issue #77][#77]
- Bugfix getObjectValue String vs Map.  [See issue #78][#78]
- Make more use of lifecycleGenerator.generateSection() method.  Fewer unit
  tests are required.
- Upgrade ASM to 5.1 so all dependencies are up-to-date.
- The changes in this version makes it easy to use the [Collapsing Console
  Sections Plugin][ccs-plugin] for Jenkins.  This visually creates sections.
  e.g.
  * Section name: `{1}`
  * Section starts with: `^\# ([^ ]+ [^ ]+)$`
  * Section ends with: `^\$ set \+x$`

# jervis 0.6

- `GitHub.fetch()` function is now public and supported as an API.
- `GitHub.isUser()` function is a new API function which checks if a user is in
  fact a user or an organization.
- Bugfix mock for GitHubTest class not properly throwing a 404 on missing files.

# jervis 0.5.2

- Bugfix IncompatibleClassChangeError exception on JDK7

# jervis 0.5.1

- Bugfix a blank yaml key causing the library to throw an exception.

# jervis 0.5

- Support for Mac OS X.
- Support for building with Java 1.8.
- Upgrade Gradle to 2.11.
- securityIO unit testing has been cleaned up.
- General improvements to securityIO class.
- Force Java 1.6 byte code so cobertura reports are accurate for all versions of
  groovy.

# jervis 0.4

- Better support for secure fields (encrypted values in YAML files).  [See issue
  #64][#64].
- Support for four new languages: `c`, `cpp`, `go`, and `node_js`.

# jervis 0.3

- Implement friendly matrix labels which allow Jenkins matrix jobs to have
  recognizable labels for matrix build project types.  [See issue #70][#70]
- Multi-OS support.  Toolchains and lifecycles files can be referenced in Job
  DSL scripts by platform and operating system.  [See issue #68][#68]

# jervis 0.2

- Renamed Java package from `jervis` to `net.gleske.jervis`.

# jervis 0.1

- Supported languages: `groovy`, `java`, `ruby`, and `python`.
- Matrix build support.
- RSA encrypted secure properties.
- Fully generated `groovydoc`.
- At least 80% test coverage.

[#61]: https://github.com/samrocketman/jervis/issues/61
[#64]: https://github.com/samrocketman/jervis/issues/64
[#68]: https://github.com/samrocketman/jervis/issues/68
[#70]: https://github.com/samrocketman/jervis/issues/70
[#77]: https://github.com/samrocketman/jervis/issues/77
[#78]: https://github.com/samrocketman/jervis/issues/78
[#80]: https://github.com/samrocketman/jervis/issues/80
[#82]: https://github.com/samrocketman/jervis/issues/82
[#84]: https://github.com/samrocketman/jervis/issues/84
[#85]: https://github.com/samrocketman/jervis/issues/85
[#87]: https://github.com/samrocketman/jervis/issues/87
[#88]: https://github.com/samrocketman/jervis/issues/88
[#90]: https://github.com/samrocketman/jervis/issues/90
[bca-plugin]: https://wiki.jenkins.io/display/JENKINS/Bouncy+Castle+API+Plugin
[ccs-plugin]: https://wiki.jenkins-ci.org/display/JENKINS/Collapsing+Console+Sections+Plugin
[gla-plugin]: https://wiki.jenkins.io/display/JENKINS/Groovy+Label+Assignment+plugin
[mig-01-ex]: https://github.com/samrocketman/jervis/commit/1d7ff1417c642d959f467c11eca7b16cb3e3ef3c
[wiki-toolchains-spec]: https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file
