#!/usr/bin/env bash

function join_by {
   local d=${1-} f=${2-}
   if shift 2; then
     printf %s "$f" "${@/#/$d}"
   fi
}

mvn clean package
cd target
module_jars=(lib/*)
eligible_main_jars=("*.jar")
main_jar=(${eligible_main_jars[0]})
module_path=$(join_by ":" ${module_jars[@]})
module_path="$main_jar:$module_path"
echo $module_path
jpackage \
  --name "Ace of Shades Launcher" \
  --app-version "1.0.0" \
  --description "Launcher app for Ace of Shades, a voxel-based first-person shooter." \
  --icon ../icon.ico \
  --linux-shortcut \
  --linux-deb-maintainer "andrewlalisofficial@gmail.com" \
  --linux-menu-group "Game" \
  --linux-app-category "Game" \
  --module-path "$module_path" \
  --module aos2_launcher/nl.andrewl.aos2_launcher.Launcher \
  --add-modules jdk.crypto.cryptoki

