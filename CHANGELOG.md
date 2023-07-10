# CHANGELOG

This file contains all of the notable changes from Jervis releases.  For the
full change log see the commit log.

# jervis 2.1

:boom: Publishing bugfix - the published pom.xml for v2.0 did not have any
dependencies listed.  This patch-release fixes the published pom.

See Jervis 2.0 release notes for breaking changes.

## Breaking changes

And other critical notes.

In `src/` folder:

- `LifecycleGenerator.loadPlatforms` is renamed to
  `LifecycleGenerator.loadPlatformsFile`
- If you instantiate a `PipelineGenerator` with a `LifecycleGenerator`, the
  `LifecycleGenerator` gets recreated from scratch.  This means you can't modify
  your original `LifecycleGenerator` object with the expectation that
  `PipelineGenerator.getGenerator()` is updated.  They're separate objects.
- `PipelineGenerator.getDefaultToolchainsEnvironment()` will now always include
  the platform and OS even if they're not used in matrix building.

In `vars/` folder:

- `loadCustomResource` var no longer throws an exception.  If a file does not
  exist it will return `null` instead of String contents.

## New features

#### Jervis API changes in `src/` folder

- `JervisException` now supports throwing with a supplemental message.

#### Pipeline DSL scripts changes in the `vars/` folder

TBD

# jervis 2.0 - Jun 27th, 2023

This is a new major release.  From an end user perspective, all behavior for 1.x
and 0.x YAML files is still supported.  However, there are major API changes
which warrant bumping the major to warn integrators who may be using code.

### Migrating code

You can use GNU `sed` to migrate code.  The following is a `sedfile` of
expressions.

```
s/ \+$//
s/pipelineGeneratorTest/PipelineGeneratorTest/g
s/platformValidatorTest/PlatformValidatorTest/g
s/lifecycleGeneratorTest/LifecycleGeneratorTest/g
s/toolchainValidatorTest/ToolchainValidatorTest/g
s/jervisConfigsTest/JervisConfigsTest/g
s/lifecycleValidatorTest/LifecycleValidatorTest/g
s/lintJenkinsVarsTest/LintJenkinsVarsTest/g
s/securityIOTest/SecurityIOTest/g
s/pipelineGenerator/PipelineGenerator/g
s/lifecycleValidator/LifecycleValidator/g
s/toolchainValidator/ToolchainValidator/g
s/lifecycleGenerator/LifecycleGenerator/g
s/platformValidator/PlatformValidator/g
s/securityIO/SecurityIO/g
s/net\.gleske\.jervis\.lang\.[Ll]ifecycleGenerator\([ .]getObjectValue\)/net.gleske.jervis.tools.YamlOperator\1/g
s/generator\.getObjectValue/net.gleske.jervis.tools.YamlOperator.getObjectValue/g
```

Usage of the sedfile is the following.

```bash
find * -type f -name '*.groovy' -exec sed -i -f /tmp/sedfile {} +
```

Manually search for and change the following methods.  See [Major API changes
section](#2.0-major-api-changes) for details.

```bash
# change id_rsa_keysize usage to rsa_keysize
grep -r '\([gs]et\)\{0,1\}[Ii]d_rsa_keysize' *
```

Several Jenkins shared pipline vars have been converted to `NonCPS`.  This means
upstream `admin*` functions must also be changed to `NonCPS`.  The following is
an example of a NonCPS var.

```groovy
@NonCPS
def call() {
    // this method is NonCPS JIT compiled
}
```

If you define `admin*` vars you'll have to convert them to NonCPS.  The
following is a list of vars now requiring `NonCPS` annotation.

- `vars/adminLibraryResource.groovy`; refer to
  [`loadCustomResource`][loadCustomResource]

### Migrating JSON to YAML

platforms, lifecycles, and toolchains have migrated from JSON to YAML.  Users tend to define these files themselves.  As a result, [a migration script][migrate-yaml-platforms] has been created.  Run the migration script for your given prefix.

[migrate-yaml-platforms]: https://github.com/samrocketman/jervis/issues/142#issuecomment-1605691587

<a name="2.0-major-api-changes"></a>
### Major API changes

The following classes have been renamed.

| Old name for imports                        | New name for imports                        |
| ------------------------------------------- | ------------------------------------------- |
| `net.gleske.jervis.lang.lifecycleGenerator` | `net.gleske.jervis.lang.LifecycleGenerator` |
| `net.gleske.jervis.lang.lifecycleValidator` | `net.gleske.jervis.lang.LifecycleValidator` |
| `net.gleske.jervis.lang.pipelineGenerator`  | `net.gleske.jervis.lang.PipelineGenerator`  |
| `net.gleske.jervis.lang.platformValidator`  | `net.gleske.jervis.lang.PlatformValidator`  |
| `net.gleske.jervis.lang.toolchainValidator` | `net.gleske.jervis.lang.ToolchainValidator` |
| `net.gleske.jervis.tools.securityIO`        | `net.gleske.jervis.tools.SecurityIO`        |

The following methods and fields have been renamed or removed.

| Class                | Old method name       | New method name    |
| -------------------- | --------------------- | ------------------ |
| `LifecycleValidator` | `load_JSON()`         | `loadYamlFile()`   |
| `LifecycleValidator` | `load_JSONString()`   | `loadYamlString()` |
| `PlatformValidator`  | `load_JSON()`         | `loadYamlFile()`   |
| `PlatformValidator`  | `load_JSONString()`   | `loadYamlString()` |
| `SecurityIO`         | `getId_rsa_keysize()` | `getRsa_keysize()` |
| `SecurityIO`         | `setId_rsa_keysize()` | Removed            |
| `SecurityIO`         | `id_rsa_keysize`      | Removed            |
| `ToolchainValidator` | `load_JSON()`         | `loadYamlFile()`   |
| `ToolchainValidator` | `load_JSONString()`   | `loadYamlString()` |


The following methods have moved.

| Method           | Old class | New class |
| ---------------- | --------- | --------- |
| `getObjectValue` | `net.gleske.jervis.lang.LifecycleGenerator` | `net.gleske.jervis.tools.YamlOperator` |

### Warnings:

- Support for all vendors of JDK 1.8 is dropped in this release.
- OpenJDK11 or OpenJDK17 is build runtime and OpenJDK8 is the bytecode
  compatibility going forward to match the Jenkins project.  Groovy 2.4 does not
  support higher than OpenJDK8 bytecode.
- Function `getJervisYamlFiles(String owner, String repository)` within class
  `net.gleske.jervis.remotes.GitHubGraphQL` used to default to `master` branch.
  It now defaults to `main` branch.
- Function `getJervisYamlFiles(String repositoryWithOwner)` within class
  `net.gleske.jervis.remotes.GitHubGraphQL` used to default to `master` branch.
  It now defaults to `main` branch.

### New features:

#### Pipeline DSL scripts changes in the `vars/` folder

- Jervis steps read from platforms, lifecycles, and toolchains YAML instead of
  JSON.
- Matrix building nodes reordered so it is wrapped in stages.
- New `hasGlobalResource()` step which can be used to conditionally load
  resources from `libraryResource` step.  Allows a pipeline developer to only
  call `libraryResource` if it exists.  Normally `libraryResource` step will
  throw an exception if the step doesn't exist.  This is a fully `NonCPS` step
  and can be called from other `NonCPS` code blocks.
- New `getBuildContextMap()` which returns information about the current running
  pipeline such as how it was triggered, which part of Git workflow, and other
  meta info.
- New `getJervisPipelineGenerators()` which can read multiple repositories and
  return `.jervis.yml` pipeline objects for each repository in one API call.
- `isBuilding()` more reliable now that it is built into Jervis with unit tests.
  Several bugs were fixed while reaching 100% test coverage.
- `loadCustomResource()` has some new behavior.  It first loads
  `adminLibraryResource`, then checks for the resource in the global config
  files plugin, and finally falls back to `libraryResource`.  It can also skip
  looking for `adminLibraryResource` via a new boolean option:
  ```groovy
  // skip loading adminLibraryResource
  loadCustomResource('resource-name', true)
  ```
- The following vars are now fully `NonCPS`.  These vars can be called from
  within other `NonCPS` annotated methods in shared pipelines.
  - [`getBuildContextMap`](vars/getBuildContextMap.groovy)
  - [`getJervisPipelineGenerators`](vars/getJervisPipelineGenerators.groovy)
  - [`getUserBinding`](vars/getUserBinding.groovy)
  - [`hasGlobalResource`](vars/hasGlobalResource.groovy)
  - [`hasGlobalVar`](vars/hasGlobalVar.groovy)
  - [`isBuilding`](vars/isBuilding.groovy)
  - [`isPRBuild`](vars/isPRBuild.groovy)
  - [`isTagBuild`](vars/isTagBuild.groovy)
  - [`loadCustomResource`][loadCustomResource]
  - [`prepareJervisLifecycleGenerator`](vars/prepareJervisLifecycleGenerator.groovy)
  - [`prepareJervisPipelineGenerator`](vars/prepareJervisPipelineGenerator.groovy)

#### Jervis API changes in `src/` folder

- [`net.gleske.jervis.remotes.GitHubGraphQL`][GitHubGraphQL] has a new `sendGQL`
  method.  `variables` are now supported as a Map in addition to a String.  The
  Map will be automatically converted to a String before being sent to GitHub as
  a query.
- [HashiCorp Vault][vault.io] support classes available.  This will eventually
  lead to better native pipeline integration with Vault.
  - [`VaultService`][VaultService] class provides an easy to use communication
    class to KV Secrets Engine v1 and v2.  AppRole authentication is recommended
    but any [`TokenCredential`][TokenCredential] type can be used.
  - AppRole authentication provided by
    [`VaultAppRoleCredential`][VaultAppRoleCredential].  It automatically renews
    leases and rotates credentials as leases run out.  By default AppRole
    `role_id` and `secret_id` are resolved from
    [`VaultRoleIdCredentialImpl`][VaultRoleIdCredentialImpl], but custom
    credential resolver can be implented on
    [`VaultRoleIdCredential`][VaultRoleIdCredential] interface.
- Extend [`net.gleske.jervis.remotes.StaticMocking`][StaticMocking] test class
  to support recording mock API responses while calling Jervis dependent code.
- SimpleRestServiceSupport class changes.  All REST services provided in
  `net.gleske.jervis.remotes.*` have new behaviors.
  - New HTTP header available on all REST services.  Setting the `Parse-JSON`
    HTTP header on any REST service will override its default behavior.  It can
    force-parse JSON or it can force returning plain text for JSON APIs instead
    of parsed JSON objects.
  - The default API response for SimpleRestService ias changed from a `Map` to a
    `String`.  This means if there's no content response an empty `String` will
    be returned regardless of JSON parsing for the API.  This used to return an
    empty `HashMap`.
- More flexibility has been added to static method
  `net.gleske.jervis.remotes.SimpleRestService.apiFetch()`.
- Enhancements in `net.gleske.jervis.tools.SecurityIO`
  - Converted multiple functions to `static` to ease their use.
  - Added AES-256 encryption functions.
  - Added RS256 aglorithm for data signing and verification.
  - Added GitHub JSON Web Token (JWT) creation and verification support.
  - Added generic JWT verification.
  - `avoidTimingAttack()` static function available with usage documentation.
  - `getRsa_keysize()` always returns the calculated key size if any.
- Enhancements in [`net.gleske.jervis.remotes.GitHub`][GitHub]
  - Added support for adding headers to all requests via `GitHub.headers` field.
  - Updated client HTTP headers to match GitHub v3 REST API version
    `2022-11-28`.
- New [`CipherMap`][CipherMap] utility class meant to transparently provide
  strong encryption for map objects.
- New [`EphemeralTokenCache`][EphemeralTokenCache] credential which is an
  encrypted cache meant to store ephemeral API tokens issued by services such as
  GitHub App or any other time-limited token service.  The intention of the
  cache is to reuse issued tokens in order to reduce API requests.
- GitHub App authentication now available via the following classes.
  - [`EphemeralTokenCache`][EphemeralTokenCache] provides token storage and
    automatic cleanup of expired tokens.
  - [`GitHubAppRsaCredentialImpl`][GitHubAppRsaCredentialImpl]
  - [`GitHubAppCredential`][GitHubAppCredential] a credential meant for API
    clients such as [`GitHub`][GitHub] or [`GitHubGraphQL`][GitHubGraphQL].
    Credential rotation is handled automatically and transparent to the client.

### Bug fixes:

- Major bugfix: support for more HTTP methods which have no content in the
  response.
- Bugfix: Groovy 3.0.5 YAML `additional_toolchains` order was not preserved.
  This change makes Jervis compatible with Groovy 2.4, 2.5, 2.6, and 3.0, and
  4.0 series of releases.  Jenkins LTS currently uses Groovy 2.4.21 so this is
  more of a future-proofing fix than a bug for existing usage.
- Minor bugfix around cipherlist loading in LifecycleGenerator.  Discovered via
  100% test coverage goal.

### Other notes:

- Added support for [VSCode dev containers][vscode-devc] to ease with portable
  development environments going forward.  Due to tight integration with X11 and
  other Linux APIs the development host must be Linux in order to use VSCode dev
  containers.  Fine for me since all of my computers are Linux but an important
  note for would-be contributors.
- Upgraded to Gradle 7.6
- Added support for building on OpenJDK 11 and OpenJDK 17.  OpenJDK 17 requires
  Gradle 3 or higher.
- Extended support for building and running on Groovy versions 2.4 through 4.0.
- API docs now have syntax highlighting in sample usage code blocks.

[CipherMap]: src/main/groovy/net/gleske/jervis/tools/CipherMap.groovy
[loadCustomResource]: vars/loadCustomResource.groovy
[EphemeralTokenCache]: src/main/groovy/net/gleske/jervis/remotes/creds/EphemeralTokenCache.groovy
[GitHubAppCredential]: src/main/groovy/net/gleske/jervis/remotes/creds/GitHubAppCredential.groovy
[GitHubAppRsaCredentialImpl]: src/main/groovy/net/gleske/jervis/remotes/creds/GitHubAppRsaCredentialImpl.groovy
[GitHub]: src/main/groovy/net/gleske/jervis/remotes/GitHub.groovy
[StaticMocking]: src/test/groovy/net/gleske/jervis/remotes/StaticMocking.groovy
[TokenCredential]: src/main/groovy/net/gleske/jervis/remotes/interfaces/TokenCredential.groovy
[VaultAppRoleCredential]: src/main/groovy/net/gleske/jervis/remotes/creds/VaultAppRoleCredential.groovy
[VaultRoleIdCredentialImpl]: src/main/groovy/net/gleske/jervis/remotes/creds/VaultRoleIdCredentialImpl.groovy
[VaultRoleIdCredential]: src/main/groovy/net/gleske/jervis/remotes/interfaces/VaultRoleIdCredential.groovy
[VaultService]: src/main/groovy/net/gleske/jervis/remotes/VaultService.groovy
[vault.io]: https://www.vaultproject.io/
[vscode-devc]: https://code.visualstudio.com/docs/devcontainers/containers

# jervis 1.7 - Apr 14th, 2020

### Bug fixes:

- Bugfix: Additional toolchains loaded into a matrix build did not properly
  matrix.  This bug has been fixed and tests added to avoid it.
- Security: Bump snakeyaml to 1.26 to protect against billion laughs style
  attacks.
- Security: Use SafeConstructor when parsing YAML to prevent remote code
  execution.  See [Documentation][safeconst] and [Java Doc][safeconst-java].

### Breaking Job DSL changes

Jobs generated now use the [SCM Filter Jervis YAML][plugin-sf-jervis] plugin
instead of the [SCM Filter Branch PR][plugin-sf-bpr] plugin.  If your Jenkins
instance does not have the SCM Filter Jervis YAML plugin installed, then you'll
get errors attempting to generate new jobs.  This should not affect jobs that
already exist but it also means existing jobs can't be regenerated without the
plugin.

As a recommended migration path to convert all jobs to use the SCM Filter for
Jervis YAML, you can run a [script console script to regenerate all
jbos][regenerate-jobs-script]

### Deprecated pipeline steps

The following Jenkins pipeline steps provided by Jervis are deprecated and will
go away in a future release.

- `isPRBuild()` - use `isBuilding('pr')` instead.
- `isTagBuild()` - use `isBuilding('tag') instead.

As an admin, if you still want to support these steps then create your own steps
within your own shared pipeline library.  Here are some examples:

Contents of `vars/isPRBuild()`:

```groovy
Boolean call() {
    isBuilding('pr')
}
```

Contents of `vars/isTagBuild.groovy`:

```groovy
Boolean call() {
    isBuilding('tag')
}
```

### Pipeline DSL scripts changes in the `vars/` folder:

- New pipeline steps:
  - `getMatrixAxes()` - Spawned from a [Jenkins blog post][jenkins-blog-matrix].
    It is not used directly by Jervis but is available to users of Jervis.
  - `getUserBinding('somevar')` - Users can set bindings in their pipeline
    runtime.  This step allows pipeline shared libraries to retrieve bindings as
    opposed to having them passed as arguments to a step.
- `isBuilding` now supports `manually` triggered builds via
  `isBuilding('manually')`.  `manually` takes several options
  - `isBuilding(manually: false, combined: true)` - a boolean where if true returns the
    username of the user who triggered the build and is boolean truthy.  If
    false it will return true if the build was triggered by anything except a
    user, manually.
  - `isBuilding(manually: 'samrocketman', combined: true)` - returns true only
    if the build was manually triggered by user `samrocketman`.
- `isBuilding` now supports a `combined` boolean status of all filters for
  easing use in pipelines logic.  Example of filtering for manually triggered
  tags is `isBuilding(manually: true, tag: '/.*/', combined: true)` which
  instead of returning a HashMap of the results for each filter it will return a
  single boolean.  Returns `true` if all examples were true and false if any
  filter was not true.
- `isBuilding` now supports a List.  See also documentation in the [Jervis
  wiki][isBuilding-list].

[isBuilding-list]: https://github.com/samrocketman/jervis/wiki/Pipeline-support#isBuilding-step
[jenkins-blog-matrix]: https://jenkins.io/blog/2019/12/02/matrix-building-with-scripted-pipeline/
[plugin-sf-bpr]: https://plugins.jenkins.io/scm-filter-branch-pr
[plugin-sf-jervis]: https://plugins.jenkins.io/scm-filter-jervis
[regenerate-jobs-script]: https://github.com/samrocketman/jenkins-script-console-scripts/blob/main/generate-all-jervis-jobs.groovy
[safeconst-java]: https://www.javadoc.io/doc/org.yaml/snakeyaml/1.19/org/yaml/snakeyaml/constructor/SafeConstructor.html
[safeconst]: https://bitbucket.org/asomov/snakeyaml/wiki/Documentation

# jervis 1.6 - Nov 10th, 2019

### New features:

- [`net.gleske.jervis.remotes.GitHubGraphQL`][GitHubGraphQL] has a new method
  `getJervisYamlFiles` which allows a caller to get multiple Jervis YAML files
  from multiple branches in a single API call.  It is overloaded to return a
  list of files in each branch as well.

#### Pipeline DSL scripts changes in the `vars/` folder:

- New pipeline step `isBuilding()` which provides a versatile conditional which
  allows a Jenkins pipeline user to filter the type of build that the current
  runtime is.  e.g. Cron build, PR build, tag build, branch build... It supports
  checking tags and branches against a filter (literal or regex) so that it
  matches only tags and branches which have a specific name.  See the header
  comment in [`vars/isBuilding.groovy`](vars/isBuilding.groovy) which has
  documentation and examples for how to use the step.

#### Job DSL scripts changes in the `jobs/` folder:

- Lots of code cleanup.  Generating jobs just got a lot simpler!  With the
  release of the scm-filter-jervis plugin we can now rely on on-the-fly branch
  detection.  So we can generate jobs without pre-populating the repository with
  YAML.  Rather than exhaustively listing everything that was deleted please
  review this [git diff of code cleanup][code-cleanup-1].

### Other notes

- For tests, `net.gleske.jervis.remotes.StaticMocking` now supports mocking
  GraphQL and can render a different response depending on the query passed to
  the GraphQL mock.  See [`GitHubGraphQLTest.groovy`][GitHubGraphQLTest.groovy]
  for an example of how it is used in tests.
- For tests, `./gradlew console` now includes test classes in its classpath to
  help with debugging `net.gleske.jervis.remotes.StaticMocking`.

[GitHubGraphQLTest.groovy]: src/test/groovy/net/gleske/jervis/remotes/GitHubGraphQLTest.groovy
[GitHubGraphQL]: src/main/groovy/net/gleske/jervis/remotes/GitHubGraphQL.groovy
[code-cleanup-1]: https://github.com/samrocketman/jervis/commit/0e29edd5b1cadae11a02a1d680296ebfda52ad0e

# jervis 1.5 - Nov 9th, 2019

### New features:

- AutoRelease support class added which provides automated bumping for versions.
  This is useful in Jenkins pipeline scripts by giving the next version if given
  a version and a set of existing Git tags.  It supports unconventional
  continuos release and semantic versioning.  It supports tags having prefixes
  like `v1.0` (prefix being `v`) and suffixes like `v1.0-beta`.  The API
  documentation for the AutoRelease class has several examples for how this new
  feature behaves.

# jervis 1.4 - Nov 1st, 2019

### New features:

- Support for external credentials integration.  Most likely used by Jenkins
  credentials store.  Currently, the only credential type is a TokenCredential
  for GitHub services.
- `net.gleske.jervis.remotes.creds.ReadonlyTokenCredential` is available for
  implementation for a TokenCredential which doesn't support writing.

### Other notes

- Added Jacoco for code coverage.
- Updated builds and coveralls to support Jacoco instead of Cobertura.
- [Local SonarQube analysis](sonarqube/README.md) available.
- Code has been refactored based on SonarQube analysis.
- Upgraded SnakeYAML dependency to 1.25.

# jervis 1.3 - Oct 31st, 2019

#### New features:

- Test helper classes provided by Jervis may now be referenced by other projects
  via the `tests` classifier.  Source code for tests available within the
  `tests-sources` classifier.
- Add a GitHub GraphQL v4 API client.  It's not used yet but is available for me
  to start using it in pipeline scripts.
- Upgrade to Gradle 5.6.3.  Unfortunately, we're losing our ability to calculate
  code coverage with Cobertura.  However, that hasn't really worked since Groovy
  2.0.8 anyways so there's no reason to keep holding back any longer.

#### Pipeline DSL scripts changes in the `vars/` folder:

- Added `isPRBuild()` step for filtering pull request builds.  `IS_PR_BUILD`
  environment variable is available to shell scripts.
- Added `isTagBuild()` step for filtering tag builds.  `IS_TAG_BUILD`
  environment variable is available to shell scripts.
- Allow admin extension of `jervisBuildNode` via `adminJervisBuildNode` step.
- Allow admin extension of library resources with adminLibraryResource.  This
  allows 3rd parties to integrate their own custom resources.
- withEnvSecretWrapper now supports a secrets map.
- BugFix decoding private keys

#### Job DSL scripts changes in the `jobs/` folder:

- Added support for the scm-filter-branch-pr 0.4 plugin which allows for
  filtering for branches and tags.
- New Feature: `tryReadFile` is implemented for all Job DSL bindings so that Job
  DSL scripts can be customized as-needed by 3rd parties.  This is to support
  reading jervis from a Git submodule.
- Remove decoding private keys during job generation.
- Replace config with pipelineBranchDefaultsProjectFactory

# jervis 1.2 - Aug 12th, 2018

#### New features:

- Support for `skip_on_pr` and `skip_on_tag` in YAML specification when
  publishing collections like artifacts.  An admin can set default values for
  each per collection in `jenkins.collect` YAML.
- A new `net.gleske.jervis.remotes.SimpleRestService` class is available which
  makes it easy to communicate with REST services.  SnakeYAML is used for
  parsing JSON responses which is the same for the rest of the Jervis library.
- `generator.is_pr` and `generator.is_tag` properties now provide a portable
  means for changing behavior based on what type of build is occuring.

#### Bug fixes:

- Stashmap Preprocessor did not work at all which meant the HTML publisher was
  broken.  This release fixes that.

#### Pipeline DSL scripts changes in the `vars/` folder:

- Major rewrite: `buildViaJervis` has been completely refactored and utilizes
  better use of pipeline variables.
- Since `skip_on_pr` support is now built into Jervis this part was removed from
  the pipeline library.
- Matrix builds are now grouped with a friendly name `Build Project` stage.
- Support for admins to side load their own libraries to customize code.
- Support to load JSON files from global config file plugin settings in Jenkins.
- Publishing collections now occurs in parallel.
- Updated to support `generator.is_pr` in Jervis.
- HTML publishing is fixed!

# jervis 1.1 - Apr 16th, 2018

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
  Validation also works on the default user field which means if a user does not
  pass validation, then their publisher is simply skipped.
- Admins can preprocess stashmaps for publishers so that more complex stashing
  can occur if a plugin for publishing results requires it.

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

[cfp]: https://plugins.jenkins.io/config-file-provider
[mpwd]: https://plugins.jenkins.io/pipeline-multibranch-defaults

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
[fs-migrate-1]: https://github.com/samrocketman/jenkins-script-console-scripts/blob/main/disable-freestyle-jobs.groovy
[fs-migrate-2]: https://github.com/samrocketman/jenkins-script-console-scripts/blob/main/delete-freestyle-jobs.groovy
