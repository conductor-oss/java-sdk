# Salvage Recovery in Java with Conductor :  Assess Damage, Salvage, Auction, Settle, Close

A Java Conductor workflow example demonstrating salvage-recovery Salvage Recovery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Total Loss Vehicles Need Salvage, Not Repair

A 2020 sedan with $18K in damage and a pre-loss value of $22K. repair cost exceeds the total loss threshold (typically 70-80% of value). The vehicle is a total loss. The salvage recovery process assesses the damage and confirms total loss, arranges salvage (tow the vehicle to a salvage yard), auctions the salvage (sell the wreck for parts value), settles with the policyholder (pay the actual cash value minus deductible, minus salvage proceeds if the owner retains the vehicle), and closes the claim.

Salvage recovery directly impacts the insurer's bottom line. the auction proceeds offset the claim payment. A $22K claim with $4K in salvage recovery nets to $18K. Efficient salvage handling (fast towing, competitive auction, quick settlement) reduces loss adjustment expenses and improves the combined ratio.

## The Solution

**You just write the damage assessment, salvage valuation, auction listing, settlement, and claim closure logic. Conductor handles auction retries, valuation sequencing, and salvage recovery audit trails.**

`AssessDamageWorker` evaluates the vehicle damage against the pre-loss value to confirm total loss designation and determine the actual cash value. `SalvageWorker` arranges towing to a salvage facility and manages storage until auction. `AuctionWorker` lists the salvage vehicle with auction partners, manages bids, and tracks sale proceeds. `SettleWorker` calculates and issues the settlement payment. actual cash value minus deductible, with any salvage retention adjustments. `CloseWorker` closes the claim file with final financial reconciliation. Conductor tracks the full salvage lifecycle for loss recovery analytics.

### What You Write: Workers

Damage assessment, salvage valuation, auction coordination, and recovery accounting workers each manage one phase of recovering value from insured losses.

| Worker | Task | What It Does |
|---|---|---|
| **AssessDamageWorker** | `slv_assess_damage` | Assesses the vehicle damage. determines total loss status and calculates the salvage value ($4,200) based on the vehicle condition, year, make, and model |
| **SalvageWorker** | `slv_salvage` | Processes the salvage. obtains the salvage title, arranges towing to the salvage yard, and sets the reserve price based on the assessed salvage value |
| **AuctionWorker** | `slv_auction` | Auctions the salvaged vehicle. lists at the reserve price and records the sale proceeds ($5,100) from the winning bid |
| **SettleWorker** | `slv_settle` | Settles the financial recovery. calculates the recovery amount and net recovery from the auction proceeds against the original claim payout |
| **CloseWorker** | `slv_close` | Closes the salvage claim. records the final recovery amount, marks the claim as closed, and generates the closing statement for audit |

Workers implement insurance operations. claim intake, assessment, settlement,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
slv_assess_damage
    │
    ▼
slv_salvage
    │
    ▼
slv_auction
    │
    ▼
slv_settle
    │
    ▼
slv_close

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
java -jar target/salvage-recovery-1.0.0.jar

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
java -jar target/salvage-recovery-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow slv_salvage_recovery \
  --version 1 \
  --input '{"claimId": "TEST-001", "vehicleId": "TEST-001", "damageType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w slv_salvage_recovery -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real salvage systems. your claims platform for damage assessment, Copart or IAA for auction listing, your finance system for recovery settlement, and the workflow runs identically in production.

- **AuctionWorker** (`slv_auction`): integrate with Copart or IAA (Insurance Auto Auctions) APIs for real salvage vehicle listing, bidding, and sale proceeds tracking
- **AssessDamageWorker** (`slv_assess_damage`): use CCC Intelligent Solutions or Mitchell for automated damage appraisal with photo-based AI damage assessment
- **SettleWorker** (`slv_settle`): calculate actual cash value using NADA, Kelley Blue Book, or CCC Valuescope, with tax and fee adjustments per state total loss regulations

Switch auction platforms or valuation tools and the salvage pipeline keeps working.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
salvage-recovery-salvage-recovery/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/salvagerecovery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SalvageRecoveryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessDamageWorker.java
│       ├── AuctionWorker.java
│       ├── CloseWorker.java
│       ├── SalvageWorker.java
│       └── SettleWorker.java
└── src/test/java/salvagerecovery/workers/
    ├── AssessDamageWorkerTest.java        # 1 tests
    └── AuctionWorkerTest.java        # 1 tests

```
