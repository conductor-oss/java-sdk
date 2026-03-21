# Travel Policy in Java with Conductor

Travel policy with SWITCH for compliant/exception. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to enforce travel policy on booking requests. Checking whether a booking (flight class, hotel rate, rental vehicle class) complies with company policy, routing compliant bookings for automatic approval, routing non-compliant bookings through an exception approval process, and then processing the booking once approved. The routing decision depends on the policy check result.

Without orchestration, you'd hardcode policy rules in the booking handler, mixing compliance checks with approval logic and booking processing. Updating the policy (e.g., allowing business class for flights over 6 hours) means editing the same code that handles booking creation. A SWITCH task cleanly separates the compliance check from the approval path, so you can update policy rules without touching the approval or booking logic.

## The Solution

**You just write the policy compliance check, auto-approval, exception routing, and booking processing logic. Conductor handles rule evaluation retries, exception routing, and policy compliance audit trails.**

CheckWorker evaluates the booking against travel policy rules. Maximum hotel rate by city tier, flight class by trip duration, rental vehicle class by trip purpose, and returns a compliance result with a reason. A SWITCH task routes based on the result: CompliantWorker auto-approves policy-compliant bookings, while ExceptionWorker routes non-compliant bookings to a manager or finance team for exception approval, recording who approved and why. After either approval path, ProcessWorker finalizes the booking with the approved parameters. Each worker is a standalone Java class. Conductor handles the routing, retries, and execution tracking.

### What You Write: Workers

Policy retrieval, rule evaluation, exception handling, and compliance reporting workers each enforce one layer of corporate travel governance.

| Worker | Task | What It Does |
|---|---|---|
| **CheckWorker** | `tpl_check` | Checks the input and computes compliance result, reason |
| **CompliantWorker** | `tpl_compliant` | Within policy. auto-approved |
| **ExceptionWorker** | `tpl_exception` | Processes a travel policy exception request. Approves the exception and records the VP-level approver |
| **ProcessWorker** | `tpl_process` | Processes the booking request against the company travel policy |

Workers implement travel operations. booking, approval, itinerary generation,  with realistic outputs. Replace with real GDS and travel API integrations and the workflow stays the same.

### The Workflow

```
tpl_check
    │
    ▼
SWITCH (tpl_switch_ref)
    ├── compliant: tpl_compliant
    ├── exception: tpl_exception
    │
    ▼
tpl_process

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
java -jar target/travel-policy-1.0.0.jar

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
java -jar target/travel-policy-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tpl_travel_policy \
  --version 1 \
  --input '{"employeeId": "TEST-001", "bookingType": "standard", "amount": 100, "policyTier": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tpl_travel_policy -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real compliance systems. your policy rules engine for compliance checks, your HR system for exception approvals, your booking platform for reservation processing, and the workflow runs identically in production.

- **CheckWorker** (`tpl_check`): evaluate the booking against your company's travel policy rules stored in your TMS (SAP Concur, Navan) or policy engine, checking rate caps, class restrictions, and advance booking requirements
- **CompliantWorker** (`tpl_compliant`): auto-approve the booking in your travel management system, deduct from the department's travel budget, and notify the traveler they can proceed
- **ExceptionWorker** (`tpl_exception`): route the exception request to the appropriate approver (manager, finance, VP) via email or Slack, record the justification, and log the exception for audit
- **ProcessWorker** (`tpl_process`): finalize the approved booking by calling the GDS or hotel/airline booking API with the approved parameters and corporate payment method

Update policy rules or exception criteria and the governance pipeline adapts with no structural changes.

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
travel-policy-travel-policy/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/travelpolicy/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TravelPolicyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckWorker.java
│       ├── CompliantWorker.java
│       ├── ExceptionWorker.java
│       └── ProcessWorker.java
└── src/test/java/travelpolicy/workers/
    ├── CheckWorkerTest.java        # 2 tests
    ├── CompliantWorkerTest.java        # 2 tests
    ├── ExceptionWorkerTest.java        # 2 tests
    └── ProcessWorkerTest.java        # 2 tests

```
