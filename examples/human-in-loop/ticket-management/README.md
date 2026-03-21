# Ticket Management in Java with Conductor

A Java Conductor workflow that manages the full lifecycle of a support ticket. creating the ticket, classifying it by category and priority, assigning it to the right agent, resolving the issue, and closing the ticket. Given a subject, description, and reporter, the pipeline drives a ticket from creation through resolution to closure. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the create-classify-assign-resolve-close pipeline.

## Managing Tickets from Creation to Closure

When a user reports an issue, the ticket needs to be created, categorized by type and priority, assigned to the right agent, worked on until resolved, and formally closed. Dropping any step means tickets get lost, misrouted, or left in limbo. Each step depends on the previous one. you cannot assign a ticket before classifying its priority, and you cannot close it before it is resolved.

This workflow drives a single ticket through its full lifecycle. The creator generates a ticket ID and records the subject and reporter. The classifier determines category (bug, feature request, question) and priority (critical, high, medium, low). The assigner routes the ticket to an appropriate agent based on category and priority. The resolver records the fix and resolution details. The closer marks the ticket as done and captures final metadata.

## The Solution

**You just write the ticket-creation, classification, assignment, resolution, and closure workers. Conductor handles the full lifecycle sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

CreateTicketWorker generates a unique ID, ClassifyTicketWorker determines category and priority, AssignTicketWorker routes to an agent, ResolveTicketWorker records the fix, and CloseTicketWorker completes the lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AssignTicketWorker** | `tkt_assign` | Routes the ticket to an appropriate agent based on category and priority. |
| **ClassifyTicketWorker** | `tkt_classify` | Determines the ticket's category (bug, feature request, question) and priority level. |
| **CloseTicketWorker** | `tkt_close` | Marks the ticket as closed and records final metadata. |
| **CreateTicketWorker** | `tkt_create` | Creates a new ticket with a unique ID from the subject, description, and reporter. |
| **ResolveTicketWorker** | `tkt_resolve` | Records the resolution details and marks the ticket as resolved. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
tkt_create
    │
    ▼
tkt_classify
    │
    ▼
tkt_assign
    │
    ▼
tkt_resolve
    │
    ▼
tkt_close

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
java -jar target/ticket-management-1.0.0.jar

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
java -jar target/ticket-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tkt_ticket_management \
  --version 1 \
  --input '{"subject": "microservices best practices", "description": "sample-description", "reportedBy": "sample-reportedBy"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tkt_ticket_management -s COMPLETED -c 5

```

## How to Extend

Each worker handles one ticket lifecycle step. connect your helpdesk platform (Zendesk, Jira Service Management, Freshdesk) for creation and routing, and the ticket-management workflow stays the same.

- **AssignTicketWorker** (`tkt_assign`): integrate with Zendesk or Jira Service Management for real agent assignment and SLA tracking
- **ClassifyTicketWorker** (`tkt_classify`): use an LLM or ML classifier for intelligent ticket categorization from description text
- **CloseTicketWorker** (`tkt_close`): connect to your ticketing system to update status and trigger customer satisfaction surveys

Integrate Jira or ServiceNow and the create-classify-assign-resolve-close ticket lifecycle remains intact.

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
ticket-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ticketmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TicketManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignTicketWorker.java
│       ├── ClassifyTicketWorker.java
│       ├── CloseTicketWorker.java
│       ├── CreateTicketWorker.java
│       └── ResolveTicketWorker.java
└── src/test/java/ticketmanagement/workers/
    ├── AssignTicketWorkerTest.java        # 3 tests
    ├── ClassifyTicketWorkerTest.java        # 4 tests
    ├── CloseTicketWorkerTest.java        # 2 tests
    ├── CreateTicketWorkerTest.java        # 2 tests
    └── ResolveTicketWorkerTest.java        # 2 tests

```
