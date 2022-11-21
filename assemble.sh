#!/bin/bash
set +e
count=0
STATUS=1
while [ ! "${STATUS}" = '0' ]; do
  ./gradlew assemble
  STATUS=$?
  if [[ "${STATUS}" -eq '0' || "${count}" -ge '2' ]];then
    break
  fi
  echo 'Failed to assemble dependencies.  Sleep 5 and retry.'
  sleep 5
  ((count++))
done
exit $STATUS
