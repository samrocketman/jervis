#!/bin/bash -l
export PS4='$ '
echo '# ENVIRONMENT SECTION'
echo -n "Hostname: "
hostname
echo -n "Distro: "
head -n1 /etc/issue
echo -n "Kernel: "
uname -rms
echo -n "Bash: "
bash --version | head -n1
export JERVIS_RANDOM="${RANDOM}"
echo "JERVIS_RANDOM: ${JERVIS_RANDOM}"

#clean up on exit
function cleanup_on() {
  set +x

  if [ "${JERVIS_LANG}" = 'ruby' ]; then
    rvm gemset delete "${JERVIS_GEMSET}" --force
  fi

  if [ "${JERVIS_LANG}" = 'go' ]; then
    cd -
    rm -rf "${GOPATH}"
  fi
}
trap cleanup_on EXIT

set -axeE
