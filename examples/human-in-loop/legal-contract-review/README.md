# Legal Contract Review in Java Using Conductor: AI Term Extraction, Human Legal Review via WAIT, and Finalization

A Java Conductor workflow example for legal contract review: using AI to extract key terms (parties, payment terms, liability caps, termination clauses) and risk flags (unlimited liability, auto-renewal, broad IP assignment) from a contract, pausing at a WAIT task for a lawyer to verify the extracted terms and approve or flag issues, then finalizing the reviewed contract. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Contracts Need AI-Assisted Extraction Followed by Human Legal Review

Legal contracts contain key terms (parties, payment terms, liability caps, termination clauses) and risk flags (unlimited liability, auto-renewal, broad IP assignment) that AI can extract, but a human lawyer must verify before the contract is signed. The workflow extracts terms via AI, pauses for legal review via a WAIT task, then finalizes the review. If finalization fails after the lawyer approves, you need to retry it without asking the lawyer to re-review.

## The Solution

**You just write the term-extraction and review-finalization workers. Conductor handles the durable pause for lawyer review and the retry logic.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

LcrExtractTermsWorker pulls key clauses and risk flags from contracts, and LcrFinalizeWorker records the lawyer's redline notes, the WAIT task between them holds state for days if needed.

| Worker | Task | What It Does |
|---|---|---|
| **LcrExtractTermsWorker** | `lcr_extract_terms` | Uses AI to extract key contract terms (parties, payment terms, liability caps, IP clauses, termination conditions) and flag risk areas (unlimited liability, auto-renewal) |
| *WAIT task* | `lcr_legal_review` | Pauses with the extracted terms and risk flags until a lawyer reviews, verifies accuracy, and submits their assessment via `POST /tasks/{taskId}` | Built-in Conductor WAIT.; no worker needed |
| **LcrFinalizeWorker** | `lcr_finalize` | Finalizes the contract review. records the lawyer's approval and any redline notes, updates the contract status in the CLM system |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
lcr_extract_terms
    │
    ▼
legal_review [WAIT]
    │
    ▼
lcr_finalize
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
java -jar target/legal-contract-review-1.0.0.jar
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
=== Legal Contract Review Demo: AI Extract Terms + Human Legal Review ===

Step 1: Registering task definitions...
  Registered: lcr_extract_terms, lcr_finalize

Step 2: Registering workflow 'legal_contract_review_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  [lcr_extract_terms] Extracting contract terms...
  [lcr_extract_terms] Extracted 5 terms, 3 risk flags.

  Workflow ID: c4d5e6f7-...

Step 5: Waiting for completion (WAIT task requires external signal)...
  Status: RUNNING

Note: Workflow is paused at the legal_review WAIT task.
      A lawyer must review the extracted terms and risk flags, then complete the task.
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/legal-contract-review-1.0.0.jar --workers
```

Then use the CLI in a separate terminal to start and manage workflows.

### Start a workflow run

```bash
conductor workflow start \
  --workflow legal_contract_review_demo \
  --version 1 \
  --input '{"contractId": "MSA-2026-0147"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w legal_contract_review_demo -s COMPLETED -c 5
```

### Completing the WAIT task (lawyer review)

When the workflow hits the `legal_review` WAIT task, it pauses with the extracted key terms and risk flags until a lawyer completes the review. The workflow status will show as `RUNNING` with the WAIT task `IN_PROGRESS`.

**Step 1: Find the WAIT task ID**

```bash
# Get the execution details: look for the task named "legal_review"
conductor workflow get-execution <workflow_id> -c
```

The task ID is in the `taskId` field of the `legal_review_ref` task. The WAIT task's input will contain the extracted `keyTerms` and `riskFlags` for the lawyer to review.

**Step 2: Approve the contract**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowInstanceId": "<workflow_id>",
    "taskId": "<task_id>",
    "status": "COMPLETED",
    "outputData": {
      "approved": true,
      "reviewedBy": "jsmith@lawfirm.com",
      "reviewedAt": "2026-03-14T16:00:00Z",
      "notes": "Liability cap negotiated down to $5M. Auto-renewal opt-out window added.",
      "redlineRequested": false
    }
  }'
```

**Step 2 (alternative): Flag issues and request redlines**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "workflowInstanceId": "<workflow_id>",
    "taskId": "<task_id>",
    "status": "COMPLETED",
    "outputData": {
      "approved": false,
      "reviewedBy": "jsmith@lawfirm.com",
      "reviewedAt": "2026-03-14T16:00:00Z",
      "redlineRequested": true,
      "issues": [
        "Unlimited liability clause must be capped at 2x contract value",
        "Auto-renewal needs a 30-day opt-out window",
        "Termination notice should be reduced from 90 to 30 days"
      ]
    }
  }'
```

After the lawyer completes the review, the workflow automatically resumes and the `lcr_finalize` worker records the outcome.

## How to Extend

Each worker handles one piece of the contract pipeline. Connect your legal AI platform (Kira Systems, LawGeex, Ironclad) for extraction and your CLM system (DocuSign CLM, Agiloft) for finalization, and the review workflow stays the same.

- **LcrExtractTermsWorker** (`lcr_extract_terms`): use a legal NLP service like Kira Systems, LawGeex, or an LLM (Claude, GPT-4) fine-tuned on contracts to extract real terms and identify risk flags from uploaded contract PDFs
- **WAIT task**: trigger from your legal review dashboard or CLM (Contract Lifecycle Management) system, where the lawyer sees the extracted terms, marks up risk flags, and clicks Approve/Reject to call `POST /tasks/{taskId}`
- **LcrFinalizeWorker** (`lcr_finalize`): push the reviewed contract to a CLM system like Ironclad, DocuSign CLM, or Agiloft, update the deal record in your CRM (Salesforce, HubSpot), and notify the requesting team via email or Slack
- **Add a redline loop**: if the lawyer flags issues, add a sub-workflow that sends redlines to the counterparty, waits for a revised contract, re-extracts terms, and returns to the legal review WAIT task

Replace the simulated extraction with a real contract AI platform and the review pipeline continues to work as designed.

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
legal-contract-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/legalcontractreview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LegalContractReviewExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── LcrExtractTermsWorker.java   # AI-based term extraction with risk flags
│       └── LcrFinalizeWorker.java       # Records lawyer's review outcome
└── src/test/java/legalcontractreview/workers/
    ├── LcrExtractTermsWorkerTest.java   # 8 tests. Key terms, risk flags, determinism
    └── LcrFinalizeWorkerTest.java       # 5 tests. Completion, output shape
```
