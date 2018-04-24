# How to release to Maven Central

* Follow Central recommendations for [setting up gradle][ossrh-gradle].
  Additionally, there's decent documentation on code signing in the [gradle
  documentation][gradle-signing]

* Ensure GPG is set up for signing jars.
* Ensure `~/.gradle/gradle.properties` has the following contents for signing
  and uploading jars to Maven Central.

```
signing.keyId=7257E65F
signing.password=secret
signing.secretKeyRingFile=/home/sam/.gnupg/secring.gpg
ossrhUsername=samrocketman
ossrhPassword=secret
```

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
   ./gradlew clean check uploadArchives
   ```
4. Increment plugin version to next snapshot: e.g. `0.3-SNAPSHOT`.  Commit.
5. Upload jervis-api docs for the latest release.

This will initially upload the artifact to a staging repository.  Once confident
about the release visit [Maven Central Nexus][ossrh] and follow [instructions on
releasing to production][ossrh-release].

[gradle-signing]: https://docs.gradle.org/current/userguide/signing_plugin.html
[ossrh-gradle]: http://central.sonatype.org/pages/gradle.html
[ossrh-guide]: http://central.sonatype.org/pages/ossrh-guide.html
[ossrh]: https://oss.sonatype.org/
[ossrh-release]: http://central.sonatype.org/pages/releasing-the-deployment.html
