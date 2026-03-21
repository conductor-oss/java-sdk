# Tool Use Logging in Java Using Conductor :  Request Log, Execute, Response Log, Audit Trail

Tool Use Logging: log tool requests and responses, execute tools, and create audit entries through a sequential pipeline. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Every Tool Call Needs an Audit Trail

When an AI agent calls tools on behalf of users, you need to know exactly what was called, with what arguments, what it returned, how long it took, and who initiated it. Compliance (SOC2, HIPAA) requires audit logs. Debugging requires request-response pairs. Cost tracking requires knowing which tools are called how often.

The logging pattern wraps every tool call with pre-execution and post-execution logging. Before execution: log the tool name, arguments, user ID, and session ID. After execution: log the result, execution time, and any errors. The audit entry combines both into a single record that links the request to the response with timing data.

## The Solution

**You write the request logging, tool execution, response logging, and audit creation logic. Conductor handles the logging pipeline, ensuring audit records are created even on tool failure.**

`LogRequestWorker` records the incoming tool call. tool name, arguments, user ID, session ID, and timestamp,  before execution begins. `ExecuteToolWorker` runs the actual tool and returns results. `LogResponseWorker` records the tool's output, execution duration, and success/error status. `CreateAuditEntryWorker` assembles the request log, response log, and timing data into a complete audit record. Conductor chains these four steps, ensuring logging happens even if the tool fails, and records the entire audit chain.

### What You Write: Workers

Four workers wrap tool calls with observability. Logging the request, executing the tool, logging the response, and creating a complete audit entry.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAuditEntryWorker** | `tl_create_audit_entry` | Creates an audit trail entry for a tool invocation. Takes requestId, userId, sessionId, toolName, toolArgs, result, e... |
| **ExecuteToolWorker** | `tl_execute_tool` | Simulates executing a tool (sentiment analysis). Takes toolName, toolArgs, requestId and returns a fixed sentiment an... |
| **LogRequestWorker** | `tl_log_request` | Logs an incoming tool request. Takes toolName, toolArgs, userId, sessionId and returns a fixed requestId, timestamp, ... |
| **LogResponseWorker** | `tl_log_response` | Logs a tool response. Takes requestId, toolName, result, executionTimeMs, toolStatus and returns a fixed timestamp an... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tl_log_request
    │
    ▼
tl_execute_tool
    │
    ▼
tl_log_response
    │
    ▼
tl_create_audit_entry

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
java -jar target/tool-use-logging-1.0.0.jar

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
java -jar target/tool-use-logging-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_logging \
  --version 1 \
  --input '{"toolName": "test", "toolArgs": "sample-toolArgs", "userId": "TEST-001", "sessionId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_logging -s COMPLETED -c 5

```

## How to Extend

Each worker handles one observability concern. Write structured logs to Elasticsearch or CloudWatch, store audit records in a compliance-grade data store (DynamoDB, PostgreSQL), and the log-execute-log-audit pipeline runs unchanged.

- **LogRequestWorker/LogResponseWorker** (`tl_log_request/response`): write structured logs to Elasticsearch for searchable audit history, or CloudWatch Logs with structured JSON for AWS environments
- **CreateAuditEntryWorker** (`tl_create_audit_entry`): store audit records in a compliance-grade data store (immutable DynamoDB table, append-only PostgreSQL table with row-level security, or a dedicated audit service)
- **ExecuteToolWorker** (`tl_execute_tool`): ensure the tool execution is independent of logging: if logging fails, the tool result should still reach the user (graceful degradation of observability)

Wire in real logging backends like CloudWatch or Datadog; the audit pipeline maintains the same request-response-audit interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-logging/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tooluselogging/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseLoggingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateAuditEntryWorker.java
│       ├── ExecuteToolWorker.java
│       ├── LogRequestWorker.java
│       └── LogResponseWorker.java
└── src/test/java/tooluselogging/workers/
    ├── CreateAuditEntryWorkerTest.java        # 8 tests
    ├── ExecuteToolWorkerTest.java        # 9 tests
    ├── LogRequestWorkerTest.java        # 8 tests
    └── LogResponseWorkerTest.java        # 8 tests

```
