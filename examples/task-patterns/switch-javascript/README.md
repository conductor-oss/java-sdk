# Switch Javascript in Java with Conductor

SWITCH with JavaScript evaluator for complex routing based on amount, customerType, and region. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to route order processing based on multiple criteria simultaneously .  not just a single field. VIP customers with high-value orders (amount > $1,000) get white-glove concierge service. VIP customers with standard orders get priority processing. Non-VIP orders over $5,000 require manual review for fraud screening. EU region orders need compliance processing (VAT, GDPR). Everything else goes through standard processing. A simple value-param SWITCH can only match on one field, but this routing logic depends on amount AND customerType AND region evaluated together.

Without orchestration, you'd write nested if/else conditions: `if (customerType == "vip" && amount > 1000)` then concierge, `else if (customerType == "vip")` then priority, etc. Adding a new routing rule means modifying deeply nested conditionals and retesting every path. There is no record of which evaluation path was taken for a given order, making it impossible to audit why an order was or was not flagged for review.

## The Solution

**You just write the per-route processing and finalization workers. Conductor handles the JavaScript-based multi-criteria evaluation and branch routing.**

This example demonstrates Conductor's SWITCH task with a JavaScript evaluator .  multi-criteria routing logic evaluated server-side. The JavaScript expression takes amount, customerType, and region as inputs and returns a case label: `vip_high` (VIP + amount > $1,000), `vip_standard` (VIP, any amount), `needs_review` (amount > $5,000, non-VIP), `eu_processing` (EU region), or `standard` (everything else). Each case routes to a dedicated worker. VipConciergeWorker handles white-glove VIP orders, ManualReviewWorker flags high-value orders for fraud screening, EuHandlerWorker handles EU compliance, and StandardWorker processes normal orders. After the SWITCH resolves, FinalizeWorker runs regardless of branch. Conductor records the JavaScript evaluation result, so you can see exactly which routing decision was made and why.

### What You Write: Workers

Five workers handle the multi-criteria routing outcomes: VipConciergeWorker for high-value VIP orders, VipPriorityWorker for standard VIP orders, ManualReviewWorker for fraud screening, EuHandlerWorker for EU compliance, StandardWorker for normal processing, and FinalizeWorker for the common completion step.

| Worker | Task | What It Does |
|---|---|---|
| **EuHandlerWorker** | `swjs_eu_handler` | Handles orders from the EU region for compliance processing. |
| **FinalizeWorker** | `swjs_finalize` | Common finalization step that runs after the SWITCH regardless of which branch was taken. |
| **ManualReviewWorker** | `swjs_manual_review` | Handles high-value orders that require manual review (amount > 5000, non-VIP). |
| **StandardWorker** | `swjs_standard` | Default handler for orders that don't match any special criteria. |
| **VipConciergeWorker** | `swjs_vip_concierge` | Handles VIP customers with high-value orders (amount > 1000). |
| **VipStandardWorker** | `swjs_vip_standard` | Handles VIP customers with standard-value orders. |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_order_ref)
    ├── vip_high: swjs_vip_concierge
    ├── vip_standard: swjs_vip_standard
    ├── needs_review: swjs_manual_review
    ├── eu_processing: swjs_eu_handler
    └── default: swjs_standard
    │
    ▼
swjs_finalize

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
java -jar target/switch-javascript-1.0.0.jar

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
java -jar target/switch-javascript-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow switch_js_demo \
  --version 1 \
  --input '{"amount": 100, "customerType": "standard", "region": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w switch_js_demo -s COMPLETED -c 5

```

## How to Extend

Replace the VIP concierge, manual review, and EU compliance handlers with your real processing logic, and the JavaScript-evaluated multi-criteria routing workflow runs unchanged.

- **VipConciergeWorker** (`swjs_vip_concierge`): assign a dedicated account manager, apply VIP pricing and expedited shipping, and send a personalized confirmation via the CRM
- **ManualReviewWorker** (`swjs_manual_review`): create a fraud review case in your fraud detection system (Sift, Riskified), hold the order until an analyst approves it, and notify the compliance team
- **EuHandlerWorker** (`swjs_eu_handler`): apply EU-specific processing: calculate VAT, ensure GDPR-compliant data handling, route through EU-based payment processors, and generate EU-formatted invoices
- **FinalizeWorker** (`swjs_finalize`): update the order status in your OMS, trigger fulfillment, and send order confirmation to the customer

Adding a new routing rule in the JavaScript evaluator or connecting branch workers to real services does not affect the multi-criteria SWITCH structure, since routing logic and processing logic are fully separated.

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
switch-javascript/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/switchjavascript/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SwitchJavascriptExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EuHandlerWorker.java
│       ├── FinalizeWorker.java
│       ├── ManualReviewWorker.java
│       ├── StandardWorker.java
│       ├── VipConciergeWorker.java
│       └── VipStandardWorker.java
└── src/test/java/switchjavascript/workers/
    ├── WorkflowJsonTest.java        # 20 tests
    ├── EuHandlerWorkerTest.java        # 4 tests
    ├── FinalizeWorkerTest.java        # 4 tests
    ├── ManualReviewWorkerTest.java        # 4 tests
    ├── StandardWorkerTest.java        # 4 tests
    ├── VipConciergeWorkerTest.java        # 4 tests
    └── VipStandardWorkerTest.java        # 4 tests

```
