#!/usr/bin/env bash
# Local OSS Conductor for the end-to-end tests, started with the Conductor CLI (`conductor server start`).
# The server keeps its SQLite database in the directory it starts from, so every run gets an empty one.
# Sourced by run.sh and run-functional.sh. The CLI's latest server runs unless E2E_SERVER_VERSION names a release.

E2E_SERVER_VERSION="${E2E_SERVER_VERSION:-latest}"

# start_server <dir>: stops any CLI-managed server, then starts a fresh one with its database in <dir>.
start_server() {
  local dir="$1"
  mkdir -p "$dir"
  conductor server stop >/dev/null 2>&1 || true
  (cd "$dir" && conductor server start --version "$E2E_SERVER_VERSION" > "$dir/start.log" 2>&1) || {
    cat "$dir/start.log"; return 1; }
  for _ in $(seq 1 90); do
    curl -sf http://localhost:8080/health >/dev/null && return 0
    sleep 2
  done
  echo "Conductor did not become healthy; see ~/.conductor-cli/server/conductor.log" >&2
  return 1
}

# stop_server <dir>: stops the server and keeps its log in <dir>.
stop_server() {
  local dir="$1"
  cp ~/.conductor-cli/server/conductor.log "$dir/conductor.log" 2>/dev/null || true
  conductor server stop >/dev/null 2>&1 || true
}
