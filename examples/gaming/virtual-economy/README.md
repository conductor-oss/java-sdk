# Virtual Economy in Java Using Conductor

Processes virtual economy transactions: recording the transaction, validating balance and ownership, updating player balances, creating an audit trail for fraud detection, and generating a settlement report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process a virtual economy transaction in your game. a purchase, sale, trade, or currency conversion. The transaction must be recorded, validated for sufficient balance and item ownership, the sender and receiver balances updated atomically, an audit record created for fraud detection, and a transaction report generated. Processing transactions without validation enables item duplication and currency exploits; missing audit records makes fraud investigation impossible.

Without orchestration, you'd handle transactions in a single database transaction with validation, balance updates, and audit logging. manually ensuring atomicity across player accounts, handling concurrent transactions on the same account, and maintaining economy health metrics.

## The Solution

**You just write the transaction recording, balance validation, currency transfer, audit logging, and settlement reporting logic. Conductor handles transaction validation retries, balance reconciliation, and economy audit trails.**

Each economy concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (transaction, validate, update balance, audit, report), retrying if the database is temporarily unavailable, tracking every transaction with full audit trail, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Currency minting, transaction validation, balance updates, and audit logging workers each govern one aspect of the in-game economy.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `vec_audit` | Creates an audit log entry linking the transaction ID and resulting balance for fraud detection |
| **ReportWorker** | `vec_report` | Generates a settlement report with player ID, transaction ID, and final status |
| **TransactionWorker** | `vec_transaction` | Records the transaction request with type (earn, purchase, trade) and amount, and assigns a transaction ID |
| **UpdateBalanceWorker** | `vec_update_balance` | Updates the player's balance by adding the transaction amount and returns old and new balances |
| **ValidateWorker** | `vec_validate` | Validates the transaction by checking rate limits and value limits for the currency |

Workers implement game backend operations. matchmaking, score processing, reward distribution,  with realistic outputs. Replace with real game server and database integrations and the workflow stays the same.

### The Workflow

```
vec_transaction
    │
    ▼
vec_validate
    │
    ▼
vec_update_balance
    │
    ▼
vec_audit
    │
    ▼
vec_report

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
java -jar target/virtual-economy-1.0.0.jar

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
java -jar target/virtual-economy-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow virtual_economy_750 \
  --version 1 \
  --input '{"playerId": "TEST-001", "type": "standard", "amount": 100, "currency": "USD"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w virtual_economy_750 -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real economy services. your transaction ledger for recording, your balance service for atomic updates, your fraud detection system for audit trails, and the workflow runs identically in production.

- **Transaction handler**: record the transaction request with type (purchase, sale, trade, gift) and involved currencies/items
- **Validator**: check sufficient balance, item ownership, trade restrictions, and anti-fraud rules (velocity checks, value limits)
- **Balance updater**: apply atomic balance changes across player accounts using your game database with optimistic concurrency control
- **Auditor**: write transaction records to your fraud detection system; flag suspicious patterns (gold farming, real-money trading, exploits)
- **Reporter**: generate economy health reports (money supply, inflation rate, item value trends) for your live-ops team

Modify currency rates or transaction limits and the economy pipeline processes them without structural edits.

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
virtual-economy/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/virtualeconomy/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VirtualEconomyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditWorker.java
│       ├── ReportWorker.java
│       ├── TransactionWorker.java
│       ├── UpdateBalanceWorker.java
│       └── ValidateWorker.java
└── src/test/java/virtualeconomy/workers/
    ├── ReportWorkerTest.java
    └── TransactionWorkerTest.java

```
