#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

java_cmd=java
javac_cmd=javac
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  java_cmd="$JAVA_HOME/bin/java"
  javac_cmd="$JAVA_HOME/bin/javac"
fi

mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
"$javac_cmd" -encoding UTF-8 --release 17 -d out @out/sources.txt
"$java_cmd" -cp out com.campusfix.CampusFixApp --file "$@"
