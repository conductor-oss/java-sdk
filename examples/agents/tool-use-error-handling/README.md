# Tool Use Error Handling in Java Using Conductor :  Primary Tool with Fallback on Failure

Tool Use Error Handling. tries a primary tool and falls back to an alternative tool on failure via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Tools Fail :  Have a Backup Plan

Your primary weather API returns a 503 because it's having an outage. Your primary search engine is rate-limiting you. Your primary database is under maintenance. If the agent simply reports "tool failed" to the user, it's a poor experience. especially when an alternative tool could have answered the question.

The error handling pattern tries the primary tool first, checks its status, and on failure routes to a fallback tool that serves the same purpose through a different provider or method. The primary might be a paid, high-quality API; the fallback might be a free, lower-quality alternative. Either way, the user gets an answer. Conductor's `SWITCH` task makes this failover routing explicit, and every execution records which tool served the request.

## The Solution

**You write the primary and fallback tool logic. Conductor handles the success/failure routing, failover decisions, and reliability tracking per tool.**

`TryPrimaryToolWorker` executes the preferred tool and returns success/failure status with the result or error details. Conductor's `SWITCH` routes on status: success goes to `FormatSuccessWorker` which formats the primary tool's output. Failure (the default case) goes to `TryFallbackToolWorker` which executes the alternative tool, then `FormatFallbackWorker` which formats the fallback result with a note about which tool was used. Conductor records which tool served each request, enabling reliability analysis per tool.

### What You Write: Workers

Four workers implement failover. Trying the primary tool, checking its status, and routing to a fallback tool on failure before formatting the result.

| Worker | Task | What It Does |
|---|---|---|
| **FormatFallbackWorker** | `te_format_fallback` | Formats the result from a successful fallback tool invocation. |
| **FormatSuccessWorker** | `te_format_success` | Formats the result from a successful primary tool invocation. |
| **TryFallbackToolWorker** | `te_try_fallback_tool` | Attempts the fallback tool after the primary tool has failed. Returns a successful geocoding result. |
| **TryPrimaryToolWorker** | `te_try_primary_tool` | Attempts to call the primary tool. Simulates a failure by returning toolStatus="failure" with a 503 service-unavailab... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
te_try_primary_tool
    │
    ▼
SWITCH (route_on_status_ref)
    ├── success: te_format_success
    └── default: te_try_fallback_tool -> te_format_fallback

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/tool-use-error-handling-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/tool-use-error-handling-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_error_handling \
  --version 1 \
  --input '{"query": "What is workflow orchestration?", "primaryTool": "sample-primaryTool", "fallbackTool": "sample-fallbackTool"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_error_handling -s COMPLETED -c 5

```

## How to Extend

Each tool worker wraps one API provider. Connect primary and fallback services (e.g., Google Maps as primary, OpenStreetMap as fallback), add proper timeout and error classification, and the try-primary-or-fallback failover workflow runs unchanged.

- **TryPrimaryToolWorker** (`te_try_primary_tool`): implement proper timeout handling and error classification (retryable vs. permanent failures) to determine whether to fallback or retry the primary
- **TryFallbackToolWorker** (`te_try_fallback_tool`): chain multiple fallbacks: if the first fallback also fails, try a third option (cached result, degraded response, or queue for later processing)
- **FormatFallbackWorker** (`te_format_fallback`): include transparency about which tool served the request and any quality differences between primary and fallback results

Connect real primary and fallback APIs; the error-handling workflow keeps the same success/failure routing interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-error-handling/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tooluseerror/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseErrorHandlingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FormatFallbackWorker.java
│       ├── FormatSuccessWorker.java
│       ├── TryFallbackToolWorker.java
│       └── TryPrimaryToolWorker.java
└── src/test/java/tooluseerror/workers/
    ├── FormatFallbackWorkerTest.java        # 9 tests
    ├── FormatSuccessWorkerTest.java        # 8 tests
    ├── TryFallbackToolWorkerTest.java        # 8 tests
    └── TryPrimaryToolWorkerTest.java        # 8 tests

```
