#!/bin/bash

sh build.sh $1
build_out=$(docker images --filter=reference="platform/communication-engine:$1" | grep "platform/communication-engine")

if [ -z "$build_out" ]
then
  echo "Exiting since build failed"
  exit 1
fi

parsed_tag="platform/communication-engine:$1"

echo "Parsed tag is $parsed_tag"