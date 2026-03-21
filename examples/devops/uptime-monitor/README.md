# Uptime Monitoring in Java with Conductor: Endpoint Health Checks, Alerting, and Escalation

A Java Conductor workflow example for uptime monitoring, endpoint health checks, Slack/email alerting, and on-call escalation. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles parallelism, retries, failure routing, durability, and observability.

## The Problem

You need to monitor the health of multiple endpoints across your infrastructure. Each check involves different concerns. DNS resolution, HTTP availability, TLS certificate validity. When something goes wrong, the right people need to be notified through the right channels (Slack, email, status page), and if failures persist, escalation kicks in (SMS, PagerDuty).

Without orchestration, you'd wire all of this together in a single monolithic script. Managing threads for parallelism, writing if/else chains for routing, building retry loops with backoff, adding try/catch everywhere for failure handling, and bolting on logging to understand what happened. That code becomes brittle, hard to change, and impossible to observe at scale.

## The Solution

**You write the endpoint checks and notification logic. Conductor handles parallel health checking, severity-based routing, escalation policies, and full execution history.**

Each concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of running them in parallel, routing based on results, retrying on failure, tracking every execution, and resuming if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Eight workers cover the monitoring lifecycle, from parallel endpoint checks through result aggregation, multi-channel alerting, and escalation to on-call.

| Worker | What It Does |
|---|---|---|
| **CheckEndpoint** | Performs real HTTP, DNS, and TLS health checks against a single endpoint |
| **AggregateResults** | Combines results from all checks, determines overall system status |
| **SendSlackAlert** | Sends Slack notification via webhook |
| **SendEmailAlert** | Sends email alerts to the ops team |
| **UpdateStatusPage** | Updates the public status page with component statuses |
| **CheckEscalation** | Escalates when failing endpoints meet or exceed threshold |
| **SendSmsAlert** | Sends SMS alerts for critical escalations |
| **PageOncall** | Pages the on-call engineer |
| **RecordHealthy** | Logs healthy status when all endpoints pass |
| **StoreMetrics** | Writes monitoring data points |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls, the workflow and rollback logic stay the same.

Example, the entire CheckEndpoint worker is just a class that makes HTTP/DNS/TLS calls and returns the result:

```java
public class CheckEndpoint implements Worker {
    @Override
    public String getTaskDefName() { return "uptime_check_endpoint"; }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().get("url");
        // Do the actual HTTP, DNS, TLS checks...
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "healthy");
        result.getOutputData().put("responseTimeMs", 183);
        return result;
    }
}

```

No retry logic. No error routing. No thread management. Just the business logic.

### The Workflow

The workflow definition (`workflow.json`) is a simple JSON file that describes how the workers connect. No imperative code. Just declare the flow:

```
PrepareChecks
    │
    ▼
FORK_JOIN_DYNAMIC ──► [CheckEndpoint] x N (parallel)
    │
    ▼
AggregateResults
    │
    ▼
SWITCH (hasFailures?)
    │
    ├── true ──► FORK_JOIN ──► [Slack + Email + StatusPage] (parallel)
    │                │
    │                ▼
    │            CheckEscalation
    │                │
    │                ▼
    │            SWITCH (shouldEscalate?)
    │                ├── true ──► FORK_JOIN ──► [SMS + PagerDuty] (parallel)
    │                └── false
    │
    └── false ──► RecordHealthy
    │
    ▼
StoreMetrics

```

## Running It

### Prerequisites

- **Java 21+**: required by the conductor-oss SDK v5. Verify with `java -version`.
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the uptime monitor automatically.

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
java -jar target/uptime-monitor-1.0.0.jar

```

If port 8080 is taken, use a different host port and set `CONDUCTOR_BASE_URL`:

```bash
docker run -d -p 9090:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3
until curl -sf http://localhost:9090/health > /dev/null; do sleep 2; done
mvn package -DskipTests
CONDUCTOR_BASE_URL=http://localhost:9090/api java -jar target/uptime-monitor-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

Detects if Conductor is already running at `CONDUCTOR_BASE_URL`. If not, starts it via Docker Compose on `CONDUCTOR_PORT`.

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `SLACK_WEBHOOK_URL` | (none) | Fallback Slack webhook URL (workflow input takes priority) |

## Endpoints Checked

The example checks these real public endpoints:

| Endpoint | Expected | Purpose |
|---|---|---|
| https://www.google.com | 200 | Reliable external endpoint |
| https://github.com | 200 | Developer service |
| https://www.cloudflare.com | 200 | CDN/infrastructure provider |
| https://down.example.invalid | 200 | **Always fails**. `.invalid` TLD guarantees DNS failure, no internet dependency |

Each endpoint gets three real network checks:
- **DNS**: hostname resolution via `InetAddress.getAllByName()`
- **HTTP**: GET request with response time measurement via `HttpURLConnection`
- **TLS**: certificate validation and expiry check via `SSLSocket`

## Using the Conductor CLI

You can use the [Conductor CLI](https://github.com/conductor-oss/conductor-cli) to register definitions, start workflows, and inspect executions. The CLI handles the Conductor server side; but **workers must still be running** to poll and execute tasks.

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/uptime-monitor-1.0.0.jar --workers

```

Then use the CLI in a separate terminal to start and manage workflows.

### Install the CLI

```bash
# macOS/Linux
brew tap conductor-oss/conductor && brew install conductor

# or via npm
npm install -g @conductor-oss/conductor-cli

# or direct download
curl -fsSL https://raw.githubusercontent.com/conductor-oss/conductor-cli/main/install.sh | sh

```

### Start Conductor locally

```bash
conductor server start
conductor server status

```

### Register tasks and workflow

```bash
# Register task definitions
conductor task create src/main/resources/task-defs.json

# Register the workflow
conductor workflow create src/main/resources/workflow.json

```

### Start a workflow run

```bash
conductor workflow start \
  --workflow uptime_monitor \
  --version 1 \
  --input '{"endpoints": [{"url": "https://www.google.com", "name": "Google", "expectedStatus": 200, "timeout": 5000}, {"url": "https://github.com", "name": "GitHub", "expectedStatus": 200, "timeout": 5000}], "notificationChannels": {"slack": {"webhook": "", "channel": "#ops-alerts"}, "email": {"recipients": ["oncall@example.com"]}}, "escalationThreshold": 3}'

```

### Check workflow status

```bash
# Get status of a running workflow
conductor workflow status <workflow_id>

# Get full execution details including task inputs/outputs
conductor workflow get-execution <workflow_id> -c

# Search for recent runs
conductor workflow search -w uptime_monitor -s COMPLETED -c 5

```

### Debug a failed workflow

```bash
# Find failed runs
conductor workflow search -w uptime_monitor -s FAILED

# Inspect execution details
conductor workflow get-execution <workflow_id> -c

# Retry a failed workflow
conductor workflow retry <workflow_id>

# Restart from the beginning
conductor workflow restart <workflow_id>

```

### List registered definitions

```bash
conductor workflow list
conductor task list

```

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## How to Extend

Each worker is a standalone class. Swap the demo implementation for a real one and the workflow, retries, and alerting logic stay the same.

- **SendEmailAlert** (`uptime_send_email_alert`): replace the demo log with AWS SES, SendGrid, or any SMTP client:

```java
// Before (demo):
result.getOutputData().put("sent", true);
result.getOutputData().put("message", "Demo email sent");

// After (real. AWS SES):
SesClient ses = SesClient.create();
ses.sendEmail(SendEmailRequest.builder()
        .destination(d -> d.toAddresses(recipients))
        .message(m -> m.subject(s -> s.data("Uptime Alert"))
                       .body(b -> b.text(t -> t.data(alertBody))))
        .source("alerts@example.com")
        .build());
result.getOutputData().put("sent", true);
result.getOutputData().put("messageId", response.messageId());

```

- **SendSlackAlert** (`uptime_send_slack_alert`): set the `slack.webhook` input to a real Slack Incoming Webhook URL. The worker already makes a real HTTP POST when a webhook is configured.
- **StoreMetrics** (`uptime_store_metrics`): push data points to Prometheus Pushgateway, Datadog, or InfluxDB instead of logging to console.
- **CheckEndpoint** (`uptime_check_endpoint`): already performs real DNS/HTTP/TLS checks. Add custom checks (e.g., response body validation, specific header checks) by extending the `execute()` method.
- **Add new endpoints**: pass additional entries in the `endpoints` workflow input array. No code changes needed.

## Project Structure

```
uptime-monitor/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   ├── workflow.json                # Workflow definition (just JSON.; no code)
│   └── task-defs.json               # Task definitions for CLI registration
├── src/main/java/uptimemonitor/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UptimeMonitorExample.java    # Main entry point (supports --workers mode)
│   └── workers/                     # Each worker = one independent service
│       ├── PrepareChecks.java
│       ├── CheckEndpoint.java       # Real HTTP/DNS/TLS checks
│       ├── AggregateResults.java
│       ├── SendSlackAlert.java      # Real if webhook configured
│       ├── SendEmailAlert.java      # Demo
│       ├── UpdateStatusPage.java    # Demo
│       ├── CheckEscalation.java     # Deterministic threshold check
│       ├── SendSmsAlert.java        # Demo
│       ├── PageOncall.java          # Demo
│       ├── RecordHealthy.java       # Demo
│       └── StoreMetrics.java        # Demo
└── src/test/java/uptimemonitor/workers/
    ├── PrepareChecksTest.java       # 7 tests
    ├── AggregateResultsTest.java    # 9 tests
    └── CheckEndpointTest.java       # 5 tests

```
