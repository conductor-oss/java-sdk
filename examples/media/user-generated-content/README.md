# User-Generated Content Pipeline in Java Using Conductor :  Submission, Moderation, Approval, Enrichment, and Publishing

A Java Conductor workflow example that orchestrates a UGC pipeline. receiving user submissions with queue positioning, running automated moderation (spam detection, adult content filtering, toxicity scoring), approving content that passes moderation, enriching with auto-generated metadata (tags, sentiment analysis, readability scores, language detection), and publishing to the platform. Uses [Conductor](https://github.

## Why UGC Pipelines Need Orchestration

Processing user-generated content requires a pipeline that balances speed (users expect fast publication) with safety (you must not publish harmful content). You receive the submission and assign a queue position. You run automated moderation. spam detection, adult content classification, toxicity scoring,  to catch clearly violating content. Content that passes moderation is approved (automatically or by a human moderator, depending on your confidence threshold). Approved content is enriched with auto-generated tags, sentiment analysis, readability scores, and language detection to improve discoverability. Finally, the enriched content is published with a public URL.

Each stage gates the next. you must not enrich or publish unmoderated content. If moderation flags borderline content, you need a different approval path (human review) than content that passes cleanly (auto-approve). Without orchestration, you'd build a monolithic UGC processor that mixes upload handling, ML inference for moderation, NLP enrichment, and database writes,  making it impossible to tune moderation thresholds without risking the enrichment pipeline, add new enrichment types, or audit which moderation step rejected a specific submission.

## How This Workflow Solves It

**You just write the UGC workers. Submission intake, automated moderation, approval, metadata enrichment, and publishing. Conductor handles safety-gated sequencing, ML service retries, and a full audit trail from submission through publication.**

Each UGC stage is an independent worker. submit, moderate, approve, enrich, publish. Conductor sequences them, passes moderation scores and approval flags between stages, retries if an ML service times out, and maintains a complete audit trail from submission through publication for every piece of user content.

### What You Write: Workers

Five workers process user submissions: SubmitWorker queues incoming content, ModerateWorker runs automated safety checks, ApproveWorker gates publication, EnrichWorker adds auto-generated tags and sentiment analysis, and PublishWorker makes content live.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `ugc_approve` | Evaluates approval criteria and computes approval method, approved at |
| **EnrichWorker** | `ugc_enrich` | Enriches the data and computes metadata, auto tags, sentiment, readability |
| **ModerateWorker** | `ugc_moderate` | Moderates the content and computes moderation score, flagged, categories, spam |
| **PublishWorker** | `ugc_publish` | Publishes the content and computes published, publish url, published at |
| **SubmitWorker** | `ugc_submit` | Receives the user-submitted content, records the submission timestamp, and assigns a queue position for moderation review |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
ugc_submit
    │
    ▼
ugc_moderate
    │
    ▼
ugc_approve
    │
    ▼
ugc_enrich
    │
    ▼
ugc_publish

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
java -jar target/user-generated-content-1.0.0.jar

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
java -jar target/user-generated-content-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow user_generated_content_workflow \
  --version 1 \
  --input '{"submissionId": "TEST-001", "userId": "TEST-001", "contentType": "Process this order for customer C-100", "contentBody": "Process this order for customer C-100", "title": "sample-title"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w user_generated_content_workflow -s COMPLETED -c 5

```

## How to Extend

Connect ModerateWorker to your content safety model (Perspective API, OpenAI Moderation), EnrichWorker to your NLP tagging service, and PublishWorker to your content platform. The workflow definition stays exactly the same.

- **SubmitWorker** (`ugc_submit`): accept submissions via your API gateway, assign queue positions from a Redis counter, and store raw content in your object store (S3, GCS) with user metadata
- **ModerateWorker** (`ugc_moderate`): run content through real moderation services: Perspective API for toxicity scoring, AWS Rekognition for adult content detection, Akismet or custom ML models for spam classification
- **ApproveWorker** (`ugc_approve`): auto-approve content below your moderation thresholds, route borderline submissions (e.g., toxicity 0.4-0.7) to a human review queue in your moderation dashboard
- **EnrichWorker** (`ugc_enrich`): generate metadata with NLP services: auto-tags via a topic classifier, sentiment analysis through Google Cloud Natural Language, readability scores (Flesch-Kincaid), and language detection
- **PublishWorker** (`ugc_publish`): write the approved, enriched content to your CMS or content database, assign a public URL, and invalidate CDN caches so the post goes live immediately

Point each worker at your ML moderation service or NLP enrichment API while keeping output fields consistent, and the UGC pipeline works without modification.

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
user-generated-content/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/usergeneratedcontent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserGeneratedContentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveWorker.java
│       ├── EnrichWorker.java
│       ├── ModerateWorker.java
│       ├── PublishWorker.java
│       └── SubmitWorker.java
└── src/test/java/usergeneratedcontent/workers/
    ├── ModerateWorkerTest.java        # 2 tests
    └── SubmitWorkerTest.java        # 2 tests

```
