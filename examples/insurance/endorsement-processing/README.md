# Endorsement Processing in Java with Conductor :  Request Change, Assess Impact, Price, Approve, Apply

A Java Conductor workflow example for mid-term policy endorsement processing .  receiving a change request (adding a driver, changing coverage limits, updating a vehicle), assessing the impact on the policy, repricing the premium adjustment (+$150/year), approving the endorsement, and applying the amendment to the active policy. Each step depends on the previous: the impact assessment feeds into repricing, the premium change feeds into approval, and the endorsement is only applied after approval. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Mid-Term Policy Changes Require Impact Assessment, Repricing, and Approval Before Amendment

When a policyholder requests a mid-term change (adding a driver, increasing coverage, changing a deductible), the insurer must assess the coverage impact, calculate the premium adjustment, obtain approval for the change, and apply the endorsement to the active policy. The premium adjustment depends on the impact assessment, and the endorsement is only applied after approval. If the pricing step fails, you need to retry it without re-assessing the impact.

## The Solution

**You just write the change request intake, impact assessment, repricing, approval, and policy amendment logic. Conductor handles coverage evaluation retries, amendment routing, and endorsement audit trails.**

`RequestChangeWorker` captures the endorsement request .  what's changing, effective date, and supporting documentation. `AssessWorker` evaluates the impact on risk exposure, coverage adequacy, and underwriting acceptability. `PriceWorker` calculates the pro-rated premium change ,  additional premium for increased coverage or refund for reduced coverage. `ApproveWorker` routes the endorsement through approval ,  auto-approving standard changes or escalating exceptions to underwriters. `ApplyWorker` updates the active policy ,  modifying coverage terms, issuing an updated declarations page, and adjusting the billing schedule. Conductor tracks the endorsement from request to application.

### What You Write: Workers

Change request intake, coverage evaluation, premium adjustment, and policy amendment workers each handle one aspect of mid-term policy modifications.

| Worker | Task | What It Does |
|---|---|---|
| **RequestChangeWorker** | `edp_request_change` | Receives the endorsement request .  logs the policyId and changeType, creates an endorsementId, and initiates the change process |
| **AssessWorker** | `edp_assess` | Assesses the impact of the requested change on the policy .  evaluates how the change type and details affect coverage, limits, and risk profile |
| **PriceWorker** | `edp_price` | Calculates the premium adjustment for the endorsement .  uses the impact assessment to determine the pro-rata premium change (+$150/year) for the remaining policy term |
| **ApproveWorker** | `edp_approve` | Approves the endorsement .  reviews the policyId and premium change amount, then authorizes the amendment to the active policy |
| **ApplyWorker** | `edp_apply` | Applies the endorsement to the active policy .  amends the policy record with the approved changes using the endorsementId, updates the declarations page, and triggers billing adjustment |

Workers simulate insurance operations .  claim intake, assessment, settlement ,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
edp_request_change
    │
    ▼
edp_assess
    │
    ▼
edp_price
    │
    ▼
edp_approve
    │
    ▼
edp_apply
```

## Example Output

```
=== Example 704: Endorsement Processing ===

Step 1: Registering task definitions...
  Registered: edp_request_change, edp_assess, edp_price, edp_approve, edp_apply

Step 2: Registering workflow 'edp_endorsement_processing'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apply] Processing
  [approve] Endorsement approved
  [assess] Change impact assessed
  [price] Premium adjustment: +$150/year
  [request_change] Processing

  Status: COMPLETED
  Output: {applied=..., effectiveDate=..., approved=..., impact=...}

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
java -jar target/endorsement-processing-1.0.0.jar
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
java -jar target/endorsement-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow edp_endorsement_processing \
  --version 1 \
  --input '{"policyId": "POL-704", "POL-704": "changeType", "changeType": "add-vehicle", "add-vehicle": "details", "details": "2024 Honda Civic", "2024 Honda Civic": "sample-2024 Honda Civic"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w edp_endorsement_processing -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real endorsement systems .  your policy admin for change requests, your rating engine for premium adjustments, your approval platform for underwriter sign-off, and the workflow runs identically in production.

- **PriceWorker** (`edp_price`): implement pro-rata premium calculation with short-rate tables for mid-term changes, minimum earned premium rules, and endorsement-specific rate factors
- **ApplyWorker** (`edp_apply`): update policies in Guidewire PolicyCenter or Duck Creek Policy, generate amended declarations pages, and trigger billing adjustments in the billing system
- **AssessWorker** (`edp_assess`): integrate with underwriting rules engines (Drools, FICO Blaze) for automated risk assessment of common endorsement types

Change coverage evaluation rules or premium adjustment formulas and the endorsement pipeline stays intact.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
endorsement-processing-endorsement-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/endorsementprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EndorsementProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyWorker.java
│       ├── ApproveWorker.java
│       ├── AssessWorker.java
│       ├── PriceWorker.java
│       └── RequestChangeWorker.java
└── src/test/java/endorsementprocessing/workers/
    ├── PriceWorkerTest.java        # 1 tests
    └── RequestChangeWorkerTest.java        # 1 tests
```
