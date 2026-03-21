# Contract Lifecycle Management in Java with Conductor: Drafting, Legal Review, Approval, Execution, and Renewal

Your $250K logistics contract auto-renewed last month at a 20% rate increase. The opt-out window closed 60 days before expiry, but nobody tracked it. the renewal date was buried in a PDF on a shared drive, and the calendar reminder was set for the week of expiry, not the week of the opt-out deadline. That's $50K/year you'll pay for the next 12 months because a date wasn't surfaced. Meanwhile, three other contracts are sitting in legal review right now: one has been there for six weeks because legal didn't know it was waiting, and the other two were approved but never executed because the DocuSign request went to a generic inbox. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full contract lifecycle, drafting, legal review, approval, execution, and renewal scheduling, so deadlines are enforced and contracts don't silently lapse or auto-renew.

## The Problem

You need to manage supplier contracts from creation to renewal. The procurement team drafts a service agreement with terms and pricing, legal reviews for risk and compliance with company policies, finance approves based on budget authority, the contract is executed with digital signatures, and renewal needs to be triggered before the 12-month term expires. If the legal review identifies issues, the contract must go back to drafting; but today that happens over email with no version control.

Without orchestration, contracts sit in shared drives with no visibility into their status. Legal review takes weeks because nobody knows a contract is waiting for them. Approved contracts are executed but renewal dates go untracked, causing service interruptions when contracts lapse. When the CFO asks how many contracts above $100K are pending approval, nobody can answer without manually checking email threads.

## The Solution

**You just write the contract lifecycle workers. Drafting, legal review, approval routing, execution, and renewal scheduling. Conductor handles sequencing, retry logic, version tracking, and complete audit trails for contract analytics.**

Each stage of the contract lifecycle is a simple, independent worker, a plain Java class that does one thing. Conductor sequences them so drafts are completed before legal review begins, review feedback gates approval, approval gates execution, and renewal is automatically scheduled based on the contract term. If the approval worker fails to reach the signatory, Conductor retries without re-triggering legal review. Every draft version, review comment, approval decision, and execution timestamp is recorded for audit and contract analytics.

### What You Write: Workers

Five workers cover the contract lifecycle: DraftWorker creates agreements, ReviewWorker handles legal review, ApproveWorker obtains signatory approval, ExecuteWorker captures digital signatures, and RenewWorker schedules renewals.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `clf_approve` | Obtains budget and authority approval based on contract value and review notes. |
| **DraftWorker** | `clf_draft` | Creates a contract draft with vendor, type, value, and term details. |
| **ExecuteWorker** | `clf_execute` | Executes the approved contract with digital signatures. |
| **RenewWorker** | `clf_renew` | Schedules contract renewal based on the term length and expiry date. |
| **ReviewWorker** | `clf_review` | Routes the draft through legal review for risk and compliance assessment. |

Workers simulate supply chain operations: inventory checks, shipment tracking, supplier coordination, with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
clf_draft
    │
    ▼
clf_review
    │
    ▼
clf_approve
    │
    ▼
clf_execute
    │
    ▼
clf_renew

```

## Example Output

```
=== Example 662: Contract Lifecycle ===

Step 1: Registering task definitions...
  Registered: clf_draft, clf_review, clf_approve, clf_execute, clf_renew

Step 2: Registering workflow 'clf_contract_lifecycle'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 719f127d-1921-3db0-31f1-84cd6bccf4f2

  [draft] Contract for GlobalLogistics Corp: service-agreement, $250000
  [review] Legal review of CTR-662-001.; no issues
  [approve] CTR-662-001 approved
  [execute] CTR-662-001 signed and executed
  [renew] Renewal reminder set for 2025-04-01 (...-month term)


  Status: COMPLETED
  Output: {contractId=CTR-662-001, approved=true, executed=true, renewalDate=2025-04-01}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/contract-lifecycle-1.0.0.jar

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
java -jar target/contract-lifecycle-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow clf_contract_lifecycle \
  --version 1 \
  --input '{"vendor": "GlobalLogistics Corp", "contractType": "service-agreement", "value": 250000, "termMonths": 12}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w clf_contract_lifecycle -s COMPLETED -c 5

```

## How to Extend

Connect DraftWorker to your CLM platform (Icertis, Agiloft), ReviewWorker to your legal review queue, and ExecuteWorker to DocuSign for e-signatures. The workflow definition stays exactly the same.

- **DraftWorker** (`clf_draft`): generate contract documents from templates in your CLM platform (Ironclad, Icertis, or DocuSign CLM), pre-populating vendor details, pricing, and standard clauses
- **ReviewWorker** (`clf_review`): route the draft to the legal team via Slack or your CLM's review queue, collect redline comments, and track review cycles
- **ApproveWorker** (`clf_approve`): implement multi-tier approval based on contract value (e.g., manager for <$50K, VP for <$250K, C-suite for $250K+) via your approval engine
- **ExecuteWorker** (`clf_execute`): send for digital signature via DocuSign or Adobe Sign, store the fully executed PDF in your document management system, and update the vendor master record
- **RenewWorker** (`clf_renew`): calculate renewal date from contract term, create a renewal task in your procurement system, and send advance notice to the contract owner

Connect any worker to your CLM platform while preserving its output fields, and the workflow definition stays unchanged.

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
contract-lifecycle/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contractlifecycle/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContractLifecycleExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── DraftWorker.java
│       ├── ExecuteWorker.java
│       ├── RenewWorker.java
│       └── ReviewWorker.java
└── src/test/java/contractlifecycle/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── DraftWorkerTest.java        # 2 tests
    ├── ExecuteWorkerTest.java        # 2 tests
    ├── RenewWorkerTest.java        # 2 tests
    └── ReviewWorkerTest.java        # 2 tests

```
