# Insurance Commission Processing in Java with Conductor :  Calculate, Validate, Deduct Advances, Pay, Report

A Java Conductor workflow example demonstrating commission-insurance Commission Insurance. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Agent Commissions Have Advances, Tiers, and Clawbacks

An agent writes a $2,000 annual premium policy with a 15% new business commission rate ($300). But the agent received a $200 advance against future commissions last month. And if the policy cancels within 6 months, the commission must be clawed back. The commission calculation must apply the correct rate (new business vs: renewal, product-specific tiers), deduct any outstanding advances, process the net payment, and track the potential clawback liability.

Commission processing involves multiple rates (new business 15%, renewal 10%, bonus tiers above production thresholds), advance tracking (deducting advances from earned commissions), clawback management (reversing commissions on cancelled policies within the clawback period), and regulatory reporting (1099 forms, state premium tax credits).

## The Solution

**You just write the commission calculation, advance deduction, validation, payout processing, and reporting logic. Conductor handles split calculation retries, payment sequencing, and commission audit trails.**

`CalculateWorker` computes the commission amount using the applicable rate. new business or renewal, product-specific tiers, and any bonus qualifications. `ValidateWorker` verifies the calculation against the agent's commission schedule and carrier guidelines. `DeductAdvancesWorker` subtracts any outstanding advance balances from the earned commission. `PayWorker` issues the net commission payment to the agent via their configured payment method. `ReportWorker` generates commission statements and updates the agent's production records for year-end 1099 reporting. Conductor tracks every commission calculation for financial audit.

### What You Write: Workers

Policy matching, rate calculation, split allocation, and payment processing workers each handle one layer of insurance commission computation.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateWorker** | `cin_calculate` | Calculates the commission. applies the applicable rate (15% new business, 10% renewal) to the premium amount, with tier adjustments for production thresholds and product-specific rates |
| **ValidateWorker** | `cin_validate` | Validates the commission. verifies the calculated amount against the agent's commission schedule and carrier guidelines, checking for rate overrides and special agreements |
| **DeductAdvancesWorker** | `cin_deduct_advances` | Deducts outstanding advances. subtracts any advance balance from the gross commission to determine the net commission payable, tracking the amortization schedule |
| **PayWorker** | `cin_pay` | Issues the commission payment. processes the net commission via the agent's configured payment method and generates a paymentId for reconciliation |
| **ReportWorker** | `cin_report` | Files the commission report. records the transaction for the agent and policy, updates year-to-date production totals, and generates the commission statement |

Workers implement insurance operations. claim intake, assessment, settlement,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
cin_calculate
    │
    ▼
cin_validate
    │
    ▼
cin_deduct_advances
    │
    ▼
cin_pay
    │
    ▼
cin_report

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
java -jar target/commission-insurance-1.0.0.jar

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
java -jar target/commission-insurance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cin_commission_insurance \
  --version 1 \
  --input '{"agentId": "TEST-001", "policyId": "TEST-001", "premiumAmount": 100}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cin_commission_insurance -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real commission systems. your policy admin for production data, your commission engine for tier-based calculations, your payroll system for agent payouts, and the workflow runs identically in production.

- **CalculateWorker** (`cin_calculate`): implement tiered commission schedules with new business/renewal splits, contingent commission calculations based on loss ratios, and override commissions for agency hierarchies
- **PayWorker** (`cin_pay`): process payments via ACH (Plaid or Dwolla), generate 1099-MISC forms for year-end tax reporting, and handle multi-state commission tax withholding
- **DeductAdvancesWorker** (`cin_deduct_advances`): implement advance tracking with amortization schedules, clawback calculations for early policy cancellations, and chargeback processing

Modify rate schedules or split formulas and the commission pipeline handles them with no structural changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
commission-insurance-commission-insurance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/commissioninsurance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CommissionInsuranceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateWorker.java
│       ├── DeductAdvancesWorker.java
│       ├── PayWorker.java
│       ├── ReportWorker.java
│       └── ValidateWorker.java
└── src/test/java/commissioninsurance/workers/
    ├── CalculateWorkerTest.java        # 1 tests
    └── PayWorkerTest.java        # 1 tests

```
