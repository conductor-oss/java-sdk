# Prior Authorization in Java Using Conductor :  Request Submission, Clinical Criteria Review, Three-Way Decision Routing, and Provider Notification

A Java Conductor workflow example for prior authorization. submitting authorization requests with clinical justification, reviewing against medical necessity criteria, routing to auto-approve, auto-deny, or manual clinical review via SWITCH, and notifying the provider of the determination. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process prior authorization requests for medical procedures, imaging studies, or specialty medications. A provider submits a request with the patient ID, procedure code, and clinical justification. The request must be reviewed against the payer's medical necessity criteria. InterQual, MCG, or custom clinical guidelines. Based on the criteria review, the request is routed to one of three paths: auto-approve if all criteria are met, auto-deny if the procedure clearly does not meet medical necessity, or escalate to a medical director for manual peer review if the case is ambiguous. Regardless of the determination, the provider must be notified with the decision, authorization number (if approved), or denial reason with appeal instructions. State prompt-decision laws require turnaround within specific timeframes.

Without orchestration, you'd build a monolithic prior auth engine that receives the 278 request, runs the criteria check, branches with if/else into approve/deny/review paths, and sends the notification. If the criteria engine is slow, the entire determination is delayed. If the system crashes after the criteria review but before the decision is recorded, the provider has no determination and the clock is ticking on regulatory timeframes. Payers must maintain a complete audit trail of every authorization decision for regulatory and accreditation reviews.

## The Solution

**You just write the prior auth workers. Request submission, criteria review, approve/deny/review routing, and provider notification. Conductor handles conditional SWITCH routing between approve, deny, and manual review paths, automatic retries, and timestamped records for regulatory compliance.**

Each stage of the prior authorization process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of submitting before reviewing criteria, routing to the correct decision path (approve, deny, or manual review) via SWITCH, always notifying the provider regardless of which path was taken, and maintaining a complete audit trail with regulatory timeframes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Six workers cover the prior auth process: SubmitRequestWorker intakes the request, ReviewCriteriaWorker evaluates medical necessity, ApproveWorker and DenyWorker handle clear-cut decisions, ManualReviewWorker escalates ambiguous cases, and NotifyWorker sends the determination to the provider.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitRequestWorker** | `pa_submit_request` | Receives the authorization request with patient, procedure, and clinical justification |
| **ReviewCriteriaWorker** | `pa_review_criteria` | Evaluates the request against medical necessity criteria (InterQual, MCG) and returns a decision: approve, deny, or review |
| **ApproveWorker** | `pa_approve` | Records the approval, generates an authorization number, and sets the validity period |
| **DenyWorker** | `pa_deny` | Records the denial with the specific clinical reason and appeal instructions |
| **ManualReviewWorker** | `pa_manual_review` | Escalates ambiguous cases to a medical director for peer-to-peer clinical review |
| **NotifyWorker** | `pa_notify` | Sends the determination (approval with auth number, or denial with reason) to the requesting provider |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
pa_submit_request
    │
    ▼
pa_review_criteria
    │
    ▼
SWITCH (pa_switch_ref)
    ├── approve: pa_approve
    ├── deny: pa_deny
    ├── review: pa_manual_review
    └── default: pa_manual_review
    │
    ▼
pa_notify

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
java -jar target/prior-authorization-1.0.0.jar

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
java -jar target/prior-authorization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow prior_authorization_workflow \
  --version 1 \
  --input '{"authId": "TEST-001", "patientId": "TEST-001", "procedure": "sample-procedure", "clinicalReason": "sample-clinicalReason"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w prior_authorization_workflow -s COMPLETED -c 5

```

## How to Extend

Connect SubmitRequestWorker to your 278 transaction processor, ReviewCriteriaWorker to your clinical criteria engine (InterQual, MCG), and NotifyWorker to your provider portal for determination delivery. The workflow definition stays exactly the same.

- **SubmitRequestWorker** → parse real 278 EDI transactions or integrate with your prior auth portal for electronic submissions
- **ReviewCriteriaWorker** → integrate with InterQual, MCG, or your payer's custom medical policy rules engine for automated criteria evaluation
- **ManualReviewWorker** → route to your medical director's review queue with a human-in-the-loop WAIT task for peer-to-peer decisions
- **NotifyWorker** → send real 278 response transactions to providers or push notifications to your provider portal
- Add a **DocumentRequestWorker** before manual review to request additional clinical records from the provider when documentation is insufficient
- Add a **AppealWorker** to handle provider appeals of denied authorizations with a new clinical review cycle

Connect each worker to your 278 transaction processor, clinical criteria engine, and provider notification system while returning the same fields, and the authorization workflow operates unchanged.

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
prior-authorization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/priorauthorization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PriorAuthorizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── DenyWorker.java
│       ├── ManualReviewWorker.java
│       ├── NotifyWorker.java
│       ├── ReviewCriteriaWorker.java
│       └── SubmitRequestWorker.java
└── src/test/java/priorauthorization/workers/
    ├── ApproveWorkerTest.java        # 2 tests
    ├── DenyWorkerTest.java        # 2 tests
    ├── ManualReviewWorkerTest.java        # 2 tests
    ├── NotifyWorkerTest.java        # 2 tests
    ├── ReviewCriteriaWorkerTest.java        # 3 tests
    └── SubmitRequestWorkerTest.java        # 2 tests

```
