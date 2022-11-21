#!/bin/bash
# Created by Sam Gleske
# Ubuntu 18.04.3 LTS
# Linux 5.3.0-28-generic x86_64
# GNU Awk 4.1.4, API: 1.1 (GNU MPFR 4.0.1, GNU MP 6.1.2)
# sha256sum (GNU coreutils) 8.28
# GNU bash, version 4.4.20(1)-release (x86_64-pc-linux-gnu)
# DESCRIPTION:
#    This script will evaluate the current checkout out release and ensure it
#    is uploaded to GitHub as a tag.  It will then pull release notes from
#    CHANGELOG.md and submit the changelog to GitHub releases.
# USAGE:
#    Submit release notes
#      git checkout jervis-<version number>
#      ./submit-github-release-notes.sh

set -e

function awk_script() {
  cat <<'EOF'
BEGIN {
    show = 0
}
$0 ~ ("^# jervis " version ".*") {
    m = 1
}
$0 ~ /^# jervis [.0-9]+.*/ {
    if(m != 1) {
        next
    }
    if(show == 0) {
        show = 1
    } else {
        exit
    }
}
# print if show is enabled
show == 1
EOF
}

function getversion() {
  gawk -F= '$1 == "version" { print $2; exit}' gradle.properties
}

function get_release_notes() {
  gawk -v version="$1" "$(awk_script)" CHANGELOG.md
cat <<EOF
See the [full CHANGELOG](https://github.com/samrocketman/jervis/blob/jervis-${1}/CHANGELOG.md)...

# Build environment

\`\`\`
\$ head -n1 /etc/issue
$(head -n1 /etc/issue | sed 's/ \\.*$//')

\$ lsb_release -d
$(lsb_release -d)

\$ uname -rms
$(uname -rms)

\$ java -version
$(java -version 2>&1)
\`\`\`

\`\`\`
\$ ./gradlew  -version
$(./gradlew -version)
\`\`\`

Releases signed with [\`8D8BF0E242D8A068572EBF3CE8F732347257E65F\`][keybase].

[keybase]: https://keybase.io/samrocketman/pgp_keys.asc?fingerprint=8d8bf0e242d8a068572ebf3ce8f732347257e65f

EOF
}

function current_tag() {
  git tag --points-at HEAD
}

function check_github_vars() {
  if [[ -z "${GITHUB_USER}" && -z "${GITHUB_REPO}" ]] && git config --get remote.origin.url | grep ':' > /dev/null; then
    GITHUB_USER="$(git config --get remote.origin.url)"
    GITHUB_USER="${GITHUB_USER#*:}"
    GITHUB_USER="${GITHUB_USER%.git}"
    GITHUB_REPO="${GITHUB_USER#*/}"
    GITHUB_USER="${GITHUB_USER%/*}"
    export GITHUB_USER GITHUB_REPO
  fi

  if [[ -z "${GITHUB_TOKEN}" || -z "${GITHUB_USER}" || -z "${GITHUB_REPO}" ]]; then
    echo $'ERROR: Missing required environment variables:\n  - GITHUB_TOKEN\n  - GITHUB_USER\n  - GITHUB_REPO'
    [ -z "${!GITHUB_*}" ] || echo "You have defined: ${!GITHUB_*}"
    exit 1
  fi

  export GITHUB_API="${GITHUB_API:-https://api.github.com}"
  GITHUB_API="${GITHUB_API%/}"
}

function tempdir() {
  TMP_DIR=$(mktemp -d)
  PATH="${TMP_DIR}:${PATH}"
  export TMP_DIR PATH
}

function checkForGawk() {
  if ! type -P gawk; then
    if [ "$(uname -s)" = Darwin ]; then
      echo 'ERROR: Missing required GNU Awk.  If using homebrew then "brew install gawk".'
    else
      echo 'ERROR: Missing required GNU Awk.'
    fi
    return 1
  fi
  return 0
}

function checkGHRbin() {
  local url sha256
  local TAR=()
  case $(uname -s) in
    Linux)
      url='https://github.com/aktau/github-release/releases/download/v0.7.2/linux-amd64-github-release.tar.bz2'
      sha256='3feae868828f57a24d1bb21a5f475de4a116df3063747e3290ad561b4292185c'
      TAR+=( tar --transform 's/.*\///g' )
      ;;
    Darwin)
      url='https://github.com/aktau/github-release/releases/download/v0.7.2/darwin-amd64-github-release.tar.bz2'
      sha256='92d7472d6c872aa5f614c5141e84ee0a67fbdae87c0193dcf0a0476d9f1bc250'
      TAR+=( tar --strip-components 3 )
      ;;
  esac
  if ! type -P github-release && [ -n "${sha256}" ]; then
    pushd "${TMP_DIR}"
    curl -LO "${url}"
    "${SHASUM[@]}" -c - <<< "${sha256}  ${url##*/}"
    "${TAR[@]}" -xjf "${url##*/}"
    command rm "${url##*/}"
    popd
  fi
}

#exit non-zero if no "repo" or "public_repo" OAuth scope found for API token
function checkOAuthScopes() {
  curl -sIH "Authorization: token $GITHUB_TOKEN" "${GITHUB_API}/" |
  gawk '
  BEGIN {
    code=1
  };
  $0 ~ /^.*X-OAuth-Scopes:.*\y(public_)?repo,?\y.*/ {
    code=0;
    print $0
    exit
  };
  END { exit code }
  '
}

function read_err_on() {
  exec >&2
  case $1 in
    10)
      echo 'ERROR: must be in root of repository to publish release notes.'
      ;;
    12)
      echo 'ERROR: mktemp command is missing.'
      ;;
    13)
      echo "ERROR: you must have a release tag checked out."
      ;;
    14)
      echo "ERROR: your tag must be pushed to GitHub."
      ;;
    15)
      echo $'ERROR: GITHUB_TOKEN must have one of the following scopes:\n  - repo\n  - public_repo'
      ;;
    16)
      echo 'ERROR: github-release does not exist and could not be downloaded.'
      ;;
    0)
      echo 'SUCCESS: release published to GitHub.'
      ;;
    *)
      echo 'ERROR: an error occured.'
      ;;
  esac
  [ ! -d "${TMP_DIR:-}" ] || rm -rf "${TMP_DIR}"
}

function determine_shasum_prog() {
  if type -P sha256sum; then
    SHASUM+=( 'sha256sum' )
  elif type -P shasum; then
    SHASUM+=( 'shasum' '-a' '256' )
  else
    return 1
  fi
}

#
# MAIN EXECUTION
#

# pre-flight error checking
trap 'read_err_on $?' EXIT
[ -d '.git' ] || exit 10
type -P mktemp || exit 12
check_github_vars
checkForGawk
SHASUM=()
determine_shasum_prog || exit 11
tag=$(current_tag)
version=$(getversion)
[ "$tag" = "jervis-$version" ] || exit 13
(git ls-remote --tags | grep "jervis-$version\$") &> /dev/null || exit 14
checkOAuthScopes || exit 15
tempdir
checkGHRbin
type -P github-release || exit 16

#upload release notes
get_release_notes "$version" | \
  github-release release --tag "jervis-$version" --description -
