#!/bin/bash
#Sam Gleske
#Fri Feb 19 17:14:43 PST 2016

#DESCRIPTION
#  Script to simplify appending secrets to .mortar.yml.

#Run this script with no arguments for help doc.

function help(){
cat <<EOF
USAGE:
  $0 [key] [secret]
  $0 [secret]

EXAMPLE:
  $0 ENVIRON_VAR "my secret text"
  $0 "my secret text"
  $0 "my secret text" >> .mortar.yml
EOF
}
if [ "$#" = "1" ]; then
  echo "$1" | \
    openssl rsautl -encrypt -inkey ./id_rsa.pub -pubin | \
    openssl enc -base64 -A | \
    sed -e 's/\(.*\)/      secret: "\1"/'
elif [ $# = "2" ]; then
  echo $"    - key: \"$1\""
  echo "$2" | \
    openssl rsautl -encrypt -inkey ./id_rsa.pub -pubin | \
    openssl enc -base64 -A | \
    sed -e 's/\(.*\)/      secret: "\1"/'
else
  help
fi
