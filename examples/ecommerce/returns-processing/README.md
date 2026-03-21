# Returns Processing in Java Using Conductor :  Receive, Inspect, Route Decision via SWITCH

A Java Conductor workflow example for e-commerce returns. receiving returned items, inspecting their condition, and routing to refund, exchange, or rejection based on inspection results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Return Outcomes Depend on Product Condition and Policy

A customer returns a pair of shoes they bought 10 days ago. The outcome depends on several factors: Is it within the 30-day return window? What's the product condition. unworn with tags (full refund eligible), worn but undamaged (store credit only), or damaged (rejection)? Is the return reason covered by policy (wrong size vs, changed mind vs, defective)?

The inspection step determines the condition, and the routing step maps condition + reason + policy to the right resolution. A defective item within warranty gets a full refund regardless of condition. A changed-mind return with a worn product gets store credit at best. Damaged items outside warranty get rejected. Each resolution path has different downstream steps (refund to original payment method, generate store credit code, or send rejection notification).

## The Solution

**You just write the return intake, inspection, and resolution (refund/exchange/reject) logic. Conductor handles refund retries, inspection routing, and return lifecycle audit trails.**

`ReceiveWorker` logs the return request with order ID, return reason, items, and customer ID, and generates a return merchandise authorization (RMA) number. `InspectWorker` evaluates the product condition. checking for damage, wear, missing components, and original packaging, and produces a condition grade. Conductor's `SWITCH` routes based on the inspection result and return policy: full refund (unworn, within window), exchange (wrong size/color), store credit (worn but acceptable), or rejection (damaged, outside window). Each resolution path processes the appropriate outcome. Conductor records the inspection results and routing decision for return analytics.

### What You Write: Workers

Return authorization, inspection, refund processing, and restocking workers handle each phase of a return without depending on the others' internals.

| Worker | Task | What It Does |
|---|---|---|
| **ReceiveReturnWorker** | `ret_receive` | Logs receipt of returned items at the warehouse, assigns a return ID |
| **InspectReturnWorker** | `ret_inspect` | Evaluates item condition and return reason, decides refund/exchange/reject |
| **RefundWorker** | `ret_refund` | Issues a refund to the customer's original payment method |
| **ExchangeWorker** | `ret_exchange` | Creates a replacement order and ships the exchange item |
| **RejectWorker** | `ret_reject` | Rejects the return and notifies the customer with the reason |

Workers implement e-commerce operations. payment processing, inventory checks, shipping,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
ret_receive
    │
    ▼
ret_inspect
    │
    ▼
SWITCH (switch_ref)
    ├── refund: ret_refund
    ├── exchange: ret_exchange
    ├── reject: ret_reject

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
java -jar target/returns-processing-1.0.0.jar

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
java -jar target/returns-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow returns_processing \
  --version 1 \
  --input '{"orderId": "TEST-001", "returnReason": "sample-returnReason", "items": [{"id": "ITEM-001", "quantity": 2}], "customerId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w returns_processing -s COMPLETED -c 5

```

## How to Extend

Replace each worker with your real returns stack. Google Vision for condition assessment, Stripe for refund processing, your OMS for exchange orders, and the workflow runs identically in production.

- **InspectWorker** (`ret_inspect`): integrate with computer vision (Google Vision API, custom CNN) for automated condition assessment from return photos, or use barcode scanning to verify the correct item was returned
- **Resolution workers**: process refunds via Stripe Refund API, generate store credit codes in your loyalty system, or create exchange orders in your OMS with automatic inventory restocking
- **ReceiveWorker** (`ret_receive`): integrate with carrier return label generation (EasyPost, Happy Returns), QR code scanning for drop-off verification, and automated RMA tracking

Update your refund policies or restocking rules and the returns pipeline handles them without structural changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
returns-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/returnsprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReturnsProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExchangeWorker.java
│       ├── InspectReturnWorker.java
│       ├── ReceiveReturnWorker.java
│       ├── RefundWorker.java
│       └── RejectWorker.java
└── src/test/java/returnsprocessing/workers/
    ├── ExchangeWorkerTest.java        # 2 tests
    ├── InspectReturnWorkerTest.java        # 3 tests
    ├── ReceiveReturnWorkerTest.java        # 3 tests
    ├── RefundWorkerTest.java        # 2 tests
    └── RejectWorkerTest.java        # 2 tests

```
