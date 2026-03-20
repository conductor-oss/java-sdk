# Content Publishing Pipeline in Java Using Conductor :  Drafting, Editorial Review, Approval, Formatting, Publishing, and Multi-Channel Distribution

A Java Conductor workflow example that orchestrates a content publishing pipeline .  creating drafts with word counts and SEO slugs, routing through editorial review with quality scores, obtaining editorial approval, formatting content with SEO metadata for multiple output formats, publishing to the website with CDN cache invalidation, and distributing to social and newsletter channels on a schedule. Uses [Conductor](https://github.## Why Content Publishing Needs Orchestration

Publishing content involves a strict editorial pipeline where each stage gates the next. A draft must be reviewed before it can be approved. Approved content must be formatted with SEO metadata before publishing. Publishing must invalidate CDN caches. Distribution to social channels and newsletters must happen only after the content is live. If a reviewer rejects the draft, it should loop back to editing .  not proceed to publishing.

Each stage produces outputs the next stage needs: the draft version number, the reviewer's score and notes, the approver's sign-off, the formatted URL and metadata. Without orchestration, you'd build a monolithic CMS integration that mixes content creation, editorial workflows, SEO tooling, CDN management, and social scheduling .  making it impossible to add a new distribution channel, change the approval process, or trace why a specific article was published with incorrect metadata.

## How This Workflow Solves It

**You just write the publishing workers. Draft creation, editorial review, approval, formatting, publishing, and distribution. Conductor handles editorial gating, CDN invalidation retries, and complete records tracking every draft through distribution.**

Each publishing stage is an independent worker .  draft, review, approve, format, publish, distribute. Conductor sequences the editorial pipeline, passes draft versions and review scores between stages, retries if a CDN invalidation times out, and provides a complete audit trail of every editorial decision from draft to distribution.

### What You Write: Workers

Six workers manage the editorial pipeline: DraftContentWorker creates articles, ReviewContentWorker assigns quality scores, ApproveContentWorker gates publication, FormatContentWorker adds SEO metadata, PublishContentWorker pushes to the site, and DistributeContentWorker syndicates to channels.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveContentWorker** | `pub_approve_content` | Approves content based on review score. |
| **DistributeContentWorker** | `pub_distribute_content` | Distributes published content to channels. |
| **DraftContentWorker** | `pub_draft_content` | Creates a draft of the content. |
| **FormatContentWorker** | `pub_format_content` | Formats content for distribution. |
| **PublishContentWorker** | `pub_publish_content` | Publishes content to production. |
| **ReviewContentWorker** | `pub_review_content` | Reviews draft content and assigns a score. |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
pub_draft_content
    │
    ▼
pub_review_content
    │
    ▼
pub_approve_content
    │
    ▼
pub_format_content
    │
    ▼
pub_publish_content
    │
    ▼
pub_distribute_content
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
java -jar target/content-publishing-1.0.0.jar
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
java -jar target/content-publishing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_publishing_workflow \
  --version 1 \
  --input '{"contentId": "TEST-001", "authorId": "TEST-001", "contentType": "test-value", "title": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_publishing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect DraftContentWorker to your CMS (WordPress, Contentful), ReviewContentWorker to your editorial workflow, and DistributeContentWorker to your social and newsletter channels. The workflow definition stays exactly the same.

- **DraftContentWorker** (`pub_draft_content`): create drafts in your CMS (WordPress, Contentful, Strapi) with version tracking, word counts, estimated read time, and SEO slugs
- **ReviewContentWorker** (`pub_review_content`): assign content to reviewers in your editorial tool, collect review scores and editorial notes
- **ApproveContentWorker** (`pub_approve_content`): implement approval workflows with role-based sign-off via your CMS or project management tool (Asana, Monday)
- **FormatContentWorker** (`pub_format_content`): generate SEO metadata (titles, descriptions, structured data), format for multiple output channels (web, AMP, email), and apply design templates
- **PublishContentWorker** (`pub_publish_content`): publish to your website via CMS API, invalidate CDN caches (CloudFront, Fastly), and verify the live URL returns 200
- **DistributeContentWorker** (`pub_distribute_content`): schedule social posts (Buffer, Hootsuite), trigger newsletter sends (Mailchimp, SendGrid), and push to RSS feeds

Connect each worker to your CMS or CDN while preserving its output fields, and the editorial flow requires no changes.

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
content-publishing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentpublishing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentPublishingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveContentWorker.java
│       ├── DistributeContentWorker.java
│       ├── DraftContentWorker.java
│       ├── FormatContentWorker.java
│       ├── PublishContentWorker.java
│       └── ReviewContentWorker.java
└── src/test/java/contentpublishing/workers/
    ├── ApproveContentWorkerTest.java        # 8 tests
    ├── DistributeContentWorkerTest.java        # 8 tests
    ├── DraftContentWorkerTest.java        # 8 tests
    ├── FormatContentWorkerTest.java        # 8 tests
    ├── PublishContentWorkerTest.java        # 8 tests
    └── ReviewContentWorkerTest.java        # 8 tests
```
