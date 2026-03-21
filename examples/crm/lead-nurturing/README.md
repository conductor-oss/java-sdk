# Lead Nurturing in Java with Conductor :  Segment, Personalize, Send, and Track Nurture Campaigns

A Java Conductor workflow that nurtures a lead through a personalized outreach sequence. segmenting the lead by stage and interests, personalizing the content for their segment, sending the tailored message, and tracking engagement. Given a `leadId`, `leadStage`, and `interests`, the pipeline produces a segment assignment, personalized content, delivery status, and engagement metrics. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step nurture pipeline.

## Moving Leads Through the Funnel with the Right Message at the Right Time

Not every lead is ready to buy. A lead in the awareness stage needs educational content, while a lead in the decision stage needs case studies and pricing. Sending the same generic email to everyone wastes opportunities. Effective nurturing requires segmenting leads by where they are in the funnel and what they care about, then crafting personalized content for each segment, delivering it, and measuring whether it moved them closer to conversion.

This workflow runs one nurture cycle for a lead. The segmenter classifies the lead based on their stage (awareness, consideration, decision) and interests. The personalizer creates content tailored to that segment. educational blog posts for awareness leads, product comparisons for consideration leads, ROI calculators for decision leads. The sender delivers the personalized content. The tracker measures engagement (open, click, reply) to inform the next nurture cycle.

## The Solution

**You just write the segmentation, personalization, sending, and tracking workers. Conductor handles the nurture pipeline and engagement data flow.**

Four workers handle the nurture cycle. segmentation, personalization, sending, and tracking. The segmenter classifies the lead by funnel stage and interests. The personalizer tailors content for the assigned segment. The sender delivers the message. The tracker measures engagement. Conductor sequences the four steps and passes segment data, personalized content, and delivery confirmations between them automatically.

### What You Write: Workers

SegmentWorker classifies leads by funnel stage, PersonalizeWorker tailors content for each segment, SendWorker delivers the message, and TrackWorker measures engagement to inform the next cycle.

| Worker | Task | What It Does |
|---|---|---|
| **PersonalizeWorker** | `nur_personalize` | Creates tailored content for the lead's segment. educational posts, product comparisons, or ROI calculators. |
| **SegmentWorker** | `nur_segment` | Classifies the lead by funnel stage (awareness, consideration, decision) and interest areas. |
| **SendWorker** | `nur_send` | Delivers the personalized nurture content to the lead via email. |
| **TrackWorker** | `nur_track` | Measures engagement (opens, clicks, replies) on the delivered content. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
nur_segment
    │
    ▼
nur_personalize
    │
    ▼
nur_send
    │
    ▼
nur_track

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
java -jar target/lead-nurturing-1.0.0.jar

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
java -jar target/lead-nurturing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nur_lead_nurturing \
  --version 1 \
  --input '{"leadId": "TEST-001", "leadStage": "sample-leadStage", "interests": "sample-interests"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nur_lead_nurturing -s COMPLETED -c 5

```

## How to Extend

Each worker handles one nurture step. connect your marketing automation platform (HubSpot, Marketo, Pardot) for content delivery and your CRM for lead stage tracking, and the nurture workflow stays the same.

- **PersonalizeWorker** (`nur_personalize`): use an LLM to generate personalized content based on lead profile and segment
- **SegmentWorker** (`nur_segment`): integrate with your CRM (Salesforce, HubSpot) to pull real lead data for segmentation
- **SendWorker** (`nur_send`): connect to email platforms (SendGrid, Mailchimp) or multi-channel tools (Braze, Iterable) for delivery

Integrate your marketing automation platform and the segment-personalize-send-track nurture cycle runs unchanged.

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
lead-nurturing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/leadnurturing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LeadNurturingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PersonalizeWorker.java
│       ├── SegmentWorker.java
│       ├── SendWorker.java
│       └── TrackWorker.java
└── src/test/java/leadnurturing/workers/
    ├── PersonalizeWorkerTest.java        # 3 tests
    ├── SegmentWorkerTest.java        # 4 tests
    ├── SendWorkerTest.java        # 2 tests
    └── TrackWorkerTest.java        # 3 tests

```
