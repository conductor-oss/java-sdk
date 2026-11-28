#!/bin/bash
# The kitchensink under random, seeded faults: workers killed, paused and cut off from the server
# while three executions run. See src/main/java/durability/KitchenSinkChaos.java for the settings
# (CHAOS_SEED, CHAOS_FAULTS, CHAOS_FAULT_INTERVAL, CHAOS_WORKERS, CHAOS_ROUNDS, ...).
# Needs a server built from the conductor branch on CHAOS_SERVER_PORT (default 8099; never 8080)
# and stubs/ai_stub.py on 8765. Check the executions it lists with the conductor repo's
# proposals/native-workflows/spec: check_paths.py, check_runs.py and kitchensink_spec.py --chaos.
# Exit status: 0 when every kitchensink check passed.
set -u
case ",${CHAOS_SERVER_PORT:-8099},${CHAOS_PROXY_PORT:-18197}," in *,8080,*) echo "refusing to touch port 8080"; exit 2;; esac
HERE="$(cd "$(dirname "$0")" && pwd)"
LIB="$HERE/build/install/native-examples/lib"
[ -d "$LIB" ] || { echo "run ./gradlew :native-examples:installDist first"; exit 2; }
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cd "$HERE"
exec "$JAVA" -cp "$LIB/*" durability.KitchenSinkChaos
