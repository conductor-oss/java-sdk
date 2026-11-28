#!/bin/bash
# Worker failover: the kitchensink on several worker processes while the worker that holds an
# execution is killed (SIGKILL), paused (SIGSTOP), GC-paused (short SIGSTOPs) or cut off from the
# server, then proving from the server's records that no execution waited for that worker and every
# one finished. The server is never touched. See
# src/main/java/durability/Failover.java for the settings (FAILOVER_SERVER_URL, FAILOVER_WORKERS,
# FAILOVER_DRIVERS, ...). Needs a server built from the conductor branch with
# conductor.workflow-message-queue.enabled=true and conductor.ai.litellm.baseURL=http://localhost:8765,
# and stubs/ai_stub.py on 8765.
# Exit status: 0 when every fault was handed over or ridden out and every execution completed and
# replays. `failover.sh check <run dir>` judges a finished run again from the files it saved.
set -u
case "${FAILOVER_SERVER_URL:-http://localhost:8099/api}" in *:8080/*|*:8080) echo "refusing to touch port 8080"; exit 2;; esac
HERE="$(cd "$(dirname "$0")" && pwd)"
LIB="$HERE/build/install/native-examples/lib"
[ -d "$LIB" ] || { echo "run ./gradlew :native-examples:installDist first"; exit 2; }
if [ "${1:-}" != "check" ]; then
  curl -sf -o /dev/null "http://localhost:8765/.well-known/agent-card.json" || { echo "start stubs/ai_stub.py 8765 first"; exit 2; }
fi
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cd "$HERE"
exec "$JAVA" -cp "$LIB/*" durability.Failover "$@"
