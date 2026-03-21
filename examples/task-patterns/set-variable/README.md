# Set Variable in Java with Conductor

Demonstrates SET_VARIABLE system task for storing intermediate state accessible via ${workflow.variables.key} Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to accumulate state across multiple workflow steps .  processing a list of items to compute a total amount and category, then applying business rules based on those intermediate results (does this order need approval? what is the risk level?), and finally producing a decision that uses variables from both steps. The rules step needs the total amount and category from the processing step. The finalize step needs the original totals plus the approval and risk results. Intermediate state must be accessible from any downstream task without threading it through every task's input/output mapping.

Without orchestration, you'd store intermediate values in instance variables, a shared map, or a database, threading state manually between method calls. If the process crashes between computing the total and applying rules, the intermediate state is lost. There is no way to inspect what the accumulated state looked like at any point in the execution without adding custom logging at every step.

## The Solution

**You just write the item processing, rules evaluation, and finalization workers. Conductor handles the workflow variable storage and cross-step state sharing.**

This example demonstrates Conductor's SET_VARIABLE system task for storing intermediate state in workflow variables accessible via `${workflow.variables.key}`. ProcessItemsWorker computes totalAmount, itemCount, and category from the input items. A SET_VARIABLE task stores those results as workflow variables. ApplyRulesWorker reads the stored variables (not the task output directly) and applies business rules .  orders over $500 need approval, high-value orders are high risk. A second SET_VARIABLE stores the rule results (needsApproval, riskLevel). FinalizeWorker reads all accumulated variables (totals + rules) to produce the final decision. Workflow variables persist across the entire execution and are visible in the Conductor UI, making intermediate state inspectable at any point.

### What You Write: Workers

Three workers build the order processing pipeline: ProcessItemsWorker computes totals and category, ApplyRulesWorker evaluates approval and risk thresholds from stored variables, and FinalizeWorker assembles the final decision using accumulated state from all previous steps.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyRulesWorker** | `sv_apply_rules` | Applies business rules based on intermediate state stored in workflow variables. Rules: - needsApproval: totalAmount ... |
| **FinalizeWorker** | `sv_finalize` | Produces the final decision string from all accumulated workflow variables. |
| **ProcessItemsWorker** | `sv_process_items` | Processes a list of items: computes total amount, count, and category. Category rules: - high-value: total >= 1000 - ... |

Workers simulate their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic .  the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
sv_process_items
    │
    ▼
store_item_results [SET_VARIABLE]
    │
    ▼
sv_apply_rules
    │
    ▼
store_rule_results [SET_VARIABLE]
    │
    ▼
sv_finalize

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
java -jar target/set-variable-1.0.0.jar

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
java -jar target/set-variable-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow set_variable_demo \
  --version 1 \
  --input '{"items": [{"id": "ITEM-001", "quantity": 2}]}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w set_variable_demo -s COMPLETED -c 5

```

## How to Extend

Replace the item processor and rules engine with your real business logic, and the SET_VARIABLE-based state accumulation pattern works unchanged.

- **ProcessItemsWorker** (`sv_process_items`): compute real totals from order line items in your database, classify orders by value tier, and calculate applicable discounts or taxes
- **ApplyRulesWorker** (`sv_apply_rules`): evaluate real business rules from a rules engine (Drools, Easy Rules) or policy service: approval thresholds, fraud risk scoring, compliance checks, and credit limit validation
- **FinalizeWorker** (`sv_finalize`): produce the final order decision, write it to your order management system, trigger approval workflows for flagged orders, and send notifications based on the risk level

Changing the business rules or approval thresholds inside any worker does not affect the SET_VARIABLE state management, since workflow variables are stored and accessed independently of worker logic.

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
set-variable/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/setvariable/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SetVariableExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyRulesWorker.java
│       ├── FinalizeWorker.java
│       └── ProcessItemsWorker.java
└── src/test/java/setvariable/workers/
    ├── ApplyRulesWorkerTest.java        # 6 tests
    ├── FinalizeWorkerTest.java        # 6 tests
    └── ProcessItemsWorkerTest.java        # 6 tests

```
