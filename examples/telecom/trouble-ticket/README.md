# Trouble Ticket in Java Using Conductor

A Java Conductor workflow example that orchestrates the telecom trouble ticket lifecycle. opening a ticket when a customer reports a service issue, diagnosing the problem to determine its category, assigning the ticket to the appropriate technician based on the diagnosis, resolving the issue, and closing the ticket with the resolution details. Uses [Conductor](https://github.

## Why Trouble Ticket Management Needs Orchestration

Handling a customer trouble ticket requires a structured progression where each step depends on the outcome of the previous one. You open a ticket with the customer's ID, issue type, and description. You diagnose the reported issue to categorize it (network fault, equipment failure, billing dispute, provisioning error). You assign the ticket to a technician with the right skills for that category. The assigned technician resolves the issue and records the resolution. Finally, you close the ticket with the resolution details and timestamp.

If diagnosis categorizes incorrectly, the ticket gets assigned to the wrong team and bounces around for days. If the resolution completes but the ticket isn't closed, SLA metrics look worse than reality and the customer gets follow-up calls about an already-fixed problem. Without orchestration, you'd build a monolithic ticket handler that mixes CRM lookups, network diagnostics, workforce scheduling, and notification logic. making it impossible to swap ticketing systems, test diagnosis rules independently, or track mean time to resolution across issue categories.

## The Solution

**You just write the ticket creation, issue diagnosis, technician assignment, resolution, and ticket closure logic. Conductor handles diagnostic retries, dispatch routing, and ticket resolution audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Ticket creation, diagnostic routing, technician dispatch, and resolution tracking workers each manage one phase of customer issue resolution.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `tbt_assign` | Assigns the ticket to a technician with the right skills based on the diagnosed category. |
| **CloseWorker** | `tbt_close` | Closes the ticket with the resolution details and records the closure timestamp. |
| **DiagnoseWorker** | `tbt_diagnose` | Diagnoses the reported issue to categorize it (network fault, equipment failure, billing, provisioning). |
| **OpenWorker** | `tbt_open` | Opens a trouble ticket with the customer ID, issue type, and returns a ticket ID. |
| **ResolveWorker** | `tbt_resolve` | Records the resolution performed by the assigned technician for the ticket. |

Workers implement telecom operations. provisioning, activation, billing,  with realistic outputs. Replace with real OSS/BSS integrations and the workflow stays the same.

### The Workflow

```
tbt_open
    │
    ▼
tbt_diagnose
    │
    ▼
tbt_assign
    │
    ▼
tbt_resolve
    │
    ▼
tbt_close

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
java -jar target/trouble-ticket-1.0.0.jar

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
java -jar target/trouble-ticket-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tbt_trouble_ticket \
  --version 1 \
  --input '{"customerId": "TEST-001", "issueType": "standard", "description": "sample-description"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tbt_trouble_ticket -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real support systems. ServiceNow for ticket management, your diagnostic engine for issue categorization, your workforce scheduler for technician dispatch, and the workflow runs identically in production.

- **OpenWorker** (`tbt_open`): create the ticket in your trouble management system (BMC Remedy, ServiceNow, Salesforce Service Cloud) via its REST API, linking to the customer's CRM record
- **DiagnoseWorker** (`tbt_diagnose`): run automated diagnostics using your OSS fault management system, correlating the customer's issue with active network alarms and recent provisioning changes
- **AssignWorker** (`tbt_assign`): query your workforce management system (ClickSoftware, Oracle Field Service) to find available technicians with the right skill set and assign the ticket
- **ResolveWorker** (`tbt_resolve`): update the ticket with the resolution action performed by the technician, pulled from field service completion reports or technician mobile app submissions
- **CloseWorker** (`tbt_close`): close the ticket in your trouble management system, trigger a customer satisfaction survey via your CRM, and update SLA metrics in your reporting dashboard

Switch diagnostic tools or dispatch systems and the ticket pipeline structure stays the same.

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
trouble-ticket-trouble-ticket/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/troubleticket/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TroubleTicketExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignWorker.java
│       ├── CloseWorker.java
│       ├── DiagnoseWorker.java
│       ├── OpenWorker.java
│       └── ResolveWorker.java
└── src/test/java/troubleticket/workers/
    ├── OpenWorkerTest.java        # 1 tests
    └── ResolveWorkerTest.java        # 1 tests

```
