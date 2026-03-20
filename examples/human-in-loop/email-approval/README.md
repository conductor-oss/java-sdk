# Email-Based Approval in Java Using Conductor :  Request Preparation, Approval Email with Click Links, WAIT for Response, and Decision Processing

A Java Conductor workflow example for email-based approvals .  preparing a request, sending an email with embedded approve/reject URLs that map to the workflow's WAIT task, pausing until the approver clicks one of the links, and processing the resulting decision. Demonstrates how approvers who do not have dashboard access can approve directly from their inbox with a single click. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need approvals from people who live in their email .  executives, external partners, or field workers who will never log into an approval dashboard. The workflow must prepare the request, send an email containing one-click approve and reject URLs, and wait for the approver to click one. Each URL maps to a unique Conductor WAIT task completion endpoint, so clicking "Approve" sends `{ "decision": "approved" }` and clicking "Reject" sends `{ "decision": "rejected" }` ,  completing the WAIT task and resuming the workflow. The decision must then be processed downstream. If the email provider is temporarily down, the send must retry without re-preparing the request. If the decision processing fails, it must retry without re-sending the email.

Without orchestration, you'd send the email, embed callback URLs pointing to a custom webhook server, poll a database for the response, and then advance the workflow. If the webhook server is down when the approver clicks, the approval is lost. If the system crashes between receiving the click and processing the decision, the request is stuck. There is no audit trail showing when the email was sent, when the link was clicked, or how long the approver took to respond.

## The Solution

**You just write the request-preparation, email-sending, and decision-processing workers. Conductor handles the durable wait for the approver's one-click response.**

The WAIT task is the key pattern here. After preparing the request and sending the email with embedded approve/reject URLs, the workflow pauses at the WAIT task. When the approver clicks a link, the URL triggers a Conductor API call that completes the WAIT task with the decision. The process-decision worker then handles the result. Conductor takes care of holding the workflow durably while the approver reads their email, accepting the one-click decision via the API, retrying email delivery if the email provider is temporarily unavailable (without re-preparing), and providing a complete timeline from email sent to link clicked to decision processed. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

PrepareWorker validates the request, SendEmailWorker delivers approve/reject links, and ProcessDecisionWorker handles the outcome. None of them manage the durable wait for the email click.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `ea_prepare` | Prepares the approval request .  validates the requester and subject, enriches with context, and marks it as ready for email delivery |
| **SendEmailWorker** | `ea_send_email` | Sends an approval email with embedded approve/reject click URLs that map to the WAIT task's completion endpoint |
| *WAIT task* | `email_response` | Pauses the workflow until the approver clicks an approve or reject link in the email, which completes this task via the Conductor API with the decision | Built-in Conductor WAIT .  no worker needed |
| **ProcessDecisionWorker** | `ea_process_decision` | Processes the approval decision .  updates the request status, triggers downstream actions, and notifies the requester of the outcome |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ea_prepare
    │
    ▼
ea_send_email
    │
    ▼
email_response [WAIT]
    │
    ▼
ea_process_decision
```

## Example Output

```
=== Email-Based Approval with Click Links Demo ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'email_approval_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [prepare] Processing
  [ea_process_decision] Processing decision:
  [ea_send_email] Sending approval email for workflow

  Status: COMPLETED
  Output: {ready=..., processed=..., sent=..., approveUrl=...}

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
java -jar target/email-approval-1.0.0.jar
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
java -jar target/email-approval-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow email_approval_workflow \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w email_approval_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one step of the email approval flow .  connect your email provider (SendGrid, SES, Mailgun) for delivery and your business system for decision processing, and the one-click approval workflow stays the same.

- **PrepareWorker** (`ea_prepare`): enrich the approval request with context from your business systems. Attach relevant documents, compute urgency scores
- **ProcessDecisionWorker** (`ea_process_decision`): push the approval decision to downstream systems. Update a database, trigger a fulfillment workflow, or notify stakeholders via Slack
- **SendEmailWorker** (`ea_send_email`): send real emails via SendGrid, AWS SES, or your SMTP server with branded templates and deep-link approve/reject URLs

Connect SendGrid or SES for real email delivery and the one-click approval flow: from inbox to decision processing, continues without modification.

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
email-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/emailapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EmailApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PrepareWorker.java
│       ├── ProcessDecisionWorker.java
│       └── SendEmailWorker.java
└── src/test/java/emailapproval/workers/
    ├── PrepareWorkerTest.java        # 4 tests
    ├── ProcessDecisionWorkerTest.java        # 7 tests
    └── SendEmailWorkerTest.java        # 8 tests
```
