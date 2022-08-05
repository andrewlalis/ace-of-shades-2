#!/usr/bin/env bash

# Put your GRAALVM location here.
export GRAALVM_HOME=/home/andrew/Downloads/graalvm-ce-java17-22.2.0
mvn -Pnative -DskipTests clean package
