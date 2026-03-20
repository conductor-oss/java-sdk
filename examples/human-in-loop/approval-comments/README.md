# Approval with Comments and Attachments in Java Using Conductor :  Document Preparation, Human Review via WAIT, and Feedback Application

Human-in-the-loop approval with rich feedback .  prepares a document for review, pauses the workflow with a WAIT task until a human reviewer submits their decision along with comments, attachments, ratings, and tags, then applies that feedback to the document. Uses [Conductor](https://github.## The Problem

You need approvals that go beyond a simple approve/reject button. A reviewer needs to provide structured feedback .  a decision (approve, reject, revise), free-text comments explaining their reasoning, file attachments (annotated screenshots, supporting documents, redlined versions), a quality rating, and classification tags. The workflow must pause and wait for this human input, then pass every piece of that feedback to downstream processing. Without a WAIT mechanism, you'd poll a database or queue for the reviewer's response, building your own timeout and notification logic.

Without orchestration, you'd build a custom review portal backed by a polling loop .  the system prepares the document, writes a review request to a database, and then polls every few seconds for a response. When the reviewer finally submits their decision, comments, and attachments, you'd manually update the workflow state. If the process crashes while waiting for the review, the request is lost. If the reviewer attaches files, you'd need to pass attachment metadata through your own state management. There is no built-in timeout, no audit trail of when the review was requested versus completed, and no visibility into how long reviews are taking.

## The Solution

**You just write the document-preparation and feedback-application workers. Conductor handles the durable pause for rich reviewer input.**

The WAIT task is the key pattern here. After preparing the document, the workflow pauses at the WAIT task. Conductor holds the workflow state durably until an external signal (REST API call or SDK call) completes the task with the reviewer's decision, comments, attachments, rating, and tags. The apply-feedback worker then receives all of that structured input. Conductor takes care of holding the workflow state for hours or days while awaiting review, accepting the reviewer's rich feedback (decision, comments, attachments, rating, tags) via API, passing every field from the WAIT output into the feedback worker, and providing a complete timeline showing when the document was prepared, when the review was completed, and how long it took. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

PrepareDocWorker formats the document for review, and ApplyFeedbackWorker processes the reviewer's comments, attachments, and ratings. Neither manages the durable pause between them.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareDocWorker** | `ac_prepare_doc` | Prepares the document for review .  formats it, generates a preview, and signals readiness so the WAIT task can accept reviewer input |
| *WAIT task* | `review_with_comments` | Pauses the workflow until a reviewer submits their decision, comments, attachments, rating, and tags via the Conductor REST API | Built-in Conductor WAIT .  no worker needed |
| **ApplyFeedbackWorker** | `ac_apply_feedback` | Applies the reviewer's structured feedback .  updates the document status based on the decision, stores comments, processes attachments, and records the rating and tags |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
ac_prepare_doc
    │
    ▼
review_with_comments [WAIT]
    │
    ▼
ac_apply_feedback
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
java -jar target/approval-comments-1.0.0.jar
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
java -jar target/approval-comments-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow approval_comments_demo \
  --version 1 \
  --input '{"documentId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w approval_comments_demo -s COMPLETED -c 5
```

## How to Extend

Each worker handles one side of the review cycle .  connect your document management system (SharePoint, Google Docs, Confluence) for preparation and your feedback tracking system for applying decisions, and the review workflow stays the same.

- **PrepareDocWorker** → fetch the real document from SharePoint, Google Docs, or your CMS, generate a review-ready preview, and notify the reviewer via email or Slack that a review is waiting
- **WAIT task** → complete it from your review UI by calling `POST /tasks/{taskId}` with the reviewer's decision, comments, attachments (as URLs or base64), rating (1-5), and classification tags
- **ApplyFeedbackWorker** → update the document status in your DMS, store reviewer comments as annotations, move attachments to the document's history, and route approved documents to the next stage
- Add a **NotifyWorker** to send the document author a notification with the reviewer's decision and comments when the review is complete
- Add a SWITCH after apply-feedback to route "revise" decisions back to the author and "reject" decisions to an archive workflow

Swap in a real DMS for document preparation and the rich feedback pipeline: comments, attachments, ratings, still flows through seamlessly.

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
approval-comments/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/approvalcomments/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApprovalCommentsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyFeedbackWorker.java
│       └── PrepareDocWorker.java
└── src/test/java/approvalcomments/workers/
    ├── ApplyFeedbackWorkerTest.java        # 10 tests
    └── PrepareDocWorkerTest.java        # 4 tests
```
