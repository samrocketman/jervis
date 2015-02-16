#!/bin/bash
set +e
count=0
STATUS=0
while ! ./gradlew assemble;do
  STATUS=$?
  if [ "${count}" = 3 ];then
    break
  fi
  echo "Failed to assemble dependencies.  Sleep 5 and retry."
  sleep 5
  ((count++))
done
exit $STATUS
