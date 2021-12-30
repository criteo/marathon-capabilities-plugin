#!/bin/bash

artifact_path=$(ls -1 target/scala-2.12/marathon-capabilities-plugin-assembly-*.jar)
artifact=$(basename "$artifact_path")
artifact_version="$(echo $artifact | sed -E 's/marathon-capabilities-plugin-assemply-(.*)\.jar/\1/')"

echo "::set-output name=path::$artifact_path"
echo "::set-output name=artifact::$artifact"
echo "::set-output name=version::$artifact_version"
