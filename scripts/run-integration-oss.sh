#!/usr/bin/env bash
#
# Spin up a local Conductor OSS stack and run the `tests` module's
# integration suite against it, mirroring the `integration-tests-oss` job in
# .github/workflows/integration-tests-oss.yml. Orkes-Enterprise-only test
# classes are annotated with
# @DisabledIfEnvironmentVariable(named = "CONDUCTOR_SERVER_TYPE", matches = "oss")
# so they skip themselves when it's set (see the individual test files for
# the empirically-confirmed gaps).
#
# The stack (Conductor OSS + Postgres) is defined in
# scripts/docker-compose-oss.yaml and is torn down automatically on exit. The
# image is always pulled before starting, since `latest` (the local default)
# is a mutable tag and a cached copy would otherwise go stale silently.
#
# Usage:
#   scripts/run-integration-oss.sh [--keep-up] [--version <tag>] [--include-gated] [-- gradle args]
# Examples:
#   scripts/run-integration-oss.sh
#   scripts/run-integration-oss.sh --version 3.32.0-rc18
#   scripts/run-integration-oss.sh --keep-up
#   scripts/run-integration-oss.sh --include-gated       # also run tests normally skipped as Orkes-only
#   scripts/run-integration-oss.sh -- --tests "*WorkflowClientTests"
set -euo pipefail

KEEP_UP=0
INCLUDE_GATED=0
extra=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-up) KEEP_UP=1; shift ;;
    --version) OSS_CONDUCTOR_VERSION="${2:?--version needs a tag}"; shift 2 ;;
    --include-gated) INCLUDE_GATED=1; shift ;;
    -h|--help)
      echo "Usage: $0 [--keep-up] [--version <tag>] [--include-gated] [-- gradle args]"
      exit 0
      ;;
    --) shift; extra=("$@"); break ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

export OSS_CONDUCTOR_VERSION="${OSS_CONDUCTOR_VERSION:-latest}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-oss.yaml"
cd "${REPO_ROOT}"

compose() { docker compose -f "${COMPOSE_FILE}" "$@"; }

cleanup() {
  if [[ "${KEEP_UP}" == "1" ]]; then
    echo "--keep-up set: leaving the OSS stack running. Tear down with:"
    echo "  docker compose -f ${COMPOSE_FILE} down -v"
    return
  fi
  echo "Tearing down Conductor OSS stack..."
  compose down -v || true
}
trap cleanup EXIT

echo "Using conductoross/conductor:${OSS_CONDUCTOR_VERSION}"

# `docker compose up` only pulls an image when it is missing locally, so a
# previously-cached `latest` (or any other mutable tag) would silently be
# reused instead of getting the current version. Pull unconditionally so the
# stack always reflects the tag we just printed.
echo "Pulling conductoross/conductor:${OSS_CONDUCTOR_VERSION} to ensure it's current..."
compose pull conductor-server

echo "Starting Conductor OSS stack..."
compose up -d

echo "Waiting for Conductor to be healthy..."
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"
deadline=$(( SECONDS + HEALTH_TIMEOUT ))
until curl -sf http://localhost:8080/health >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "Error: Conductor did not become healthy within ${HEALTH_TIMEOUT}s." >&2
    compose logs conductor-server || true
    exit 1
  fi
  sleep 5
done
echo "Conductor is up."

export CONDUCTOR_SERVER_URL="http://localhost:8080/api"

if [[ "${INCLUDE_GATED}" == "1" ]]; then
  echo "--include-gated set: leaving CONDUCTOR_SERVER_TYPE unset, so tests normally" \
       "skipped as Orkes-only will run against OSS too."
  unset CONDUCTOR_SERVER_TYPE || true
else
  export CONDUCTOR_SERVER_TYPE="oss"
fi


# --rerun-tasks: the `test` task's up-to-date check only considers the compiled
# test classpath, not env vars like CONDUCTOR_SERVER_URL/CONDUCTOR_SERVER_TYPE
# or the state of the live server underneath. Without this, Gradle can report
# BUILD SUCCESSFUL while silently reusing a stale cached result from a
# previous run against a different server/tag/gating state instead of
# actually executing anything.
./gradlew :tests:test -PIntegrationTests --rerun-tasks ${extra[@]+"${extra[@]}"}
