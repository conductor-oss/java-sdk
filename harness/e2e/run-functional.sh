#!/usr/bin/env bash
# Functional scenarios for adaptive concurrency against a local OSS Conductor started with the Conductor CLI.
# Usage: harness/e2e/run-functional.sh [scenario ...]   (default: all)
set -uo pipefail
cd "$(dirname "$0")"
source ./server.sh
source ./processes.sh
SCENARIOS=("$@")
[ ${#SCENARIOS[@]} -eq 0 ] && SCENARIOS=(weights rotation starvation scaling warmstart cpu heap)
RUN="$PWD/out/functional-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RUN"

(cd ../.. && ./gradlew -q :harness:installDist) || exit 1
trap 'stop_processes; stop_server "$RUN/server"' EXIT
start_server "$RUN/server" || exit 1

overall=0
for s in "${SCENARIOS[@]}"; do
  # Worker settings per scenario: process ceiling, initial target, CPUs and memory the JVM sees, domains.
  export FN_CPUS=2 FN_MEMORY=512m E2E_MAX_PROCESS_CONCURRENCY=64 E2E_INITIAL_PROCESS_CONCURRENCY=0 \
    E2E_MAX_WORKER_CONCURRENCY=64 E2E_TASK_DOMAINS="" E2E_MIN_PROCESS_CONCURRENCY=1
  case "$s" in
    weights)    E2E_MAX_PROCESS_CONCURRENCY=16; E2E_INITIAL_PROCESS_CONCURRENCY=16 ;;
    rotation)   E2E_MAX_PROCESS_CONCURRENCY=2;  E2E_INITIAL_PROCESS_CONCURRENCY=2 ;;
    starvation) E2E_MAX_PROCESS_CONCURRENCY=4;  E2E_INITIAL_PROCESS_CONCURRENCY=4; E2E_TASK_DOMAINS="fn_d=partner_a" ;;
    scaling)    ;;
    warmstart)  E2E_INITIAL_PROCESS_CONCURRENCY=32; E2E_MIN_PROCESS_CONCURRENCY=16 ;;
    cpu)        FN_CPUS=1; E2E_INITIAL_PROCESS_CONCURRENCY=8 ;;
    heap)       ;;
  esac
  OUT="$RUN/$s"; export E2E_LOG_DIR_HOST="$OUT/logs"; mkdir -p "$E2E_LOG_DIR_HOST"
  E2E_LOG_DIR="$E2E_LOG_DIR_HOST" CONDUCTOR_SERVER_URL=http://localhost:8080/api E2E_METRICS_PORT=9993 \
    start_jvm fn-worker "$OUT/worker.log" "$FN_CPUS" "$FN_MEMORY" io.orkes.conductor.harness.e2e.E2eFunctionalWorkerMain
  E2E_SCENARIO="$s" E2E_OUT_DIR="$OUT" E2E_LOG_DIR="$E2E_LOG_DIR_HOST" CONDUCTOR_SERVER_URL=http://localhost:8080/api \
    E2E_WORKER_METRICS=http://localhost:9993 \
    java -cp "../build/install/harness/lib/*" io.orkes.conductor.harness.e2e.E2eFunctionalDriver > "$OUT/driver.log" 2>&1
  status=$?
  [ $status -ne 0 ] && overall=1
  process_status fn-worker "$OUT/worker.log" > "$OUT/worker.txt"
  stop_jvm fn-worker
  sed -n '/==== scenario/,$p' "$OUT/driver.log"
  echo "worker: $(cat "$OUT/worker.txt")"
done
echo "Output: $RUN"
exit $overall
