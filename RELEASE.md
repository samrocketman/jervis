# How to release to Maven Central

* Follow Central recommendations for [setting up gradle][ossrh-gradle].
  Additionally, there's decent documentation on code signing in the [gradle
  documentation][gradle-signing]

* Ensure GPG is set up for signing jars.
* Ensure `~/.gradle/gradle.properties` has the following contents for signing
  and uploading jars to Maven Central.

```
ossrhUsername=samrocketman
ossrhPassword=secret
```

Next we need to prepare the environment according to the [signing plugin
documentation][sign-plugin] for Gradle.

    ORG_GRADLE_PROJECT_signingKey="$(gpg -a --export-secret-keys "Sam Gleske")"
    read -ersp pass: ORG_GRADLE_PROJECT_signingPassword
    export ORG_GRADLE_PROJECT_signingKey ORG_GRADLE_PROJECT_signingPassword

I now use in-memory signing keys because versions of GnuPG 2.1 and newer are
challenging to set up for the signing Gradle plugin.

[sign-plugin]: https://docs.gradle.org/current/userguide/signing_plugin.html

# About signatures

All Jervis releases are signed by Sam Gleske GPG key ID `7257E65F`.  The long
fingerprint is the following.

    8D8B F0E2 42D8 A068 572E  BF3C E8F7 3234 7257 E65F

### Prepare for release checklist

- [ ] Check test coverage and ensure it is high.
- [ ] Ensure groovydoc API is fully documented.
- [ ] Update wiki documentation for release.
- [ ] Update [CHANGELOG.md](CHANGELOG.md) with changes.
- [ ] Update [CHANGELOG.md](CHANGELOG.md) with release date.
- [ ] Update README maven and gradle examples to reflect latest version.
- [ ] Update pipeline scripts to grab the next version.

### Perform release

1. Increment the plugin version in `build.gradle` to a stable release:
   e.g. `0.2`.  Commit.
2. Tag the current commit: e.g. `jervis-0.2`.
3. Upload the release.
   ```
   ./gradlew clean check publish
   ```
4. Increment plugin version to next snapshot: e.g. `0.3-SNAPSHOT`.  Commit.

This will initially upload the artifact to a staging repository.


# Final releasing steps

1. Visit [Maven Central Nexus][ossrh] and follow [instructions on releasing to
   production][ossrh-release].
2. Upload jervis-api docs for the latest release.
3. Upload GitHub release notes by executing `./submit-github-release-notes.sh`.

[gradle-signing]: https://docs.gradle.org/current/userguide/signing_plugin.html
[ossrh-gradle]: http://central.sonatype.org/pages/gradle.html
[ossrh-guide]: http://central.sonatype.org/pages/ossrh-guide.html
[ossrh]: https://oss.sonatype.org/
[ossrh-release]: http://central.sonatype.org/pages/releasing-the-deployment.html
