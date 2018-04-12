# jervis 1.1

#### Warnings

Job DSL Scripts: Freestyle will no longer be updated.  Only pipeline jobs are
supported.  This will not overwrite freestyle jobs.  It will simply not touch
them.  The following migration path is recommended:

- If you wish to preserve build history, then just disable all freestyle and
  matrix project jobs from running.  Run [disable-freestyle-jobs][fs-migrate-1].
- Permanently delete all disabled freestyle and matrix project jobs can be done
  after disabling them.  Review what was disabled before running
  [delete-freestyle-jobs][fs-migrate-2] Script Console script.

#### New features:

- Admins can now define user input validation (or fall back to a default) on
  `jenkins.collect` publishers.  For more complicated string input for settings
  which requires a specific format.  This is to optionally protect users from
  accidentally defining an incorrect setting and breaking their builds.

#### Bug fixes:

- Fixes critical bug where users who do not define collecting any artifacts in
  YAML will cause their job to fail to build.
- Fixes critical bug where admins who define default settings as a fileset and a
  user does not customize it, causes an invalid value to be set as the default
  instead of the proper default.

#### Job DSL scripts changes in the `jobs/` folder:

- Since pipelines are now fully supported the configuring views for jobs no
  longer make sense.  Configuring views has been removed.
- Bugfix branch filters not working.
- `Jenkinsfile` is no longer referenced in the repository.  It is loaded by
  Jervis pipeline DSL scripts.  Jenkins jobs now load a default `Jenkinsfile`
  provided by the plugins [Multibranch: Pipeline with defaults][mpwd] and
  [Config File Provider][cfp].

[mpwd]: https://plugins.jenkins.io/pipeline-multibranch-defaults
[cfp]: https://plugins.jenkins.io/config-file-provider

#### Pipeline DSL scripts changes in the `vars/` folder:

- Feature: failed unit tests are now properly exposed.
- Feature: Cobertura report collection can now be customized for enforcement.
- Feature: JUnit report collection can now be fully customized.
- Bugfix: Pull request builds no longer worked after upgrading plugins.  This is
  now fixed.

# jervis 1.0 - Oct 30th, 2017

#### Warnings

The major version was bumped because of API breaking changes.  Be sure to fully
test this release before rolling it out to production in your own Job DSL
scripts.

- **Security Notice:** Private keys smaller than 2048 are no longer allowed for
  securing repository secrets.  Affected users will be required to generate a
  new key pair 2048 bits or larger.  A `KeyPairDecodeException` will now be
  thrown when users attempt to use private keys smaller than 2048 bits to
  encrypt repository secrets.  Users **should** rotate their secrets, rather
  than migrate, because their data was encrypted using a known broken RSA
  algorithm.  Data encrypted with this weaker algorithm is not considered secure
  and will still be accessible in git history.  Even if the git history is
  "modified" it may still exist in cloned copies.  It is safer to assume the
  encrypted data was compromised.  Learn more by reading about [enforcing
  stronger RSA keys in the wiki][wiki-stronger-rsa].
- The following deprecated methods were removed from Jervis and will no longer
  be available to Job DSL scripts.  See Jervis 0.13 release notes for a
  migration path.

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

#### New features:

- Jenkins Pipelines are now fully supported.  Jobs will automatically start
  using pipelines if there's a `Jenkinsfile` in the root of the repository or if
  the following is set in `.jervis.yml`.

  ```yaml
  jenkins:
    pipeline_jenkinsfile: 'path/to/Jenkinsfile'
  ```

  [See issue #98][#98]

- What is supported in Pipelines?
  - Non-matrix building.
  - Matrix building.
  - New YAML spec for stashing artifacts (even in matrix building).
    `jenkins.stash` is the new YAML spec.  [See issue #100][#100]
  - New YAML spec for publishing artifacts.  Items to be collected for
    publishing are automatically added to stashes in non-matrix builds.  Admins
    can define simple collections or expose more advanced options to users for
    customizing the publisher.  `jenkins.collect` is the new YAML spec.  [See
    also #97][#97]

#### Job DSL scripts changes in the `jobs/` folder:

- Job DSL scripts have been broken apart into more reusable parts.  This uses an
  advanced feature of Groovy known as the binding.  See
  [`jobs/README.md`](jobs/README.md) for details.
- JSON files for platforms, lifecycles, and toolchains have been moved to the
  `resources/` directory and are now shared with Jenkins pipelines.
- Unit tests for JSON files in the `resources` directory have been moved to a
  separate file: [`src/test/groovy/jervisConfigsTest.groovy`][config-tests].
  This allows admins to easily copy existing tests for use in their own Job DSL
  script libraries.
- New `pipelineGenerator` class is available for use in scripts.

#### Pipeline DSL scripts changes in the `vars/` folder:

- Added an example pipeline global shared library to `resources/` and `vars/`.
- New `pipelineGenerator` class is available for use in scripts.

#### Other notes for this release:

- Freestyle jobs and Pipeline jobs are supported together as a transition in
  this release.  In a future release, Freestyle job support will be dropped
  completely.

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

[#100]: https://github.com/samrocketman/jervis/issues/100
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
[#97]: https://github.com/samrocketman/jervis/issues/97
[#98]: https://github.com/samrocketman/jervis/issues/98
[bca-plugin]: https://wiki.jenkins.io/display/JENKINS/Bouncy+Castle+API+Plugin
[ccs-plugin]: https://wiki.jenkins-ci.org/display/JENKINS/Collapsing+Console+Sections+Plugin
[config-tests]: src/test/groovy/jervisConfigsTest.groovy
[gla-plugin]: https://wiki.jenkins.io/display/JENKINS/Groovy+Label+Assignment+plugin
[mig-01-ex]: https://github.com/samrocketman/jervis/commit/1d7ff1417c642d959f467c11eca7b16cb3e3ef3c
[wiki-stronger-rsa]: https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories#enforcing-stronger-rsa-keys
[wiki-toolchains-spec]: https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file
[fs-migrate-1]: https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/disable-freestyle-jobs.groovy
[fs-migrate-2]: https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/delete-freestyle-jobs.groovy
