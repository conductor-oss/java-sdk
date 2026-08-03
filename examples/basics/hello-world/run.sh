#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

load_repo_env() {
  local dir="$PWD"
  while [ "$dir" != "/" ]; do
    if [ -f "$dir/.env" ]; then
      set -a
      . "$dir/.env"
      set +a
      echo "Loaded environment from $dir/.env"
      return
    fi
    dir="$(dirname "$dir")"
  done
}

load_repo_env

echo "=== Hello World (Java) ==="
echo ""

EXPLICIT_SERVER_URL="${CONDUCTOR_SERVER_URL:-}"

run_with_external_server() {
    local health_url="${CONDUCTOR_SERVER_URL%/api}/health"

    if ! curl -sf "$health_url" > /dev/null 2>&1; then
        echo "No healthy Conductor server found at $health_url." >&2
        echo "Fix CONDUCTOR_SERVER_URL, or unset it to let this launcher start Conductor." >&2
        exit 1
    fi

    echo "Using the explicitly configured Conductor server at $CONDUCTOR_SERVER_URL"
    echo "Building and running the example..."
    echo ""
    docker run --rm -v "$PWD":/work -v "$HOME/.m2":/root/.m2 -w /work maven:3.9-eclipse-temurin-21 mvn -q package -DskipTests 2>/dev/null || docker run --rm -v "$PWD":/work -v "$HOME/.m2":/root/.m2 -w /work maven:3.9-eclipse-temurin-21 mvn package -DskipTests
    CONDUCTOR_SERVER_URL="$CONDUCTOR_SERVER_URL" java -jar target/hello-world-1.0.0.jar "$@"
}

managed_conductor_is_running() {
    [ -n "$(docker compose ps --status running --quiet conductor 2>/dev/null)" ]
}

managed_host_port() {
    local container_port="$1"
    local binding
    binding="$(docker compose port conductor "$container_port" 2>/dev/null)"
    echo "${binding##*:}"
}

wait_for_managed_conductor() {
    local attempt
    for attempt in {1..60}; do
        if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done

    echo "Conductor did not become healthy at $HEALTH_URL." >&2
    docker compose logs conductor >&2
    return 1
}

if [ -n "$EXPLICIT_SERVER_URL" ]; then
    CONDUCTOR_SERVER_URL="$EXPLICIT_SERVER_URL"
    export CONDUCTOR_SERVER_URL
    run_with_external_server "$@"
else
    CONDUCTOR_PORT="${CONDUCTOR_PORT:-8080}"
    CONDUCTOR_UI_PORT="${CONDUCTOR_UI_PORT:-1234}"
    CONDUCTOR_SERVER_URL="http://localhost:${CONDUCTOR_PORT}/api"
    HEALTH_URL="http://localhost:${CONDUCTOR_PORT}/health"
    export CONDUCTOR_PORT CONDUCTOR_UI_PORT CONDUCTOR_SERVER_URL

    if managed_conductor_is_running; then
        CONDUCTOR_PORT="$(managed_host_port 8080)"
        CONDUCTOR_UI_PORT="$(managed_host_port 5000)"
        CONDUCTOR_SERVER_URL="http://localhost:${CONDUCTOR_PORT}/api"
        HEALTH_URL="http://localhost:${CONDUCTOR_PORT}/health"
        export CONDUCTOR_PORT CONDUCTOR_UI_PORT CONDUCTOR_SERVER_URL
        echo "Reusing this example's managed Conductor server at $CONDUCTOR_SERVER_URL"
    else
        if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
            echo "A Conductor server is already responding at $CONDUCTOR_SERVER_URL," >&2
            echo "but it was not started by this example." >&2
            echo "Set CONDUCTOR_SERVER_URL=$CONDUCTOR_SERVER_URL to reuse it explicitly," >&2
            echo "or choose unused ports, for example:" >&2
            echo "  CONDUCTOR_PORT=18080 CONDUCTOR_UI_PORT=11234 ./run.sh" >&2
            exit 1
        fi

        echo "Starting a managed Conductor server on port $CONDUCTOR_PORT..."
        docker compose up -d conductor
    fi

    wait_for_managed_conductor
    echo "Building and running the example..."
    echo ""
    docker compose run --build --rm hello-world "$@"
    echo ""
    echo "Conductor is still running."
    echo "UI: http://localhost:${CONDUCTOR_UI_PORT}"
    echo "Stop it with: docker compose down"
fi
