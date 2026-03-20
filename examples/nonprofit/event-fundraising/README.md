# Event Fundraising in Java with Conductor

A Java Conductor workflow example demonstrating Event Fundraising. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Your nonprofit is hosting a gala dinner to raise funds. The events team needs to plan the event by booking a venue and setting capacity, promote it across email, social media, and partner channels to drive registrations, execute the event and track attendance and satisfaction, collect ticket revenue and additional donations, and reconcile the finances to produce a net-raised figure. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the event setup, registration processing, donation collection, and post-event reporting logic. Conductor handles registration retries, donation processing, and event audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Event planning, registration, donation collection, and impact reporting workers each own one aspect of fundraising event execution.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `efr_collect` | Calculates ticket revenue from attendees and ticket price, adds additional donations, and returns total raised |
| **ExecuteWorker** | `efr_execute` | Runs the event day, recording actual attendee count, total expenses, and attendee satisfaction score |
| **PlanWorker** | `efr_plan` | Plans the event by assigning a venue and capacity, returning an event ID |
| **PromoteWorker** | `efr_promote` | Promotes the event across email, social media, and partner channels, returning registration count |
| **ReconcileWorker** | `efr_reconcile` | Reconciles revenue against expenses to compute net funds raised and marks the event as reconciled |

Workers simulate nonprofit operations .  donor processing, campaign management, reporting ,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
efr_plan
    │
    ▼
efr_promote
    │
    ▼
efr_execute
    │
    ▼
efr_collect
    │
    ▼
efr_reconcile
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
java -jar target/event-fundraising-1.0.0.jar
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
java -jar target/event-fundraising-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_fundraising_759 \
  --version 1 \
  --input '{"eventName": "test", "eventDate": "2026-01-01T00:00:00Z", "ticketPrice": 100}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_fundraising_759 -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event platform. Eventbrite for registration, Stripe for ticket payments, your CRM for donor tracking and post-event follow-up, and the workflow runs identically in production.

- **PlanWorker** (`efr_plan`): create the event in Eventbrite or your event management platform via their API, reserving the venue and returning the event ID
- **PromoteWorker** (`efr_promote`): trigger email campaigns via SendGrid or Mailchimp, post to social media via Buffer or Hootsuite API, and track registrations from Eventbrite
- **ExecuteWorker** (`efr_execute`): pull check-in data from Eventbrite's attendee API and log expenses from your accounting system (QuickBooks, Sage Intacct)
- **CollectWorker** (`efr_collect`): process payments via Stripe or your payment processor and record donations in Salesforce NPSP or DonorPerfect
- **ReconcileWorker** (`efr_reconcile`): query your payment processor and accounting system to reconcile revenue against expenses and update the event record in your CRM

Switch registration platforms or payment processors and the fundraising pipeline continues unchanged.

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
event-fundraising/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventfundraising/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventFundraisingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectWorker.java
│       ├── ExecuteWorker.java
│       ├── PlanWorker.java
│       ├── PromoteWorker.java
│       └── ReconcileWorker.java
└── src/test/java/eventfundraising/workers/
    ├── PlanWorkerTest.java        # 1 tests
    └── ReconcileWorkerTest.java        # 1 tests
```
