# How to release

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

[ ] Check test coverage and ensure it is high.
[ ] Ensure groovydoc API is fully documented.
[ ] Update wiki documentation for release.
[ ] Update [CHANGELOG.md](CHANGELOG.md).

### Perform release

    ./gradlew clean uploadArchives
