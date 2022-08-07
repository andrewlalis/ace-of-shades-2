#!/usr/bin/env bash

function join_by {
   local d=${1-} f=${2-}
   if shift 2; then
     printf %s "$f" "${@/#/$d}"
   fi
}

mvn clean package javafx:jlink -DskipDebug=true -DstripJavaDebugAttributes=true -DnoHeaderFiles=true -DnoManPages=true

cd target
module_jars=(lib/*)
eligible_main_jars=("*jar-with-dependencies.jar")
main_jar=(${eligible_main_jars[0]})
module_path=$(join_by ";" ${module_jars[@]})
module_path="$main_jar;$module_path"

jpackage \
  --name "Ace of Shades Launcher" \
  --app-version "1.0.0" \
  --description "Launcher app for Ace of Shades, a voxel-based first-person shooter." \
  --linux-shortcut \
  --linux-deb-maintainer "andrewlalisofficial@gmail.com" \
  --linux-menu-group "Game" \
  --linux-app-category "Game" \
  --runtime-image image \
  --main-jar $main_jar \
  --main-class nl.andrewl.aos2_launcher.Launcher \
  --input .

