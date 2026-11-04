#!/usr/bin/env bash
# Runs the adaptive concurrency end-to-end test against a local OSS Conductor started with the Conductor CLI.
# Usage: harness/e2e/run.sh [adaptive|fixed]
set -euo pipefail
cd "$(dirname "$0")"
source ./server.sh
source ./processes.sh
export E2E_MODE="${1:-adaptive}"
OUT="$PWD/out/$E2E_MODE-$(date +%Y%m%d-%H%M%S)"
export E2E_LOG_DIR_HOST="$OUT/logs"
mkdir -p "$E2E_LOG_DIR_HOST"

(cd ../.. && ./gradlew -q :harness:installDist)
trap 'stop_processes; stop_server "$OUT/server"' EXIT
start_server "$OUT/server"
start_simulator "$OUT/simulator.log"
export CONDUCTOR_SERVER_URL=http://localhost:8080/api SIM_URL=http://localhost:18090 E2E_LOG_DIR="$E2E_LOG_DIR_HOST"
E2E_METRICS_PORT=9991 start_jvm worker-0 "$OUT/worker-0.log" 1 512m io.orkes.conductor.harness.e2e.E2eWorkerMain
E2E_METRICS_PORT=9992 start_jvm worker-1 "$OUT/worker-1.log" 1 512m io.orkes.conductor.harness.e2e.E2eWorkerMain

# A failed check must not end the script before the output is collected.
set +e
E2E_OUT_DIR="$OUT" java -cp "../build/install/harness/lib/*" io.orkes.conductor.harness.e2e.E2eDriver | tee "$OUT/driver.log"
status=${PIPESTATUS[0]}
set -e

for w in worker-0 worker-1; do
  process_status "$w" "$OUT/$w.log" | tee -a "$OUT/workers.txt"
done
echo "Output: $OUT"
exit "$status"
