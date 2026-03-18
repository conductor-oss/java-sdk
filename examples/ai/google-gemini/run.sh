#!/usr/bin/env bash
set -eo pipefail
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

echo "=== Google Gemini (Java) ==="
echo ""

GEMINI_MODEL="${GEMINI_MODEL:-gemini-2.5-flash}"
export GEMINI_MODEL

if [ -z "$GOOGLE_API_KEY" ]; then
  echo "ERROR: GOOGLE_API_KEY is required. Set it in your .env file or environment."
  echo "  export GOOGLE_API_KEY=your-api-key-here"
  exit 1
fi
echo "Google AI API key detected."
echo "Model: $GEMINI_MODEL"
echo ""

CONDUCTOR_BASE_URL="${CONDUCTOR_BASE_URL:-http://localhost:${CONDUCTOR_PORT:-8080}/api}"
HEALTH_URL="${CONDUCTOR_BASE_URL%/api}/health"

CONDUCTOR_PORT="${CONDUCTOR_PORT:-$(echo "$CONDUCTOR_BASE_URL" | sed -n 's|.*://[^:]*:\([0-9]*\).*|\1|p')}"
CONDUCTOR_PORT="${CONDUCTOR_PORT:-8080}"
export CONDUCTOR_PORT

if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
    echo "Conductor is running at $CONDUCTOR_BASE_URL"
    echo "Building and running the example..."
    echo ""
    mvn -q package -DskipTests 2>/dev/null || mvn package -DskipTests
    CONDUCTOR_BASE_URL="$CONDUCTOR_BASE_URL" GOOGLE_API_KEY="${GOOGLE_API_KEY:-}" GEMINI_MODEL="$GEMINI_MODEL" java -jar target/google-gemini-1.0.0.jar "$@"
else
    echo "Conductor not found at $HEALTH_URL"
    echo "Starting Conductor on port $CONDUCTOR_PORT with Docker Compose..."
    echo ""
    echo "Tip: If port $CONDUCTOR_PORT is taken, run: CONDUCTOR_PORT=9090 ./run.sh"
    echo ""
    docker compose up --build --abort-on-container-exit
fi
