# Trade Execution in Java with Conductor

Trade execution workflow that validates orders, checks compliance, routes to optimal exchange, executes, and confirms. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to execute a securities trade from order validation to confirmation. An order is validated for completeness and market hours, checked against compliance rules (position limits, restricted lists, wash sale prevention), routed to the optimal exchange or dark pool, executed at the best available price, and confirmed with fill details. Executing without compliance checks violates regulations; routing to the wrong venue results in worse execution prices.

Without orchestration, you'd build a single trade pipeline that validates orders, queries compliance databases, implements smart order routing, sends FIX messages to exchanges, and processes fill reports. manually handling partial fills, order cancellations, and the microsecond-level timing requirements of modern markets.

## The Solution

**You just write the trade workers. Order validation, compliance checking, smart order routing, exchange execution, and fill confirmation. Conductor handles pipeline sequencing, automatic retries when an exchange connection drops, and full order lifecycle tracking for best execution reporting.**

Each trade execution concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (validate, check compliance, route, execute, confirm), retrying if an exchange connection drops, tracking every order's full lifecycle for best execution reporting, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers form the trade pipeline: ValidateOrderWorker checks order completeness and buying power, CheckComplianceWorker screens against regulatory rules, RouteWorker selects the optimal exchange, ExecuteWorker places the order, and ConfirmWorker delivers fill details.

| Worker | Task | What It Does |
|---|---|---|
| **CheckComplianceWorker** | `trd_check_compliance` | Checks regulatory compliance for the trade. |
| **ConfirmWorker** | `trd_confirm` | Sends trade confirmation to the client. |
| **ExecuteWorker** | `trd_execute` | Executes the trade on the routed exchange. |
| **RouteWorker** | `trd_route` | Routes the trade to the optimal exchange for best execution. |
| **ValidateOrderWorker** | `trd_validate_order` | Validates a trade order for required fields and buying power. |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
trd_validate_order
    │
    ▼
trd_check_compliance
    │
    ▼
trd_route
    │
    ▼
trd_execute
    │
    ▼
trd_confirm

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
java -jar target/trade-execution-1.0.0.jar

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
java -jar target/trade-execution-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow trade_execution_workflow \
  --version 1 \
  --input '{"orderId": "TEST-001", "accountId": "TEST-001", "symbol": "sample-symbol", "side": "TEST-001", "quantity": "sample-quantity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w trade_execution_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ValidateOrderWorker to your order management system, CheckComplianceWorker to your restricted list and position limit database, and RouteWorker to your smart order router for best execution. The workflow definition stays exactly the same.

- **Order validator**: validate order parameters against market hours, lot sizes, and account restrictions using your OMS (Charles River, Bloomberg AIM)
- **Compliance checker**: screen against restricted lists, position limits, and trading policies using your compliance system (NICE Actimize, Nasdaq Surveillance)
- **Order router**: implement smart order routing across exchanges and dark pools for best execution using FIX protocol
- **Trade executor**: send orders via FIX 4.2/4.4 to exchanges (NYSE, NASDAQ) or alternative trading systems
- **Confirmation handler**: process fill reports, update positions in real-time, and generate trade confirmations per SEC Rule 10b-10

Connect each worker to your real order management system, compliance engine, and exchange FIX gateway while returning the same fields, and the execution pipeline requires no workflow changes.

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
trade-execution/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tradeexecution/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TradeExecutionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckComplianceWorker.java
│       ├── ConfirmWorker.java
│       ├── ExecuteWorker.java
│       ├── RouteWorker.java
│       └── ValidateOrderWorker.java
└── src/test/java/tradeexecution/workers/
    ├── ConfirmWorkerTest.java        # 2 tests
    ├── ExecuteWorkerTest.java        # 4 tests
    └── ValidateOrderWorkerTest.java        # 3 tests

```
