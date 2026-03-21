# Drip Campaign in Java with Conductor :  Enroll, Send, Track, and Graduate Contacts Through an Email Series

A Java Conductor workflow that runs a drip email campaign for a contact. enrolling them in a campaign, sending a series of timed emails, tracking engagement (opens, clicks, replies), and graduating the contact when the series completes. Given a `contactId`, `campaignId`, and `email`, the pipeline produces an enrollment ID, email send count, and graduation status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step drip campaign lifecycle.

## Nurturing Leads with Automated Email Sequences

Drip campaigns send a sequence of emails over time to nurture a lead toward conversion. Each contact needs to be enrolled, receive the right emails in the right order, have their engagement tracked (did they open? click? reply?), and eventually graduate from the campaign. either because they converted or completed the series. Managing this manually across thousands of contacts is impossible.

This workflow models the drip campaign lifecycle for a single contact. The enroll worker registers the contact in the campaign and creates an enrollment record. The send worker delivers the email series to the contact's address. The engagement tracker measures how the contact interacted with the emails and produces an engagement score. The graduation worker evaluates the engagement score and decides whether the contact has graduated (converted or completed the series). Each step depends on the previous one. you cannot track engagement before sending, and you cannot graduate before measuring engagement.

## The Solution

**You just write the enrollment, email-sending, engagement-tracking, and graduation workers. Conductor handles the drip sequence and lifecycle tracking.**

Four workers handle the drip lifecycle. enrollment, email sending, engagement tracking, and graduation. The enroller creates a campaign enrollment record. The sender delivers the email series and reports the count. The tracker monitors opens, clicks, and replies to compute an engagement score. The graduation worker uses the score to determine if the contact should graduate. Conductor sequences the steps and routes enrollment IDs and engagement scores between them automatically.

### What You Write: Workers

EnrollWorker registers the contact, SendSeriesWorker delivers timed emails, TrackEngagementWorker measures opens and clicks, and GraduateWorker decides if the contact moves to sales or gets recycled.

| Worker | Task | What It Does |
|---|---|---|
| **EnrollWorker** | `drp_enroll` | Registers the contact in the drip campaign and creates an enrollment record with a sequence name. |
| **GraduateWorker** | `drp_graduate` | Evaluates the engagement score and decides if the contact graduates to sales or gets recycled. |
| **SendSeriesWorker** | `drp_send_series` | Delivers the sequence of timed drip emails to the contact's email address. |
| **TrackEngagementWorker** | `drp_track_engagement` | Measures email engagement: open rates, click-through rates, and replies across the series. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
drp_enroll
    │
    ▼
drp_send_series
    │
    ▼
drp_track_engagement
    │
    ▼
drp_graduate

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
java -jar target/drip-campaign-1.0.0.jar

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
java -jar target/drip-campaign-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow drp_drip_campaign \
  --version 1 \
  --input '{"contactId": "TEST-001", "campaignId": "TEST-001", "email": "user@example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w drp_drip_campaign -s COMPLETED -c 5

```

## How to Extend

Each worker handles one drip phase. connect your email platform (SendGrid, Mailchimp, Customer.io) for delivery and your CRM (Salesforce, HubSpot) for engagement tracking, and the drip-campaign workflow stays the same.

- **EnrollWorker** (`drp_enroll`): integrate with your CRM (Salesforce, HubSpot) to create real campaign enrollment records
- **GraduateWorker** (`drp_graduate`): connect to your CRM to update lead status and trigger sales handoff workflows
- **SendSeriesWorker** (`drp_send_series`): swap in a transactional email provider (SendGrid, Postmark, SES) for real email delivery

Integrate your ESP and CRM and the enrollment-send-track-graduate drip lifecycle keeps functioning as defined.

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
drip-campaign/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dripcampaign/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DripCampaignExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EnrollWorker.java
│       ├── GraduateWorker.java
│       ├── SendSeriesWorker.java
│       └── TrackEngagementWorker.java
└── src/test/java/dripcampaign/workers/
    ├── EnrollWorkerTest.java        # 2 tests
    ├── GraduateWorkerTest.java        # 3 tests
    ├── SendSeriesWorkerTest.java        # 2 tests
    └── TrackEngagementWorkerTest.java        # 2 tests

```
