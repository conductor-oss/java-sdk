# Incident Response Automation in Java with Conductor

The PagerDuty alert fired at 2:14 AM. The on-call engineer saw the Slack notification, opened their laptop, SSHed into the wrong box, ran `top` for a while, then remembered they needed to check the dashboard, which was on a different VPN. Forty minutes later they found the actual issue: the API gateway was at 95% CPU. They scaled it up manually, forgot to create an incident ticket, and went back to sleep. The status page still says "All Systems Operational." Tomorrow, nobody will know what happened, what was tried, or whether the fix actually worked. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate incident response end-to-end: create the ticket, page the responder, pull diagnostics, and attempt automated remediation, with a complete audit trail of every step.

## When Incidents Strike

A production alert fires at 2 AM. Someone needs to create an incident ticket, page the on-call engineer, pull CPU and error-rate diagnostics from the affected service, and attempt an automated fix (like scaling up replicas), all before the SLA window closes. If any step fails silently or runs out of order, mean-time-to-recovery climbs and customers notice.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the incident handling logic. Conductor handles step sequencing, retries, and the complete incident audit trail.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle the incident lifecycle. Creating the ticket, paging the responder, pulling diagnostics, and attempting automated remediation.

| Worker | Task | What It Does |
|---|---|---|
| `CreateIncidentWorker` | `ir_create_incident` | Creates a tracked incident record with ID `INC-42` and the provided severity level (e.g., P1) |
| `NotifyOncallWorker` | `ir_notify_oncall` | Pages the current on-call engineer with the incident ID so they can begin investigation |
| `GatherDiagnosticsWorker` | `ir_gather_diagnostics` | Collects live metrics from the affected service. Returns CPU usage (95%) and error rate (5%) |
| `AutoRemediateWorker` | `ir_auto_remediate` | Attempts automated recovery (scales up 2 replicas) and reports whether remediation succeeded |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls, the workflow and rollback logic stay the same.

### The Workflow

```
ir_create_incident
    |
    v
ir_notify_oncall
    |
    v
ir_gather_diagnostics
    |
    v
ir_auto_remediate

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
java -jar target/incident-response-1.0.0.jar

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

```bash
conductor workflow start \
  --workflow incident_response_workflow \
  --version 1 \
  --input '{"alertName": "high-error-rate", "service": "api-gateway", "severity": "P1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w incident_response_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one incident response stage. Replace the simulated calls with PagerDuty, Kubernetes, or Prometheus APIs, and the response workflow runs unchanged.

- **`CreateIncidentWorker`**: Create incidents in PagerDuty, Opsgenie, or ServiceNow via their REST APIs instead of returning a hardcoded incident ID.

- **`GatherDiagnosticsWorker`**: Query Prometheus, Datadog, or CloudWatch for real CPU/memory/error-rate metrics, and pull recent logs from Elasticsearch or Loki.

- **`AutoRemediateWorker`**: Call the Kubernetes API to scale deployments, invoke AWS Auto Scaling actions, or trigger Ansible runbooks for automated recovery.

Replace the simulated calls with real PagerDuty and diagnostics APIs; the workflow contract stays unchanged.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
incident-response-incident-response/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/incidentresponse/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java             # Main entry point
│   └── workers/
│       ├── AutoRemediateWorker.java # Attempts auto-remediation and reports result
│       ├── CreateIncidentWorker.java # Creates tracked incident with ID and severity
│       ├── GatherDiagnosticsWorker.java # Collects CPU, error rate from affected service
│       └── NotifyOncallWorker.java  # Pages on-call engineer with incident context
└── src/test/java/incidentresponse/workers/
    ├── AutoRemediateWorkerTest.java
    ├── CreateIncidentWorkerTest.java
    ├── GatherDiagnosticsWorkerTest.java
    └── NotifyOncallWorkerTest.java

```
