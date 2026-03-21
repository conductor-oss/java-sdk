# Reinsurance in Java with Conductor :  Assess Risk, Treaty Lookup, Cede, Confirm, Reconcile

A Java Conductor workflow example demonstrating reinsurance Reinsurance. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Insurers Transfer Risk to Reinsurers for Large Exposures

An insurer writes a $10M commercial property policy. That's more risk than they want on their own books. Reinsurance transfers a portion of the risk (and premium) to reinsurers. The process assesses the risk profile (coverage amount, peril, territory, loss history), identifies which reinsurance treaties apply (quota share for the first $5M, excess of loss for amounts above $5M), cedes the appropriate share (notify the reinsurer, transfer premium), confirms the reinsurer's acceptance, and reconciles the accounts at period end.

Reinsurance cession is governed by treaty terms .  automatic treaties cede without individual approval, facultative treaties require case-by-case negotiation. The cession must be accurate: ceding too much transfers unnecessary premium, ceding too little retains too much risk. Every cession must be tracked for financial reporting and regulatory capital calculations.

## The Solution

**You just write the risk assessment, treaty lookup, cession calculation, confirmation, and reconciliation logic. Conductor handles cession calculation retries, settlement sequencing, and treaty audit trails.**

`AssessRiskWorker` evaluates the policy's risk profile .  coverage amount, peril category, geographic exposure, and loss potential. `TreatyLookupWorker` identifies applicable reinsurance treaties based on the risk characteristics ,  quota share, surplus, excess of loss, or catastrophe treaties. `CedeWorker` calculates and executes the cession ,  determining the ceded premium, retained premium, and commission, then notifying the reinsurer. `ConfirmWorker` records the reinsurer's acceptance and binding confirmation. `ReconcileWorker` reconciles ceded premiums and recoveries for financial reporting. Conductor tracks every cession for treaty compliance and financial audit.

### What You Write: Workers

Treaty analysis, cession calculation, premium allocation, and settlement workers each handle one layer of risk transfer between insurers.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `rin_assess_risk` | Assesses the policy's risk profile .  evaluates the coverage amount and exposure to determine net exposure and retained risk for reinsurance cession decisions |
| **TreatyLookupWorker** | `rin_treaty_lookup` | Identifies the applicable reinsurance treaty .  matches the risk category and exposure amount against treaty terms to find the correct treaty (quota share, excess of loss) and determines the cession amount and reinsurer |
| **CedeWorker** | `rin_cede` | Executes the cession .  transfers the cession amount under the identified treaty, calculates the ceded premium and ceding commission, and generates the cessionId |
| **ConfirmWorker** | `rin_confirm` | Records the reinsurer's acceptance .  confirms the cession binding using the cessionId and reinsurer identity from the treaty lookup |
| **ReconcileWorker** | `rin_reconcile` | Reconciles the cession accounts .  matches ceded premiums against reinsurer statements, identifies variances, and prepares the bordereau for financial reporting |

Workers simulate insurance operations .  claim intake, assessment, settlement ,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
rin_assess_risk
    │
    ▼
rin_treaty_lookup
    │
    ▼
rin_cede
    │
    ▼
rin_confirm
    │
    ▼
rin_reconcile

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
java -jar target/reinsurance-1.0.0.jar

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
java -jar target/reinsurance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rin_reinsurance \
  --version 1 \
  --input '{"policyId": "TEST-001", "coverageAmount": 100, "riskCategory": "general"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rin_reinsurance -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real reinsurance systems .  your exposure database for risk assessment, your treaty management platform for cession calculations, your reinsurer portal for placement confirmation, and the workflow runs identically in production.

- **TreatyLookupWorker** (`rin_treaty_lookup`): query reinsurance treaty databases with treaty terms, retention levels, and cession percentages for automatic treaty application
- **CedeWorker** (`rin_cede`): integrate with reinsurance platforms (RMS, AIR Worldwide) for catastrophe modeling, or send ACORD XML messages for standardized cession communication
- **ReconcileWorker** (`rin_reconcile`): implement quarterly bordereau reconciliation matching ceded premiums against reinsurer statements, with loss recovery tracking

Update treaty terms or cession formulas and the reinsurance pipeline adjusts transparently.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
reinsurance-reinsurance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reinsurance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReinsuranceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessRiskWorker.java
│       ├── CedeWorker.java
│       ├── ConfirmWorker.java
│       ├── ReconcileWorker.java
│       └── TreatyLookupWorker.java
└── src/test/java/reinsurance/workers/
    ├── AssessRiskWorkerTest.java        # 1 tests
    └── CedeWorkerTest.java        # 1 tests

```
