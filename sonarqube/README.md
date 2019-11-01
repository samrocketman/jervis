# Analyzing Jervis with SonarQube

This folder is meant to make it easy to quickly analyze Jervis code (and any
local changes) with SonarQube.  This is meant to be run on my local development
machine.  When Sonar is running I visit http://localhost:9000/ in my web
browser.

Prerequisites:

* Docker
* docker-compose

# Setup

Start sonarqube by running the following command relative to this directory.

   ```
   docker-compose up -d
   ```

> Note: If you would like to log in as the administrator, then login with user:
> `admin`, password: `admin`.  This is not necessary to submit local development
> results or to read results.

# Running analysis

At the root of this repository, simply run

    ./gradlew sonar

and see the results appear.

# Starting and stopping Sonarqube

You can stop Sonarqube with

    docker-compose down

Start Sonarqube with

    docker-compose up -d

# Delete SonarQube

To completely remove SonarQube from your system run

    docker-compose down -v --rmi all

It will do the following to SonarQube

* Stop the service, if it is running.
* Delete all analysis data.
* Remove SonarQube docker image.
