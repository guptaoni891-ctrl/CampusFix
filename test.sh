#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src/main/java src/test/java -name '*.java' > out/all-sources.txt
javac -encoding UTF-8 --release 17 -d out @out/all-sources.txt
java -ea -cp out com.campusfix.SmokeTest
