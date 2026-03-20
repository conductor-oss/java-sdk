# Zendesk Integration in Java Using Conductor

A Java Conductor workflow that manages a Zendesk support ticket lifecycle end-to-end .  creating a ticket from a requester's email and subject, classifying it by priority, routing it to the appropriate agent based on priority and category, and resolving the ticket. Given a requester email, subject, description, and category, the pipeline produces a ticket ID, priority classification, assigned agent, and resolution status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the create-classify-route-resolve pipeline.

## Automating Zendesk Ticket Triage and Resolution

When a support request arrives, you need to create a ticket in Zendesk, analyze its subject and description to determine priority (urgent, high, normal, low), route it to the right agent or group based on priority and category (billing goes to finance, bugs go to engineering), and ultimately mark it as resolved. Each step depends on the previous one .  you cannot classify without a ticket ID, and you cannot route without knowing the priority.

Without orchestration, you would chain Zendesk REST API calls manually, manage ticket IDs, priority levels, and agent assignments between steps, and handle edge cases like misrouted tickets or classification failures. Conductor sequences the pipeline and routes ticket IDs, priorities, and agent IDs between workers automatically.

## The Solution

**You just write the support ticket workers. Creation, priority classification, agent routing, and resolution. Conductor handles ticket-to-resolution sequencing, Zendesk API retries, and priority-based routing of ticket IDs between classification, assignment, and resolution stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the support lifecycle: CreateTicketWorker opens tickets, ClassifyTicketWorker determines priority, RouteTicketWorker assigns the right agent, and ResolveTicketWorker closes the case.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyTicketWorker** | `zd_classify` | Classifies ticket priority. |
| **CreateTicketWorker** | `zd_create_ticket` | Creates a Zendesk support ticket. |
| **ResolveTicketWorker** | `zd_resolve` | Resolves a ticket. |
| **RouteTicketWorker** | `zd_route` | Routes ticket to an agent. |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### The Workflow

```
zd_create_ticket
    │
    ▼
zd_classify
    │
    ▼
zd_route
    │
    ▼
zd_resolve
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
java -jar target/zendesk-integration-1.0.0.jar
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
| `ZENDESK_SUBDOMAIN` | _(none)_ | Zendesk subdomain (e.g., `yourcompany` for `yourcompany.zendesk.com`). Required for live mode. |
| `ZENDESK_EMAIL` | _(none)_ | Zendesk agent email for API auth. Required for live mode. |
| `ZENDESK_API_TOKEN` | _(none)_ | Zendesk API token. When set with `ZENDESK_SUBDOMAIN`, CreateTicketWorker calls the Zendesk REST API. When unset, all workers run in simulated mode with `[SIMULATED]` output prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/zendesk-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow zendesk_integration_440 \
  --version 1 \
  --input '{"requesterEmail": "user@example.com", "subject": "test-value", "description": "test-value", "category": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w zendesk_integration_440 -s COMPLETED -c 5
```

## How to Extend

Swap in Zendesk REST API calls. POST /tickets for creation, your classification model for priority, and Zendesk's assignment API for agent routing. The workflow definition stays exactly the same.

- **CreateTicketWorker** (`zd_create_ticket`): use the Zendesk REST API (POST /api/v2/tickets) to create real support tickets with requester, subject, and description
- **ClassifyTicketWorker** (`zd_classify`): use NLP or keyword-based rules against the ticket subject/description to assign real priority levels, or integrate with Zendesk's Intelligent Triage
- **RouteTicketWorker** (`zd_route`): use the Zendesk REST API (PUT /api/v2/tickets/{id}) to assign real tickets to agents or groups based on priority and category

Wire each worker to the Zendesk REST API while keeping the same return structure, and the triage-to-resolution pipeline adapts without changes.

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
zendesk-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/zendeskintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ZendeskIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyTicketWorker.java
│       ├── CreateTicketWorker.java
│       ├── ResolveTicketWorker.java
│       └── RouteTicketWorker.java
└── src/test/java/zendeskintegration/workers/
    ├── ClassifyTicketWorkerTest.java        # 2 tests
    ├── CreateTicketWorkerTest.java        # 2 tests
    ├── ResolveTicketWorkerTest.java        # 2 tests
    └── RouteTicketWorkerTest.java        # 2 tests
```
