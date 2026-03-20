# Content Moderation Pipeline in Java Using Conductor :  Auto-Check, Toxicity Scoring, Human Review, and Policy Enforcement

A Java Conductor workflow example that orchestrates content moderation .  submitting user content for review, running automated toxicity and policy violation checks with confidence scores, routing via SWITCH to approve safe content, escalate flagged content to human reviewers, or immediately block clearly violating content, and finalizing the decision with an audit log. Uses [Conductor](https://github.## Why Content Moderation Needs Orchestration

Moderating user content requires a decision pipeline with multiple possible outcomes. You receive a submission and queue it. You run automated checks .  toxicity scoring, policy violation detection, confidence assessment. Based on the auto-check results, you route to three different paths: safe content (high confidence, low toxicity) is approved automatically; flagged content (medium confidence, potential violations) goes to a human moderator for manual review; clearly violating content (high toxicity, obvious violations) is blocked immediately with user notification and appeal options.

This is exactly the kind of conditional routing that becomes unmanageable in a monolithic system. Each moderation decision involves different downstream actions .  approval publishes the content, human review assigns a moderator and waits for their verdict, blocking notifies the user and records a block ID. Without orchestration, you'd build a monolithic moderation engine with nested if/else chains handling every combination of toxicity scores and confidence levels, mixing ML inference, queue management, human task assignment, and notification logic in one class.

## How This Workflow Solves It

**You just write the moderation workers. Content submission, auto-check scoring, human review, blocking, and finalization. Conductor handles three-way SWITCH routing, ML service retries, and a tamper-evident audit log for every moderation decision.**

Each moderation concern is an independent worker .  submit content, auto-check, approve, human review, block, finalize. Conductor sequences the initial submission and auto-check, then uses a SWITCH task to route to the correct action based on the moderation verdict. Every decision is recorded in the audit log, and adding a new moderation outcome (e.g., "restrict visibility") means adding a new SWITCH case and worker.

### What You Write: Workers

Six workers cover the moderation lifecycle: SubmitContentWorker queues submissions, AutoCheckWorker scores toxicity, ApproveSafeWorker passes clean content, HumanReviewWorker escalates borderline cases, BlockContentWorker rejects violations, and FinalizeWorker records the audit log.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveSafeWorker** | `mod_approve_safe` | Approves the safe |
| **AutoCheckWorker** | `mod_auto_check` | Handles auto check |
| **BlockContentWorker** | `mod_block_content` | Handles block content |
| **FinalizeWorker** | `mod_finalize` | Finalizes the moderation decision by recording the verdict as the final status and generating an audit log entry |
| **HumanReviewWorker** | `mod_human_review` | Handles human review |
| **SubmitContentWorker** | `mod_submit_content` | Handles submit content |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
mod_submit_content
    │
    ▼
mod_auto_check
    │
    ▼
SWITCH (mod_switch_ref)
    ├── safe: mod_approve_safe
    ├── flag: mod_human_review
    ├── block: mod_block_content
    │
    ▼
mod_finalize
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
java -jar target/content-moderation-1.0.0.jar
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
java -jar target/content-moderation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_moderation_workflow \
  --version 1 \
  --input '{"contentId": "TEST-001", "contentType": "test-value", "userId": "TEST-001", "contentText": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_moderation_workflow -s COMPLETED -c 5
```

## How to Extend

Connect AutoCheckWorker to your ML toxicity model (Perspective API, AWS Rekognition), HumanReviewWorker to your moderator queue, and BlockContentWorker to your content enforcement system. The workflow definition stays exactly the same.

- **SubmitContentWorker** (`mod_submit_content`): receive content from your submission API, validate the payload, and enqueue for moderation
- **AutoCheckWorker** (`mod_auto_check`): call your ML moderation service (OpenAI Moderation API, Perspective API, AWS Rekognition) to compute toxicity scores, flag reasons, and confidence levels
- **ApproveSafeWorker** (`mod_approve_safe`): publish approved content to your platform and update the content status in your CMS
- **HumanReviewWorker** (`mod_human_review`): assign the content to a human moderator queue (Zendesk, custom moderation tool) and wait for their verdict with review notes
- **BlockContentWorker** (`mod_block_content`): remove the content, notify the user with violation details and appeal instructions, and record the block in your trust and safety system
- **FinalizeWorker** (`mod_finalize`): write the final moderation decision to your audit log for compliance and policy review

Swap any worker for a real ML moderation service or review queue while keeping the output contract, and the SWITCH-based routing continues unchanged.

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
content-moderation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentmoderation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentModerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveSafeWorker.java
│       ├── AutoCheckWorker.java
│       ├── BlockContentWorker.java
│       ├── FinalizeWorker.java
│       ├── HumanReviewWorker.java
│       └── SubmitContentWorker.java
└── src/test/java/contentmoderation/workers/
    ├── AutoCheckWorkerTest.java        # 2 tests
    └── SubmitContentWorkerTest.java        # 2 tests
```
