# Content Review Pipeline in Java Using Conductor :  AI Draft Generation, Human Review via WAIT, and Publishing

A Java Conductor workflow example for AI-assisted content creation. an AI model generates a draft based on a topic and target audience, the workflow pauses at a WAIT task for a human editor to review, edit, and approve or reject the draft, and then the approved content is published. Demonstrates the AI-generates-human-reviews pattern where automation handles the initial draft and a person ensures quality before publication. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need a content pipeline where AI generates first drafts and humans review them before publishing. An AI model produces content for a given topic and audience. a blog post, marketing copy, product description, or documentation page. The draft includes the generated text, word count, and model used. A human editor must review the AI output for accuracy, tone, brand voice, and factual correctness. The editor may approve it as-is, edit it and approve the revised version, or reject it entirely. Only approved content should be published. Without a review step, AI hallucinations, off-brand tone, or factual errors reach your audience.

Without orchestration, you'd call the AI API, store the draft in a database, email the editor a link to review it, poll for their response, and then call the CMS publish API. If the AI API times out, you'd need retry logic. If the system crashes after the editor approves but before publishing, the reviewed content never goes live. There is no single view showing which drafts are awaiting review, how long reviews take, or the approval rate of AI-generated content.

## The Solution

**You just write the AI draft-generation and publishing workers. Conductor handles the durable pause for editorial review and the content lifecycle tracking.**

The WAIT task is the key pattern here. After the AI generates the draft, the workflow pauses at the WAIT task. Conductor holds the draft content, word count, and model metadata until a human editor completes the review with an approved/rejected decision and optionally edited content. The publish worker only fires after the review is complete. Conductor takes care of holding the draft durably while the editor reviews (minutes, hours, or days), passing the editor's approved flag and edited content to the publish worker, tracking the complete content lifecycle from AI generation through review to publication, and providing metrics on review turnaround time and approval rates. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CrpAiDraftWorker generates content for a topic and audience, and CrpPublishWorker posts approved copy to the CMS, the editorial review pause between them is handled entirely by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **CrpAiDraftWorker** | `crp_ai_draft` | Generates an AI content draft for the given topic and target audience, returning the draft text, word count, and model used |
| *WAIT task* | `crp_human_review` | Pauses the workflow with the AI draft content until a human editor submits their review. approved/rejected flag and optionally edited content,  via `POST /tasks/{taskId}` | Built-in Conductor WAIT,  no worker needed |
| **CrpPublishWorker** | `crp_publish` | Publishes the approved content to the CMS, returning the published URL; skips publishing if the review was rejected |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
crp_ai_draft
    │
    ▼
crp_human_review [WAIT]
    │
    ▼
crp_publish

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
java -jar target/content-review-pipeline-1.0.0.jar

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
java -jar target/content-review-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_review_pipeline \
  --version 1 \
  --input '{"topic": "microservices best practices", "audience": "sample-audience"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_review_pipeline -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the content pipeline. connect your LLM (Claude, GPT-4) for draft generation and your CMS (WordPress, Contentful, Ghost) for publishing, and the review workflow stays the same.

- **CrpAiDraftWorker** → call a real AI API (OpenAI GPT-4, Anthropic Claude, Cohere) with topic, audience, tone, and length parameters to generate production-quality drafts
- **WAIT task** → complete it from your content editor UI with `{ "approved": true, "editedContent": "..." }`. the editor can approve as-is or provide a revised version
- **CrpPublishWorker** → publish to your CMS (WordPress REST API, Contentful Management API, Sanity, Ghost) with SEO metadata, featured images, and category tags
- Add a **ComplianceCheckWorker** between AI draft and human review to run automated brand voice, plagiarism, and legal compliance checks before the editor sees it
- Add a SWITCH after the review to route rejections back to the AI draft worker with the editor's feedback for a revised draft
- Add a **SocialWorker** after publishing to cross-post to social media channels (Twitter, LinkedIn, Facebook) automatically

Swap in your AI model and CMS APIs and the draft-review-publish content lifecycle keeps running as defined.

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
content-review-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentreviewpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentReviewPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CrpAiDraftWorker.java
│       └── CrpPublishWorker.java
└── src/test/java/contentreviewpipeline/workers/
    ├── CrpAiDraftWorkerTest.java        # 8 tests
    └── CrpPublishWorkerTest.java        # 8 tests

```
