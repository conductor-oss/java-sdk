# Customer Onboarding KYC in Java Using Conductor: Risk Assessment, SWITCH for Auto-Approve vs. Manual Review, and Account Activation

A Java Conductor workflow example for Know Your Customer (KYC) onboarding. performing an automated risk assessment on the customer, using SWITCH to route low-risk customers to instant auto-approval or high-risk customers to a WAIT task for manual compliance review, and then activating the account. Demonstrates the pattern where automation handles the common case and humans only intervene when risk flags are raised. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to onboard customers with KYC compliance checks. Each customer is assessed for risk based on their identity, country of residence, source of funds, and PEP (Politically Exposed Person) status. Low-risk customers: those with clean identity checks, domestic addresses, and no PEP flags, should be auto-approved and activated instantly for a frictionless experience. High-risk customers, those with sanctions hits, high-risk jurisdictions, or PEP connections, must be routed to a compliance analyst for manual review before the account can be activated. The analyst reviews the flags, may request additional documentation, and makes a final approve/reject decision. Without this conditional routing, you either manually review every customer (slow and expensive) or auto-approve everyone (regulatory violation).

Without orchestration, you'd build a monolithic KYC system that runs risk checks, branches with if/else into auto-approve or manual-review queues, polls a database for analyst decisions, and activates accounts. If the identity verification API is down, you'd need retry logic. If the system crashes after the analyst approves but before activation, the customer is approved but never gets access. BSA/AML regulators require a complete audit trail of every KYC decision, including who reviewed high-risk cases and when.

## The Solution

**You just write the KYC risk-check and account-activation workers. Conductor handles the routing between auto-approve and manual review.**

The SWITCH + WAIT pattern is the key here. After the KYC check determines the risk level and whether manual review is needed, a SWITCH routes the workflow: if `needsReview` is true (high-risk), the workflow enters a WAIT task for a compliance analyst's decision; if false (low-risk), it auto-approves via SET_VARIABLE and proceeds directly to activation. Conductor takes care of routing based on risk assessment, holding the workflow durably while a compliance analyst reviews high-risk cases, activating accounts only after approval (automatic or manual), and providing a complete BSA/AML audit trail of every KYC decision. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

KycCheckWorker assesses identity and watchlist risk, while KycActivateWorker enables account access. Neither manages the routing between auto-approval and manual compliance review.

| Worker | Task | What It Does |
|---|---|---|
| **KycCheckWorker** | `kyc_check` | Performs automated KYC risk assessment: checks identity, watchlists, PEP status, and jurisdiction risk, returning a risk level and needsReview flag with any compliance flags |
| *SWITCH task* | `kyc_review_decision` | Routes based on needsReview. "true" sends to a manual WAIT task for compliance analyst review, default auto-approves and proceeds to activation | Built-in Conductor SWITCH.; no worker needed |
| *WAIT task* | `manual_kyc_review` | Pauses for a compliance analyst to review the flags and make an approve/reject decision via `POST /tasks/{taskId}` | Built-in Conductor WAIT.; no worker needed |
| **KycActivateWorker** | `kyc_activate` | Activates the customer account after KYC approval (automatic or manual), enabling login and product access |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
kyc_check
    │
    ▼
SWITCH (kyc_review_decision_ref)
    ├── true: manual_kyc_review
    └── default: kyc_auto_approved
    │
    ▼
kyc_activate

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
java -jar target/customer-onboarding-kyc-1.0.0.jar

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

## Example Output

```
=== Customer Onboarding KYC Demo: Auto/Human Review ===

Step 1: Registering task definitions...
  Registered: kyc_check, kyc_activate

Step 2: Registering workflow 'customer_onboarding_kyc'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow (low-risk customer, auto-approved)...
  [kyc_check] Checking customer: Alice Smith (id=C-1001, risk=low)
  [kyc_check] Low risk. Auto-approved.

  Workflow ID: b3f1a2c4-...

Step 5: Waiting for completion...
  [kyc_activate] Activating customer: Alice Smith (id=C-1001)
  [kyc_activate] Customer activated successfully.
  Status: COMPLETED
  Output: {activated=true, customerId=C-1001, needsReview=false}

Result: PASSED

```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/customer-onboarding-kyc-1.0.0.jar --workers

```

Then use the CLI in a separate terminal to start and manage workflows.

### Start a workflow run

Low-risk customer (auto-approved, no WAIT):

```bash
conductor workflow start \
  --workflow customer_onboarding_kyc \
  --version 1 \
  --input '{"customerId": "C-1001", "customerName": "Alice Smith", "riskLevel": "low"}'

```

High-risk customer (requires compliance analyst review via WAIT):

```bash
conductor workflow start \
  --workflow customer_onboarding_kyc \
  --version 1 \
  --input '{"customerId": "C-2050", "customerName": "Ivan Petrov", "riskLevel": "high"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w customer_onboarding_kyc -s COMPLETED -c 5

```

### Completing the WAIT task (compliance analyst review)

When the workflow hits the `manual_kyc_review` WAIT task (high-risk customers only), it pauses until a compliance analyst completes the task. The workflow status will show as `RUNNING` with the WAIT task `IN_PROGRESS`.

**Step 1: Find the WAIT task ID**

```bash
# Get the execution details: look for the task named "manual_kyc_review"
conductor workflow get-execution <workflow_id> -c

```

The task ID is in the `taskId` field of the `manual_kyc_review_ref` task.

**Step 2: Approve the customer**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<task_id>", "status": "COMPLETED", "outputData": {"approved": true, "reviewedBy": "compliance-analyst@example.com", "reviewedAt": "2026-03-14T14:30:00Z", "notes": "Enhanced due diligence completed. Source of funds verified."}}'

```

**Step 2 (alternative): Reject the customer**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"workflowInstanceId": "<workflow_id>", "taskId": "<task_id>", "status": "FAILED", "reasonForIncompletion": "KYC review failed. Sanctions match confirmed", "outputData": {"approved": false, "reviewedBy": "compliance-analyst@example.com", "reason": "Confirmed match on OFAC SDN list"}}'

```

After approval, the workflow automatically resumes and the `kyc_activate` worker activates the customer account.

## How to Extend

Each worker encapsulates a single KYC step. Swap in your identity verification provider (Jumio, Onfido, Trulioo) and core banking activation API, and the onboarding workflow remains unchanged.

- **KycCheckWorker** → integrate with real KYC providers (Jumio for document verification, Onfido for biometric checks, ComplyAdvantage or Refinitiv for sanctions/PEP screening, LexisNexis for identity verification)
- **WAIT task** → complete it from your compliance review UI with `{ "approved": true, "notes": "..." }` or `{ "approved": false, "reason": "failed enhanced due diligence" }`
- **KycActivateWorker** → provision the account in your core banking system, set up billing in Stripe, send welcome emails, and grant product access
- Add an **EnhancedDueDiligenceWorker** for high-risk cases that requests additional documentation (source of funds, bank statements) before the manual review WAIT
- Add a **SARFilingWorker** with a SWITCH case for suspicious activity detected during review, triggering a Suspicious Activity Report filing with FinCEN
- Add ongoing monitoring by scheduling periodic re-checks of activated customers against updated watchlists

Plug in a production KYC provider and the risk-routing and account activation flow remains the same.

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
customer-onboarding-kyc/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/customeronboardingkyc/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomerOnboardingKycExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── KycActivateWorker.java   # Activates account after KYC approval
│       └── KycCheckWorker.java      # Risk assessment with needsReview flag
└── src/test/java/customeronboardingkyc/workers/
    ├── KycActivateWorkerTest.java   # 5 tests. Activation, defaults, types
    └── KycCheckWorkerTest.java      # 8 tests. High/low risk, case sensitivity, defaults

```
