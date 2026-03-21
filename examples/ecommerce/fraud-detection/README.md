# Fraud Detection in Java Using Conductor: Parallel Risk Signals, Deterministic Decision

A legitimate customer buys a $5,000 camera for a trip leaving in 2 hours. Your fraud system flags it: high amount, new merchant category, first purchase over $1,000. Card frozen. Customer calls support, furious, while their flight boards without them. Meanwhile, an actual fraudster running $80 test charges across stolen cards sails through because no single transaction crosses your threshold. The problem isn't the rules or the ML model, it's that rule checks, ML scoring, and velocity analysis run sequentially, so you either add latency to every transaction or skip checks to stay fast. This example uses [Conductor](https://github.com/conductor-oss/conductor) to run all risk signals in parallel and feed them into a deterministic decision function, you write the fraud logic, Conductor handles parallelism, retries, durability, and audit-ready observability.

## Fraud Detection Must Be Fast, Thorough, and Auditable

A $2,000 transaction arrives. Is it legitimate or fraudulent? The answer depends on multiple independent signals: Is the transaction velocity unusual (5 transactions in 10 minutes from the same card)? Does the amount exceed known risk thresholds? Does the ML model flag the feature pattern as suspicious? Each check is independent and can run simultaneously.

Running risk checks sequentially adds latency. unacceptable for real-time payment processing where decisions must complete in under 500ms. Running them in parallel gives you the speed of the slowest check, not the sum of all three. The final decision combines all risk signals into a score that determines whether to approve, flag for manual review, or decline. Every decision must be auditable, regulators and dispute teams need to see exactly which risk signals triggered a decline.

## The Solution

**You just write the transaction analysis, rule checks, ML scoring, velocity detection, and decisioning logic. Conductor handles parallel risk scoring, decision routing, and complete fraud analysis audit trails.**

`AnalyzeTransactionWorker` examines the transaction metadata. Amount, merchant, and customer, and extracts a feature vector (amount deviation from historical average, merchant category, distance from home). `FORK_JOIN` dispatches three parallel risk checks: rule evaluation (amount thresholds, new-merchant risk), ML scoring (weighted feature model), and velocity analysis (transaction frequency patterns). After `JOIN` collects all risk signals, `DecideWorker` combines them into a deterministic APPROVE / REVIEW / BLOCK decision. Conductor runs all three checks in parallel for sub-second decisions and records every risk signal for audit compliance.

### What You Write: Workers

Risk signal workers run in parallel. Rule checks, ML scoring, and velocity detection execute concurrently, feeding results into a single decisioning worker.

| Worker | Task | What It Does |
|---|---|---|
| `AnalyzeTransactionWorker` | `frd_analyze_transaction` | Builds a customer profile and extracts a feature vector (amount deviation, merchant category, distance, new-merchant flag) from the transaction inputs |
| `RuleCheckWorker` | `frd_rule_check` | Evaluates fraud rules against the transaction amount: amount > $1000 threshold, new-merchant high-value (> $500), and round-amount detection. Returns overall `ruleResult` (`low_risk` / `medium_risk` / `high_risk`) and which rules fired |
| `MlScoreWorker` | `frd_ml_score` | Scores the transaction using a weighted feature model: amount deviation (0.35), distance from home (0.25), new merchant (0.20), time of day (0.10), merchant category (0.10). Returns `fraudScore` in [0, 1], model version, and confidence |
| `VelocityCheckWorker` | `frd_velocity_check` | Checks transaction velocity patterns: rapid succession, unusual volume, and geographic anomaly flags. Returns overall `velocityResult` (`normal` / `elevated` / `suspicious`) and per-flag details |
| `DecideWorker` | `frd_decide` | Combines rule result, ML fraud score, and velocity result into a final APPROVE / REVIEW / BLOCK decision with risk score and reason |

Workers simulate e-commerce operations: payment processing, inventory checks, shipping, with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### Decision Semantics

`DecideWorker` uses deterministic thresholds to make the final decision:

| Decision | Condition | Risk Score |
|---|---|---|
| **BLOCK** | `fraudScore > 0.8` OR `ruleResult == "high_risk"` OR `velocityResult == "suspicious"` | `max(fraudScore, 0.85)` |
| **REVIEW** | `fraudScore > 0.5` OR `ruleResult == "medium_risk"` | `max(fraudScore, 0.55)` |
| **APPROVE** | All other cases (score <= 0.5, rules pass, velocity normal) | `fraudScore` as-is |

Thresholds are evaluated top-down: BLOCK conditions are checked first, then REVIEW, then APPROVE (the default). The constants `BLOCK_SCORE_THRESHOLD` (0.8) and `REVIEW_SCORE_THRESHOLD` (0.5) are defined in `DecideWorker.java`.

### The Workflow

```
frd_analyze_transaction
    |
    v
FORK_JOIN
    |-- frd_rule_check
    |-- frd_ml_score
    +-- frd_velocity_check
    |
    v
JOIN (wait for all branches)
frd_decide

```

## Example Output

```
=== Fraud Detection Demo ===

Step 1: Registering task definitions...
  Registered: frd_analyze_transaction, frd_rule_check, frd_ml_score, frd_velocity_check, frd_decide

Step 2: Registering workflow 'fraud_detection_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 3a952c99-a1a4-aa6c-9dee-8e387172a72e

  [analyze] Analyzing transaction TXN-98321 | amount=249.99 merchant=MERCH-1234 customer=CUST-5678
  [rule_check] Evaluating rules for transaction TXN-98321 | amount=249.99
  [ml_score] Scoring transaction TXN-98321 with ML model
  [velocity] Checking velocity for customer CUST-5678 on transaction TXN-98321
  [decide] Transaction TXN-98321 | rules=low_risk ml=0.19 velocity=normal
  [decide] Decision: APPROVE (risk=0.19)

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {transactionId=TXN-98321, decision=APPROVE, riskScore=0.19}

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
java -jar target/fraud-detection-1.0.0.jar

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
java -jar target/fraud-detection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fraud_detection_workflow \
  --version 1 \
  --input '{"transactionId": "TXN-98321", "amount": 249.99, "merchantId": "MERCH-1234", "customerId": "CUST-5678"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fraud_detection_workflow -s COMPLETED -c 5

```

## How to Extend

Replace each worker with your real fraud systems, a feature store like Feast for transaction analysis, SageMaker for ML scoring, Redis for velocity tracking, and the workflow runs identically in production.

| Worker | Production Replacement |
|---|---|
| `AnalyzeTransactionWorker` | Feature store (Feast, Tecton) for real-time feature retrieval; customer data platform for profile enrichment |
| `RuleCheckWorker` | Rules engine (Drools, AWS Fraud Detector rules) for configurable, hot-reloadable fraud rules |
| `MlScoreWorker` | ML model endpoint (SageMaker, Vertex AI, or a custom model server) serving a trained fraud model |
| `VelocityCheckWorker` | Redis sorted sets or time-windowed counters for real-time transaction frequency tracking |
| `DecideWorker` | Case management queue (ServiceNow, custom review dashboard) for REVIEW decisions; webhook/notification for BLOCK decisions |

Update your ML model or add a new risk signal and the decision pipeline incorporates it without restructuring.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk).

## Project Structure

```
fraud-detection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/frauddetection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FraudDetectionExample.java   # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTransactionWorker.java
│       ├── RuleCheckWorker.java
│       ├── MlScoreWorker.java
│       ├── VelocityCheckWorker.java
│       └── DecideWorker.java
└── src/test/java/frauddetection/workers/
    ├── DecideWorkerTest.java              # 13 tests: APPROVE/REVIEW/BLOCK thresholds, edge cases
    ├── AnalyzeTransactionWorkerTest.java  # 9 tests: profile output, feature extraction, null safety
    └── RuleCheckWorkerTest.java           # 8 tests: rule evaluation, fired/unfired rules

```
