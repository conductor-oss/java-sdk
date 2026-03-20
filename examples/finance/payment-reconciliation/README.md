# Payment Reconciliation in Java with Conductor

Reconcile payments: match transactions, identify discrepancies, resolve mismatches, and generate report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to reconcile payments between your internal records and bank/processor statements. The workflow matches transactions from both sides, identifies discrepancies (missing transactions, amount mismatches, duplicate entries), resolves mismatches through investigation or adjustment, and generates a reconciliation report. Unreconciled payments mean your books are inaccurate; unresolved discrepancies accumulate and become harder to fix over time.

Without orchestration, you'd run a batch reconciliation script that pulls transactions from your database and bank feeds, runs matching algorithms, generates exception reports, and manually investigates discrepancies .  handling format differences between payment processors, retrying failed bank API calls, and maintaining reconciliation state across daily/weekly cycles.

## The Solution

**You just write the reconciliation workers. Transaction matching, discrepancy identification, and mismatch resolution. Conductor handles step ordering, automatic retries when the bank feed API is unavailable, and complete reconciliation cycle tracking.**

Each reconciliation concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (match, identify discrepancies, resolve, report), retrying if the bank feed API is unavailable, tracking every reconciliation cycle with full detail, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Three workers handle the reconciliation process: MatchTransactionsWorker compares internal records against bank statements, IdentifyDiscrepanciesWorker flags mismatches and missing entries, and ResolveMismatchesWorker investigates and adjusts discrepancies.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyDiscrepanciesWorker** | `prc_identify_discrepancies` | Identifies discrepancies from unmatched transactions. |
| **MatchTransactionsWorker** | `prc_match_transactions` | Matches transactions against records for reconciliation. |
| **ResolveMismatchesWorker** | `prc_resolve_mismatches` | Resolves identified mismatches/discrepancies. |

Workers simulate financial operations .  risk assessment, compliance checks, settlement ,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
prc_match_transactions
    │
    ▼
prc_identify_discrepancies
    │
    ▼
prc_resolve_mismatches
    │
    ▼
prc_generate_report
```

## Example Output

```
=== Payment Reconciliation Demo ===

Step 1: Registering task definitions...
  Registered: prc_match_transactions, prc_identify_discrepancies, prc_resolve_mismatches, prc_generate_report

Step 2: Registering workflow 'payment_reconciliation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [report] Generating reconciliation report for batch
  [discrepancy] Found
  [match] Matching transactions for batch
  [resolve] Resolving

  Status: COMPLETED
  Output: {reportId=..., generatedAt=..., summary=..., discrepancies=...}

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
java -jar target/payment-reconciliation-1.0.0.jar
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
java -jar target/payment-reconciliation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow payment_reconciliation_workflow \
  --version 1 \
  --input '{"batchId": "BATCH-2026-0314", "BATCH-2026-0314": "accountId", "accountId": "ACCT-7890", "ACCT-7890": "periodStart", "periodStart": "2026-03-01", "2026-03-01": "periodEnd", "periodEnd": "2026-03-14", "2026-03-14": "sample-2026-03-14"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w payment_reconciliation_workflow -s COMPLETED -c 5
```

## How to Extend

Connect MatchTransactionsWorker to your bank feed and internal ledger, IdentifyDiscrepanciesWorker to your exception rules engine, and ResolveMismatchesWorker to your investigation and adjustment workflow. The workflow definition stays exactly the same.

- **Transaction matcher**: pull transactions from your GL (SAP, Oracle, QuickBooks) and bank feeds (Plaid, Yodlee, direct bank APIs) and run fuzzy matching algorithms
- **Discrepancy identifier**: classify mismatches (amount difference, missing counterpart, timing difference, duplicate) with configurable tolerance thresholds
- **Resolver**: auto-resolve timing differences and known patterns; route true discrepancies to finance team for investigation via WAIT tasks
- **Report generator**: produce reconciliation reports for month-end close with matched/unmatched/adjusted totals

Connect each worker to your real bank feed APIs and general ledger while returning the same fields, and the reconciliation workflow operates without changes.

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
payment-reconciliation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/paymentreconciliation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PaymentReconciliationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── IdentifyDiscrepanciesWorker.java
│       ├── MatchTransactionsWorker.java
│       └── ResolveMismatchesWorker.java
```
