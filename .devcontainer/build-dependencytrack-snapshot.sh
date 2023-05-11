#!/bin/bash
# Created by Sam Gleske
# Sun Nov 20 13:57:14 EST 2022
# Pop!_OS 18.04 LTS
# Linux 5.4.0-113-generic x86_64
# GNU bash, version 4.4.20(1)-release (x86_64-pc-linux-gnu)
# Gradle 5.6.3
# Build time: 2019-10-18 00:28:36 UTC
# Revision: bd168bbf5d152c479186a897f2cea494b7875d13
# Kotlin: 1.3.41
# Groovy: 2.5.4
# Ant: Apache Ant(TM) version 1.9.14 compiled on March 12 2019
# JVM: 1.8.0_342 (Private Build 25.342-b07)
# OS: Linux 5.4.0-113-generic amd64
# Docker version 20.10.18, build b40c2f6

set -exo pipefail
exit

function dockerImageExists() {
  [ -n "$(docker images -q "$1")" ]
}

if dockerImageExists apiserver:snapshot && \
   dockerImageExists frontend:snapshot; then
  exit
fi

TMP_DIR="$(mktemp -d)"
trap '[ -z "${TMP_DIR:-}" ] || rm -rf "${TMP_DIR}"' EXIT
# Ensure directory created
[ -d "${TMP_DIR}" ]

dockerImageExists apiserver:snapshot || (
  git clone -b support-no-authz \
    https://github.com/samrocketman/dependency-track.git \
    "${TMP_DIR}"/dependency-track
  cd "${TMP_DIR}"/dependency-track

  docker run -it --rm -u "$(id -u):$(id -g)" -v "$PWD:$PWD" -w "$PWD" maven:3-eclipse-temurin-17-alpine /bin/bash -exc '
    mvn package -Dmaven.test.skip=true -P enhance -P embedded-jetty -Dlogback.configuration.file=src/main/docker/logback.xml
    mvn clean -P clean-exclude-wars
    '

  docker build -t apiserver:snapshot -f src/main/docker/Dockerfile .
)

dockerImageExists frontend:snapshot || (
  git clone -b nologin-backend \
    https://github.com/samrocketman/frontend \
    "${TMP_DIR}"/frontend
  cd "${TMP_DIR}"/frontend

  docker run --rm -it \
    -u "$(id -u):$(id -g)" \
    -v "$PWD:$PWD" \
    -w "$PWD" \
    node:16 /bin/bash -exc 'npm install; npm run build'

  docker build -t frontend:snapshot -f docker/Dockerfile.alpine .
)
