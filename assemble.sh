#!/bin/bash
count=0
while ! ./gradlew assemble;do
  if [ "${count}" = 3 ];then
    break
  fi
  sleep 5
  ((count++))
done
