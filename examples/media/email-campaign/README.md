# Email Campaign Pipeline in Java Using Conductor :  Audience Segmentation, Personalization, Sending, Engagement Tracking, and Performance Analysis

A Java Conductor workflow example that orchestrates an email marketing campaign .  segmenting subscribers by behavior and demographics with suppression list filtering, personalizing email content with merge fields and A/B variant creation, sending the campaign in batches with bounce tracking, monitoring engagement metrics (open rate, click rate, unsubscribes), and analyzing results against industry benchmarks. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Email Campaigns Need Orchestration

Running an email campaign involves a strict pipeline where sending before segmentation or tracking before sending produces incorrect results. You segment your subscriber list into targeted cohorts, suppress unsubscribed users, and count recipients. You personalize the email template with merge fields (first name, purchase history, recommended products) and create A/B variants. You send the campaign in batches, handling bounces in real time. You track engagement .  open rates, click-through rates, unique opens, unique clicks, unsubscribe rates. Finally, you analyze the results against industry benchmarks to measure campaign effectiveness.

If segmentation produces an empty list, you must not send. If the send fails partway through, you need to know which batch completed so you can resume without double-sending. Without orchestration, you'd build a monolithic email system that mixes list management, template rendering, SMTP delivery, event tracking, and analytics .  making it impossible to swap your ESP, test personalization independently, or trace which segment received which variant.

## How This Workflow Solves It

**You just write the campaign workers. Audience segmentation, content personalization, batch sending, engagement tracking, and results analysis. Conductor handles batch send sequencing, ESP retries, and a full audit trail from segmentation through delivery metrics.**

Each campaign stage is an independent worker .  segment audience, personalize, send, track engagement, analyze results. Conductor sequences them, passes recipient counts and send IDs between stages, retries if an ESP API times out, and provides a complete audit trail from segmentation through delivery metrics.

### What You Write: Workers

Five workers power the campaign pipeline: SegmentAudienceWorker builds targeted cohorts, PersonalizeWorker merges recipient data into templates, SendCampaignWorker delivers in batches, TrackEngagementWorker monitors opens and clicks, and AnalyzeResultsWorker benchmarks performance.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `eml_analyze_results` | Analyzes results |
| **PersonalizeWorker** | `eml_personalize` | Personalizes the content and computes personalized count, variants created, merge fields used |
| **SegmentAudienceWorker** | `eml_segment_audience` | Segments the audience |
| **SendCampaignWorker** | `eml_send_campaign` | Sends the campaign |
| **TrackEngagementWorker** | `eml_track_engagement` | Tracks the engagement |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
eml_segment_audience
    │
    ▼
eml_personalize
    │
    ▼
eml_send_campaign
    │
    ▼
eml_track_engagement
    │
    ▼
eml_analyze_results
```

## Example Output

```
=== Example 524: Email Campaig ===

Step 1: Registering task definitions...
  Registered: eml_segment_audience, eml_personalize, eml_send_campaign, eml_track_engagement, eml_analyze_results

Step 2: Registering workflow 'email_campaign_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] Processing
  [personalize] Processing
  [segment] Processing
  [send] Processing
  [track] Processing

  Status: COMPLETED
  Output: {industryBenchmark=..., clickRate=..., personalizedCount=..., variantsCreated=...}

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
java -jar target/email-campaign-1.0.0.jar
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
java -jar target/email-campaign-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow email_campaign_workflow \
  --version 1 \
  --input '{"campaignId": "CAMP-524-001", "CAMP-524-001": "subject", "subject": "Unlock the Power of Workflow Automation", "Unlock the Power of Workflow Automation": "templateId", "templateId": "TPL-PROMO-03", "TPL-PROMO-03": "listId", "listId": "LIST-MAIN-2026", "LIST-MAIN-2026": "sample-LIST-MAIN-2026"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w email_campaign_workflow -s COMPLETED -c 5
```

## How to Extend

Connect SegmentAudienceWorker to your subscriber database, SendCampaignWorker to your ESP (SendGrid, Mailchimp, SES), and TrackEngagementWorker to your email analytics platform. The workflow definition stays exactly the same.

- **SegmentAudienceWorker** (`eml_segment_audience`): query your ESP or CRM (Mailchimp, HubSpot, Salesforce) to build targeted segments with suppression list filtering and recipient counts
- **PersonalizeWorker** (`eml_personalize`): render personalized email templates with merge fields from your user database, create A/B subject line variants, and preview for QA
- **SendCampaignWorker** (`eml_send_campaign`): send the campaign via your ESP API (SendGrid, Amazon SES, Mailgun) in throttled batches, tracking send counts and bounces in real time
- **TrackEngagementWorker** (`eml_track_engagement`): poll your ESP's event webhook or API for open, click, and unsubscribe events, computing engagement rates
- **AnalyzeResultsWorker** (`eml_analyze_results`): compare campaign metrics against industry benchmarks, compute ROI, and generate performance reports for the marketing team

Connect each worker to your ESP or analytics platform while preserving output fields, and the campaign pipeline stays the same.

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
email-campaign/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/emailcampaign/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EmailCampaignExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeResultsWorker.java
│       ├── PersonalizeWorker.java
│       ├── SegmentAudienceWorker.java
│       ├── SendCampaignWorker.java
│       └── TrackEngagementWorker.java
└── src/test/java/emailcampaign/workers/
    ├── PersonalizeWorkerTest.java        # 2 tests
    └── SegmentAudienceWorkerTest.java        # 2 tests
```
