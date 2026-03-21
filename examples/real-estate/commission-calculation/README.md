# Real Estate Commission Calculation in Java with Conductor :  Base Rate, Tiered Adjustments, Deductions, and Payout

A Java Conductor workflow example for calculating real estate agent commissions. computing the base commission from sale price, applying tiered adjustments based on agent performance, subtracting brokerage fees and deductions, and finalizing the payout. Uses [Conductor](https://github.

## The Problem

You need to calculate agent commissions when a property sale closes. The calculation is never as simple as "sale price times percentage." You start with a base commission rate, then apply tiered adjustments. senior agents earn a higher split, top performers get bonus percentages, team leads take an override. Then you subtract deductions: brokerage desk fees, marketing costs, referral fees paid to other agents. Finally, the net commission is finalized and a payment is issued. Each step depends on the previous one, and any error in the chain means an agent gets paid the wrong amount.

Without orchestration, commission calculations live in a spreadsheet or a monolithic script that nobody trusts. When the tier structure changes, the deduction logic breaks. When a payment fails, nobody knows whether the calculation was correct but the transfer failed, or whether the whole thing needs to be rerun. Agents dispute their payouts, and finance can't reconstruct how a number was derived.

## The Solution

**You just write the base commission, tier adjustment, deduction, and payout logic. Conductor handles tier calculation retries, deduction sequencing, and payout audit trails.**

Each step in the commission pipeline is a simple, independent worker. one computes the base commission, one applies tier adjustments, one subtracts deductions, one finalizes the payout. Conductor takes care of executing them in sequence, retrying if the payment system is temporarily unavailable, and maintaining a complete audit trail showing exactly how each commission was calculated. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Base rate calculation, tiered adjustment, deduction application, and payout finalization workers each handle one layer of the commission computation.

| Worker | Task | What It Does |
|---|---|---|
| **BaseCommissionWorker** | `cmc_base` | Calculates the base commission amount from the sale price using the standard commission rate |
| **TiersWorker** | `cmc_tiers` | Applies tiered adjustments based on agent seniority, performance level, and team split structure |
| **DeductionsWorker** | `cmc_deductions` | Subtracts brokerage fees, desk fees, marketing costs, and referral fees from the tiered amount |
| **FinalizeWorker** | `cmc_finalize` | Records the net commission and initiates payment to the agent, returning a payment confirmation ID |

Workers implement property transaction steps. listing, inspection, escrow, closing,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
cmc_base
    │
    ▼
cmc_tiers
    │
    ▼
cmc_deductions
    │
    ▼
cmc_finalize

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
java -jar target/commission-calculation-1.0.0.jar

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
java -jar target/commission-calculation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cmc_commission_calculation \
  --version 1 \
  --input '{"agentId": "TEST-001", "salePrice": 100, "transactionId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cmc_commission_calculation -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real brokerage systems. your CRM for agent tier data, your accounting system for fee deductions, your payroll API for commission payouts, and the workflow runs identically in production.

- **BaseCommissionWorker** (`cmc_base`): look up the commission rate from your MLS or brokerage agreement database instead of using a hardcoded percentage
- **TiersWorker** (`cmc_tiers`): query your agent performance database for YTD sales volume and apply the real tier/split schedule from the brokerage's compensation plan
- **DeductionsWorker** (`cmc_deductions`): pull actual deduction schedules (desk fees, E&O insurance, franchise fees) from your accounting system
- **FinalizeWorker** (`cmc_finalize`): initiate an ACH transfer via Stripe/Plaid, record the payout in QuickBooks, and send a commission statement to the agent via email

Update rate structures or deduction categories and the commission pipeline recalculates without structural changes.

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
commission-calculation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/commissioncalculation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CommissionCalculationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BaseCommissionWorker.java
│       ├── DeductionsWorker.java
│       ├── FinalizeWorker.java
│       └── TiersWorker.java
└── src/test/java/commissioncalculation/workers/
    ├── BaseCommissionWorkerTest.java        # 2 tests
    ├── DeductionsWorkerTest.java        # 2 tests
    ├── FinalizeWorkerTest.java        # 2 tests
    └── TiersWorkerTest.java        # 2 tests

```
