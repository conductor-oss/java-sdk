#!/bin/bash
# Latency: 10 sequential 1 ms worker tasks, as a native workflow and as JSON, one execution at a
# time; prints the server's and the caller's time and where each task's time went. See
# src/main/java/durability/Latency.java for the settings (LATENCY_SERVER_URL, LATENCY_RUNS, ...).
# Polling settings go in JAVA_OPTS, e.g. JAVA_OPTS="-Dconductor.worker.all.pollingInterval=10".
set -u
case "${LATENCY_SERVER_URL:-http://localhost:8099/api}" in *:8080/*|*:8080) echo "refusing to touch port 8080"; exit 2;; esac
HERE="$(cd "$(dirname "$0")" && pwd)"
LIB="$HERE/build/install/native-examples/lib"
[ -d "$LIB" ] || { echo "run ./gradlew :native-examples:installDist first"; exit 2; }
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA" ${JAVA_OPTS:-} -cp "$LIB/*" durability.Latency "$@"
