# Slack Interactive Approval in Java Using Conductor :  Request Submission, Block Kit Message with Approve/Reject Buttons, WAIT for Slack Interaction Webhook, and Decision Finalization

A Java Conductor workflow example for Slack-native approvals. submitting a request, posting a Slack Block Kit message to a channel with interactive Approve and Reject buttons (styled with primary/danger), pausing at a WAIT task until someone clicks a button (which triggers Slack's interaction webhook to complete the WAIT via `POST /tasks/{taskId}`), and finalizing the decision. The PostSlackWorker builds the full Block Kit payload (header block, section with requestor and reason in mrkdwn, actions block with styled buttons), so the approver sees a rich, actionable message right in their Slack channel. Uses [Conductor](https://github.

## Approvals Can Be Triggered via Slack Interactive Buttons

Many teams live in Slack, so approval requests should show up there as interactive messages with Approve/Reject buttons. The workflow submits the request, posts a Slack message with Block Kit interactive buttons, and pauses at a WAIT task until someone clicks a button. The button click (via Slack's interaction webhook) completes the WAIT task with the decision.

## The Solution

**You just write the request-submission, Slack-posting, and decision-finalization workers. Conductor handles the durable wait for the Slack button click.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

SubmitWorker captures the request, PostSlackWorker builds the Block Kit message with interactive buttons, and FinalizeWorker processes the decision, the durable wait for the Slack webhook callback is managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `sa_submit` | Receives the approval request with requestor and reason, validates and marks the submission as received |
| **PostSlackWorker** | `sa_post_slack` | Builds a Slack Block Kit payload with a header block ("Approval Request"), a mrkdwn section showing the requestor and reason, and an actions block with styled Approve (primary) and Reject (danger) buttons. in production, POSTs this to Slack's chat.postMessage API |
| *WAIT task* | `slack_response` | Pauses until the Slack interaction webhook fires. when an approver clicks Approve or Reject in the Slack message, your webhook handler completes this WAIT task via `POST /tasks/{taskId}` with the decision | Built-in Conductor WAIT,  no worker needed |
| **FinalizeWorker** | `sa_finalize` | Reads the decision from the completed WAIT task output and finalizes the approval. records the outcome and triggers downstream actions based on approved or rejected |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
sa_submit
    │
    ▼
sa_post_slack
    │
    ▼
slack_response [WAIT]
    │
    ▼
sa_finalize

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
java -jar target/slack-approval-1.0.0.jar

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
java -jar target/slack-approval-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow slack_approval_demo \
  --version 1 \
  --input '{"requestor": "sample-requestor", "reason": "sample-reason", "channel": "email"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w slack_approval_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one step of the Slack approval flow. connect the Slack Block Kit API for message posting and your interaction webhook for button responses, and the Slack-native approval workflow stays the same.

- **FinalizeWorker** (`sa_finalize`): push the approval decision to downstream systems. Update a database, trigger a deployment, or notify the requester via Slack DM
- **PostSlackWorker** (`sa_post_slack`): POST the Block Kit payload to Slack's chat.postMessage API via the Slack SDK, with real channel IDs and bot tokens
- **SubmitWorker** (`sa_submit`): enrich the submission with context from your business systems to include in the Slack message

Plug in a real Slack bot token and channel ID and the interactive approval flow. Block Kit message to button click to finalization. Operates as designed.

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
slack-approval/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/slackapproval/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SlackApprovalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalizeWorker.java
│       ├── PostSlackWorker.java
│       └── SubmitWorker.java
└── src/test/java/slackapproval/workers/
    ├── FinalizeWorkerTest.java        # 7 tests
    ├── PostSlackWorkerTest.java        # 9 tests
    └── SubmitWorkerTest.java        # 4 tests

```
