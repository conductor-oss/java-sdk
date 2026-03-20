# Helpdesk Routing in Java with Conductor :  Tier-Based Ticket Routing via SWITCH

A Java Conductor workflow that routes helpdesk tickets to the right support tier .  classifying the issue by complexity, then using a SWITCH task to route to Tier 1 (simple questions), Tier 2 (technical issues), or Tier 3 (escalations and critical problems). Given an `issueDescription` and `customerId`, the pipeline classifies the ticket and routes it to the appropriate handler. Uses [Conductor](https://github.com/conductor-oss/conductor) to implement tier-based conditional routing with SWITCH.

## Getting Tickets to the Right Team on the First Try

Customers expect their support tickets to reach someone who can actually help. Routing a complex technical issue to Tier 1 wastes the customer's time with a handoff. Routing a simple password reset to Tier 3 wastes engineer time. The classification needs to happen automatically and the routing needs to be deterministic .  the right tier every time, based on issue complexity.

This workflow classifies the ticket first, then routes it to the correct tier using a Conductor SWITCH task. The classifier analyzes the issue description and customer context to determine which tier should handle it. The SWITCH task reads the tier assignment and routes to `hdr_tier1` (general support for common questions), `hdr_tier2` (technical support for product issues), or `hdr_tier3` (engineering escalation for critical problems). If the classification does not match any defined tier, the default case routes to Tier 1.

## The Solution

**You just write the ticket-classification and tier-specific handler workers. Conductor handles the SWITCH-based routing to the correct support tier.**

Four workers handle the routing .  one classifier and three tier-specific handlers. The classifier determines the appropriate tier based on issue complexity and customer context. The SWITCH task makes routing declarative: Conductor reads the tier from the classifier's output and sends the ticket to the matching handler. Each tier handler processes tickets differently. Tier 1 uses knowledge base lookups, Tier 2 investigates technical details, Tier 3 engages engineering directly.

### What You Write: Workers

ClassifyWorker determines the support tier, then the SWITCH routes to Tier1Worker for common questions, Tier2Worker for technical issues, or Tier3Worker for critical escalations, each handler focuses on its specialty.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `hdr_classify` | Analyzes the issue description and customer context to determine which support tier should handle it. |
| **Tier1Worker** | `hdr_tier1` | Handles basic support: common questions, password resets, and knowledge base lookups. |
| **Tier2Worker** | `hdr_tier2` | Handles technical support: product issues, configuration problems, and debugging. |
| **Tier3Worker** | `hdr_tier3` | Handles engineering escalations: critical problems requiring senior engineer investigation. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
hdr_classify
    │
    ▼
SWITCH (hdr_switch_ref)
    ├── tier1: hdr_tier1
    ├── tier2: hdr_tier2
    ├── tier3: hdr_tier3
    └── default: hdr_tier1
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
java -jar target/helpdesk-routing-1.0.0.jar
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
java -jar target/helpdesk-routing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hdr_helpdesk_routing \
  --version 1 \
  --input '{"issueDescription": "test-value", "customerId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hdr_helpdesk_routing -s COMPLETED -c 5
```

## How to Extend

Each worker handles one tier of support .  connect your helpdesk platform (Zendesk, Freshdesk, Intercom) for classification and your knowledge base for Tier 1 lookups, and the tier-routing workflow stays the same.

- **ClassifyWorker** (`hdr_classify`): swap in an ML model or LLM for intelligent ticket classification based on natural language
- **Tier1Worker** (`hdr_tier1`): integrate with Zendesk or Freshdesk to create and assign Tier 1 tickets
- **Tier2Worker** (`hdr_tier2`): connect to Jira Service Management for technical issue tracking and SLA management

Plug in a real NLP classifier or your ticketing system and the SWITCH-based tier routing operates without modification.

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
helpdesk-routing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/helpdeskrouting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HelpdeskRoutingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── Tier1Worker.java
│       ├── Tier2Worker.java
│       └── Tier3Worker.java
└── src/test/java/helpdeskrouting/workers/
    ├── ClassifyWorkerTest.java        # 4 tests
    ├── Tier1WorkerTest.java        # 2 tests
    ├── Tier2WorkerTest.java        # 2 tests
    └── Tier3WorkerTest.java        # 2 tests
```
