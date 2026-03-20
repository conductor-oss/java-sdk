# Real Estate Escrow Management in Java with Conductor :  Open, Deposit, Verify, Release, and Close

A Java Conductor workflow example for managing the escrow lifecycle in a real estate transaction .  opening the escrow account, accepting the buyer's deposit, verifying that all closing conditions are met, releasing funds to the seller, and formally closing the escrow. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to manage escrow for property transactions. When a buyer and seller agree on a sale, earnest money must be deposited into a neutral escrow account. Before funds can be released, contingencies must be verified .  title is clear, inspection passed, financing is approved. Only after verification should funds be released to the seller, and then the escrow must be formally closed with all parties notified. If any step executes out of order ,  funds released before verification, escrow closed before release ,  you face legal liability and financial loss.

Without orchestration, escrow management is tracked manually with phone calls, emails, and checklists. The title company calls to confirm the deposit, the agent emails to confirm verification, and a paralegal manually triggers the release. A missed step means funds are stuck or released prematurely, and reconstructing what happened requires digging through email threads.

## The Solution

**You just write the escrow opening, deposit handling, contingency verification, fund release, and closing logic. Conductor handles deposit retries, condition tracking, and escrow audit trails.**

Each escrow step is a simple, independent worker .  one opens the account, one accepts the deposit, one verifies contingencies, one releases funds, one closes the escrow. Conductor ensures strict sequential execution so funds are never released before verification, retries if the banking API is temporarily down, and maintains a tamper-proof record of every step for regulatory compliance. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Deposit collection, document verification, condition tracking, and fund disbursement workers each manage one phase of the escrow process.

| Worker | Task | What It Does |
|---|---|---|
| **OpenEscrowWorker** | `esc_open` | Creates a new escrow account for the buyer/seller pair and returns the escrow ID |
| **DepositWorker** | `esc_deposit` | Records the earnest money deposit into the escrow account and confirms receipt |
| **VerifyWorker** | `esc_verify` | Checks that all closing contingencies are satisfied (title clear, inspection, financing) |
| **ReleaseWorker** | `esc_release` | Releases escrowed funds to the seller after verification is complete |
| **CloseEscrowWorker** | `esc_close` | Formally closes the escrow account and generates the closing statement |

Workers simulate property transaction steps .  listing, inspection, escrow, closing ,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
esc_open
    │
    ▼
esc_deposit
    │
    ▼
esc_verify
    │
    ▼
esc_release
    │
    ▼
esc_close
```

## Example Output

```
=== Example 689: Escrow Management ===

Step 1: Registering task definitions...
  Registered: esc_open, esc_deposit, esc_verify, esc_release, esc_close

Step 2: Registering workflow 'esc_escrow_management'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [esc_close] Executing
  [esc_deposit] Executing
  [esc_open] Executing
  [esc_release] Executing
  [esc_verify] Executing

  Status: COMPLETED
  Output: {deposited=..., escrowId=..., released=..., conditionsMet=...}

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
java -jar target/escrow-management-1.0.0.jar
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
java -jar target/escrow-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow esc_escrow_management \
  --version 1 \
  --input '{"buyerId": "BUY-100", "BUY-100": "sellerId", "sellerId": "SEL-200", "SEL-200": "amount", "amount": 475000}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w esc_escrow_management -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real escrow systems .  your trust accounting platform for account management, your title company API for contingency verification, your banking API for fund transfers, and the workflow runs identically in production.

- **OpenEscrowWorker** (`esc_open`): integrate with your escrow company's API or banking platform (Plaid, Stripe Treasury) to open a real custodial account
- **DepositWorker** (`esc_deposit`): initiate an ACH pull from the buyer's bank account and verify the deposit cleared
- **VerifyWorker** (`esc_verify`): query title company APIs for lien checks, pull inspection reports from your document management system, and confirm lender approval
- **ReleaseWorker** (`esc_release`): trigger a wire transfer to the seller via your banking API with dual-authorization controls
- **CloseEscrowWorker** (`esc_close`): generate the HUD-1 settlement statement, file with the county recorder, and notify all parties via email

Switch document management systems or fund custody providers and the escrow pipeline continues unchanged.

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
escrow-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/escrowmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EscrowManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseEscrowWorker.java
│       ├── DepositWorker.java
│       ├── OpenEscrowWorker.java
│       ├── ReleaseWorker.java
│       └── VerifyWorker.java
└── src/test/java/escrowmanagement/workers/
    ├── CloseEscrowWorkerTest.java        # 2 tests
    ├── DepositWorkerTest.java        # 2 tests
    ├── OpenEscrowWorkerTest.java        # 2 tests
    ├── ReleaseWorkerTest.java        # 2 tests
    └── VerifyWorkerTest.java        # 2 tests
```
