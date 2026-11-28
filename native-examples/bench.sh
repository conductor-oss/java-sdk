#!/bin/bash
# Jepsen-style failure bench for native workflows. See src/main/java/durability/Bench.java for the
# phases, faults and checks, and DURABILITY-BENCH.md for how to read the report.
#
#   BENCH_SERVER_PORT     port of a server built from the conductor branch (default 8099; never 8080)
#   BENCH_SERVER_SCRIPT   script that runs that server in the foreground (exec java ...); required
#                         for the server-kill fault, and to start the server if it is down
#   BENCH_CLUSTER         cluster mode: server ports, e.g. 8101,8102,8103; the script is then called
#                         as <script> <http-port> <db-port> and must point the server at <db-port>
#   BENCH_DB_PORT, BENCH_DB_CONTAINER   the shared Postgres (port, Docker container) in cluster mode
#   everything else       see Bench.java (BENCH_WORKFLOWS, BENCH_MAIN_SECONDS, BENCH_FAULTS, ...)
#
# Exit status: 0 when every check passes, 1 when the checker found a violation.
set -u
case ",${BENCH_SERVER_PORT:-8099},${BENCH_CLUSTER:-}," in *,8080,*) echo "refusing to touch port 8080"; exit 2;; esac
HERE="$(cd "$(dirname "$0")" && pwd)"
LIB="$HERE/build/install/native-examples/lib"
[ -d "$LIB" ] || { echo "run ./gradlew :native-examples:installDist first"; exit 2; }
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cd "$HERE"
case "${1:-run}" in
  run)   exec "$JAVA" -cp "$LIB/*" durability.Bench ;;
  check) exec "$JAVA" -cp "$LIB/*" durability.Checker "$2" ;;
  *)     echo "usage: bench.sh [run | check <run-dir>]"; exit 2 ;;
esac
