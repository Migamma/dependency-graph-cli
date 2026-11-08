#!/usr/bin/env bash
# Собирает проект и запускает приложение.
# Использование: ./run.sh [путь к config.yaml]
set -euo pipefail

cd "$(dirname "$0")"

JAR="target/dependency-graph-cli-1.0-SNAPSHOT.jar"

mvn -q package
java -jar "$JAR" "$@"
