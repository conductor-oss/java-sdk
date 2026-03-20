# Event Routing in Java Using Conductor

An order-cancellation event lands in the user-profile handler. The handler doesn't know what to do with it, silently drops it, and the customer's order stays active. Meanwhile, a user-signup event hits the order processor, which tries to look up a nonexistent order ID and throws a NullPointerException, taking down the entire event consumer. You restart the consumer, but the misrouted events are gone. When every event flows through the same pipe with a big `if/else` block deciding where it goes, one wrong routing decision cascades into data corruption, silent failures, and lost events. This workflow extracts each event's domain, routes it to the correct processor via a SWITCH task, and gives you a full trace of every routing decision. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to route incoming events to domain-specific processors based on the event's domain. User events go to the user processor, order events go to the order processor, and system events go to the system processor. Each domain has different processing logic, different downstream systems, and different SLAs. Routing an event to the wrong domain processor produces incorrect results or data corruption.

Without orchestration, you'd build a routing table with a switch statement or map lookup, manually dispatching events to different services, handling unknown domains with fallback logic, and logging every routing decision to debug misrouted events.

## The Solution

**You just write the event-receive, type-extraction, and domain-specific processor workers. Conductor handles domain-based SWITCH routing, per-processor retries, and full traceability of every routing decision.**

Each domain processor is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of receiving the event, extracting its domain, routing via a SWITCH task to the correct processor (user, order, system), retrying if the processor fails, and tracking every event's routing and outcome. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement domain-based routing: ReceiveEventWorker ingests the event, ExtractTypeWorker parses the domain and sub-type, then UserProcessorWorker, OrderProcessorWorker, or SystemProcessorWorker handles it based on the extracted domain.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractTypeWorker** | `eo_extract_type` | Splits the eventDomain string by "." to extract the domain (first part) and subType (remaining parts joined by ".").  |
| **OrderProcessorWorker** | `eo_order_processor` | Processes order-domain events. Extracts the orderId from eventData and returns a result indicating fulfillment  |
| **ReceiveEventWorker** | `eo_receive_event` | Receives an incoming event and passes through its domain and data, stamping a receivedAt timestamp. |
| **SystemProcessorWorker** | `eo_system_processor` | Default processor for events that do not match user or order domains. Passes through the domain and marks the event a |
| **UserProcessorWorker** | `eo_user_processor` | Processes user-domain events. Returns a result indicating the user event was handled: profile updated, notifica |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
eo_receive_event
    │
    ▼
eo_extract_type
    │
    ▼
SWITCH (route_ref)
    ├── user: eo_user_processor
    ├── order: eo_order_processor
    └── default: eo_system_processor
```

## Example Output

```
=== Event Routing Demo ===

Step 1: Registering task definitions...
  Registered: eo_receive_event, eo_extract_type, eo_user_processor, eo_order_processor, eo_system_processor

Step 2: Registering workflow 'event_routing_wf'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 89a1c576-0871-7c3e-0aa5-ebc78170f40c

  [eo_receive_event] Received event: evt-fixed-001 domain=user.profile_update
  [eo_extract_type] Extracting type from: unknown
  [eo_user_processor] Processing user event: evt-fixed-001 subType=profile_update


  Status: COMPLETED
  Output: {eventId=evt-fixed-001, domain=user, subType=profile_update, routed=true}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/event-routing-1.0.0.jar
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
java -jar target/event-routing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_routing_wf \
  --version 1 \
  --input '{"eventId": "evt-fixed-001", "eventDomain": "user.profile_update", "eventData": {"userId": "U-3301", "action": "profile_update", "changes": {"displayName": "Alice Johnson", "avatar": "new-avatar.png"}}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_routing_wf -s COMPLETED -c 5
```

## How to Extend

Point each domain processor at your real user service, order management API, and system-event handler, the receive-extract-route workflow stays exactly the same.

- **User processor**: handle user events (signup, profile update, login) by writing to your user service or CRM (Salesforce, HubSpot)
- **Order processor**: handle order events (created, updated, cancelled) by updating your OMS and notifying fulfillment
- **System processor**: handle system events (alerts, metrics, health checks) by forwarding to your monitoring platform (Datadog, PagerDuty)
- Add new domain processors by adding SWITCH cases and workers. Existing processors remain untouched

Adding a new domain processor means one new worker and a SWITCH case. Existing domain handlers stay untouched.

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
event-routing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventrouting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventRoutingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractTypeWorker.java
│       ├── OrderProcessorWorker.java
│       ├── ReceiveEventWorker.java
│       ├── SystemProcessorWorker.java
│       └── UserProcessorWorker.java
└── src/test/java/eventrouting/workers/
    ├── ExtractTypeWorkerTest.java        # 9 tests
    ├── OrderProcessorWorkerTest.java        # 9 tests
    ├── ReceiveEventWorkerTest.java        # 8 tests
    ├── SystemProcessorWorkerTest.java        # 8 tests
    └── UserProcessorWorkerTest.java        # 8 tests
```
