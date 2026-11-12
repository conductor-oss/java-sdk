#!/bin/bash
# Durability check for native workflows: runs the kitchensink while killing and restarting the
# worker and the server. Every execution must still pass all its checks.
#
#   SERVER_PORT     port of a server built from the conductor branch (required; never 8080 here)
#   SERVER_START    command that starts that server in the background (required for the restart)
#
# Sequence, while the kitchensink driver runs three executions:
#   1. worker A starts; ~8s in it is killed with SIGKILL
#   2. 20s with no worker at all
#   3. worker B starts with a different id (a new host): continuations A left behind must
#      be picked up by it
#   4. the server is killed with SIGKILL and started again
#   5. worker B is killed and restarted with the same id
set -u
: "${SERVER_PORT:?set SERVER_PORT}"
: "${SERVER_START:?set SERVER_START}"
if [ "$SERVER_PORT" = "8080" ]; then echo "refusing to touch port 8080"; exit 2; fi

HERE="$(cd "$(dirname "$0")" && pwd)"
LIB="$HERE/build/install/native-examples/lib"
LOGS="${LOGS:-$HERE/build/chaos}"
mkdir -p "$LOGS"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
export CONDUCTOR_SERVER_URL="http://localhost:$SERVER_PORT/api" CONDUCTOR_DEV=true BUILD_ID=chaos

wait_server() { for _ in $(seq 1 180); do curl -sf "localhost:$SERVER_PORT/api/metadata/taskdefs" >/dev/null && return 0; sleep 1; done; return 1; }
start_worker() { CONDUCTOR_WORKER_ID="$1" "$JAVA" -cp "$LIB/*" kitchensink.Worker > "$LOGS/worker-$1.log" 2>&1 & echo $!; }
server_pid() { lsof -ti "tcp:$SERVER_PORT" -sTCP:LISTEN | head -1; }
log() { echo "[chaos $(date +%H:%M:%S)] $*"; }

wait_server || { log "server not reachable on $SERVER_PORT"; exit 1; }
A=$(start_worker chaos-a); log "worker A started (pid $A)"
sleep 3
KITCHENSINK_HOST=false "$JAVA" -cp "$LIB/*" kitchensink.Main > "$LOGS/driver.log" 2>&1 & DRIVER=$!
log "driver started (pid $DRIVER)"

sleep 8;  kill -9 "$A"; log "worker A killed"
sleep 20; B=$(start_worker chaos-b); log "worker B started with a new id (pid $B)"
sleep 25; S=$(server_pid); kill -9 "$S"; log "server killed (pid $S)"
sleep 5;  eval "$SERVER_START"; wait_server && log "server back"
sleep 15; kill -9 "$B"; log "worker B killed"
sleep 5;  B=$(start_worker chaos-b); log "worker B restarted with the same id (pid $B)"

for _ in $(seq 1 900); do kill -0 "$DRIVER" 2>/dev/null || break; sleep 1; done
if kill -0 "$DRIVER" 2>/dev/null; then kill "$DRIVER"; log "driver timed out"; fi
wait "$DRIVER"; STATUS=$?
kill "$B" 2>/dev/null
log "driver exit $STATUS: $(grep -c '^  PASS' "$LOGS/driver.log") passed, $(grep -c '^  FAIL' "$LOGS/driver.log") failed"
grep -E "KITCHENSINK|^  FAIL|Exception" "$LOGS/driver.log" | head -20
exit $STATUS
