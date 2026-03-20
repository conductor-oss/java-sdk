# Policy Issuance in Java with Conductor :  Underwrite, Approve, Generate Policy, Issue, Deliver

A Java Conductor workflow example for end-to-end insurance policy issuance .  underwriting the applicant to determine risk class, approving the application and setting the premium, generating the policy document, officially issuing the policy in the system of record, and delivering the policy documents to the policyholder. Each step feeds into the next: underwriting produces the riskClass that approval uses to set the premium, the approved premium feeds into document generation which produces the policyId, and that policyId flows through issuance and delivery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## New Insurance Applications Must Flow Through Underwriting, Approval, and Issuance

When a new insurance application arrives, the insurer must underwrite the applicant (assess risk class based on coverage type), approve the application (determine the premium from the underwriting result), generate the policy document with all terms and conditions, officially issue the policy in the administration system, and deliver the documents to the policyholder. If document generation fails after approval, you need to retry it without re-underwriting or re-approving the application.

## The Solution

**You just write the underwriting, approval, document generation, policy issuance, and delivery logic. Conductor handles underwriting retries, document generation sequencing, and policy lifecycle audit trails.**

`UnderwriteWorker` evaluates the application .  property inspection data, credit score, claims history, coverage amount vs: property value, and produces a risk assessment with a recommendation. `ApproveWorker` makes the acceptance decision based on underwriting guidelines, with auto-approval for standard risks and referral for exceptions. `GeneratePolicyWorker` creates the policy document ,  declarations page, coverage schedule, endorsements, and exclusions specific to the approved coverage. `IssueWorker` records the policy in the administration system with policy number, effective dates, and premium schedule. `DeliverWorker` sends the policy package to the policyholder via their preferred channel. Conductor tracks the full issuance timeline.

### What You Write: Workers

Application intake, underwriting, policy document generation, and activation workers each handle one step of bringing a new policy into force.

| Worker | Task | What It Does |
|---|---|---|
| **UnderwriteWorker** | `pis_underwrite` | Underwrites the applicant .  evaluates the applicantId and coverageType to determine the risk classification (standard, preferred, substandard) and underwriting result |
| **ApproveWorker** | `pis_approve` | Approves the application based on the underwriting result .  reads the riskClass to determine the premium and issues the approval decision |
| **GeneratePolicyWorker** | `pis_generate_policy` | Generates the policy document .  creates the declarations page, coverage schedule, and terms using the applicant details, coverage type, and approved premium, then outputs the policyId |
| **IssueWorker** | `pis_issue` | Officially issues the policy .  records the policy in the administration system with the assigned policyId, setting the effective date and policy status to active |
| **DeliverWorker** | `pis_deliver` | Delivers the policy documents to the policyholder .  sends the generated policy via the appropriate delivery method (email, postal mail, portal) |

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
pis_underwrite
    │
    ▼
pis_approve
    │
    ▼
pis_generate_policy
    │
    ▼
pis_issue
    │
    ▼
pis_deliver
```

## Example Output

```
=== Example 703: Policy Issuance ===

Step 1: Registering task definitions...
  Registered: pis_underwrite, pis_approve, pis_generate_policy, pis_issue, pis_deliver

Step 2: Registering workflow 'pis_policy_issuance'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [approve] Policy approved .  standard risk
  [deliver] Processing
  [generate] Policy document generated
  [issue] Processing
  [underwrite] Processing

  Status: COMPLETED
  Output: {approved=..., premium=..., delivered=..., method=...}

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
java -jar target/policy-issuance-1.0.0.jar
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
java -jar target/policy-issuance-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pis_policy_issuance \
  --version 1 \
  --input '{"applicantId": "APP-703", "APP-703": "coverageType", "coverageType": "auto", "auto": "requestedAmount", "requestedAmount": 100000}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pis_policy_issuance -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real issuance stack .  your underwriting engine for risk classification, your policy admin system for document generation, your distribution platform for policy delivery, and the workflow runs identically in production.

- **UnderwriteWorker** (`pis_underwrite`): integrate with third-party data: LexisNexis for identity verification, Verisk for property risk scores, or TransUnion for insurance credit scores
- **GeneratePolicyWorker** (`pis_generate_policy`): use Apache PDFBox or DocuSign templates to generate branded policy documents with dynamic coverage schedules and endorsement riders
- **IssueWorker** (`pis_issue`): create policies in Guidewire PolicyCenter, Duck Creek Policy, or a custom policy administration system with proper effective date and billing schedule setup

Swap underwriting engines or document generators and the issuance pipeline structure persists.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
policy-issuance-policy-issuance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/policyissuance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PolicyIssuanceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── DeliverWorker.java
│       ├── GeneratePolicyWorker.java
│       ├── IssueWorker.java
│       └── UnderwriteWorker.java
└── src/test/java/policyissuance/workers/
    ├── IssueWorkerTest.java        # 1 tests
    └── UnderwriteWorkerTest.java        # 1 tests
```
