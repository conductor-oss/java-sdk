#!/usr/bin/env bash
# Workers and the downstream simulator for the end-to-end tests, as JVMs on the host. Sourced by run.sh and
# run-functional.sh after server.sh.
#
# A worker JVM is started with -XX:ActiveProcessorCount and -XX:MaxRAM so the JVM and the SDK see the resources of the
# pod in harness/manifests/deployment.yaml: one CPU, 512Mi, hence SerialGC and a heap of about 123 MB. The host does not
# cap the CPU the process actually uses; the SDK reads CPU relative to one core and counts anything above it as full.

HARNESS_LIB="$(cd "$(dirname "${BASH_SOURCE[0]}")/../build/install/harness/lib" && pwd)"
PIDS=()

# start_jvm <name> <log> <cpus> <memory> <main class>: the caller's environment is passed through.
start_jvm() {
  local name="$1" log="$2" cpus="$3" memory="$4" main="$5"
  HOSTNAME="$name" java -XX:ActiveProcessorCount="$cpus" -XX:MaxRAM="$memory" -cp "$HARNESS_LIB/*" "$main" \
    > "$log" 2>&1 &
  PIDS+=("$!")
  eval "PID_${name//-/_}=$!"
}

start_simulator() {
  local log="$1"
  SIM_PORT=18090 java -cp "$HARNESS_LIB/*" io.orkes.conductor.harness.e2e.DependencySimulator > "$log" 2>&1 &
  PIDS+=("$!")
  for _ in $(seq 1 30); do
    curl -s -o /dev/null http://localhost:18090/stats && return 0
    sleep 1
  done
  echo "Simulator did not start; see $log" >&2
  return 1
}

# process_status <name> <log>: one line with whether the JVM still runs and how often it ran out of memory.
process_status() {
  local name="$1" log="$2" pid running=false oom
  pid=$(eval "echo \${PID_${name//-/_}:-}")
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && running=true
  oom=$(grep -c OutOfMemoryError "$log" 2>/dev/null)
  echo "$name running=$running outOfMemory=${oom:-0}"
}

# stop_jvm <name>: stops one JVM started by start_jvm.
stop_jvm() {
  local pid
  pid=$(eval "echo \${PID_${1//-/_}:-}")
  [ -n "$pid" ] && kill "$pid" 2>/dev/null && wait "$pid" 2>/dev/null
  return 0
}

stop_processes() {
  for pid in "${PIDS[@]:-}"; do
    [ -n "$pid" ] && kill "$pid" 2>/dev/null
  done
  wait 2>/dev/null
  PIDS=()
}
