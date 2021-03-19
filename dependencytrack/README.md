# DependencyTrack integration

This is an automated local integration for vulnerability reporting in Jervis
dependencies.

This guide assumes you have docker, docker-compose, GNU Awk, and OpenSSL tools
available.  Tested from Ubuntu 18.04 GNU/Linux.

# Provision

    docker-compose up -d

1. Log into `http://localhost:8080/` username `admin` and password `admin`.  You
   will be prompted to change it.
2. Under _Administration > Access Management > Teams_, open the `Automation`
    team, and add the permission `PROJECT_CREATION_UPLOAD`.
3. Within the `Automation` team copy the generated API key.  You'll need this
   later to set the `DT_API_TOKEN` environment variable.

# Generate SBOM

From the root of this repository.

    ./gradlew --init-script ./dependencytrack/dtrack.gradle cyclonedxBom

# Upload to DependencyTrack

From the root of this repository execute the following.

    export DT_API_TOKEN=...
    ./dependencytrack/submit-results.sh
