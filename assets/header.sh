#!/bin/bash
export PS4='$ '
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
set -axeE
