# Fundraising Campaign in Java with Conductor

A Java Conductor workflow example demonstrating Fundraising Campaign. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

Your nonprofit is launching an end-of-year fundraising campaign with a $100,000 goal. The development team needs to plan the campaign by selecting outreach channels (email, social, direct mail), launch it on the target date, track donations against the goal throughout the campaign, close the campaign at the end date, and produce a final report showing total raised, donor count, and whether the goal was met. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the campaign planning, donor outreach, donation tracking, and results reporting logic. Conductor handles outreach retries, pledge tracking, and campaign performance audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Campaign setup, outreach, pledge tracking, and results reporting workers each handle one phase of the fundraising drive.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `frc_close` | Closes the campaign on the end date and records the final close timestamp |
| **LaunchWorker** | `frc_launch` | Launches the campaign and records the launch date |
| **PlanWorker** | `frc_plan` | Plans the campaign by setting the name, goal amount, and outreach channels (email, social, direct mail) |
| **ReportWorker** | `frc_report` | Generates the final campaign report with total raised, goal-met status, and campaign ID |
| **TrackWorker** | `frc_track` | Tracks donations against the goal, computing total raised, donor count, and average donation |

Workers implement nonprofit operations. donor processing, campaign management, reporting,  with realistic outputs. Replace with real CRM and payment integrations and the workflow stays the same.

### The Workflow

```
frc_plan
    │
    ▼
frc_launch
    │
    ▼
frc_track
    │
    ▼
frc_close
    │
    ▼
frc_report

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
java -jar target/fundraising-campaign-1.0.0.jar

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
java -jar target/fundraising-campaign-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fundraising_campaign_754 \
  --version 1 \
  --input '{"campaignName": "test", "goalAmount": 100, "endDate": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fundraising_campaign_754 -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real fundraising tools. your CRM for donor segmentation, your email platform for campaign outreach, your payment processor for donation collection, and the workflow runs identically in production.

- **PlanWorker** (`frc_plan`): create the campaign in Salesforce NPSP, Bloomerang, or DonorPerfect and configure outreach sequences in your email platform (Mailchimp, SendGrid)
- **LaunchWorker** (`frc_launch`): activate email drip campaigns in Mailchimp, schedule social media posts via Buffer, and update the campaign status in your CRM
- **TrackWorker** (`frc_track`): query donation data from your payment processor (Stripe, PayPal) and CRM to compute real-time progress against the goal
- **CloseWorker** (`frc_close`): deactivate donation pages, stop scheduled outreach in your email platform, and mark the campaign as closed in your CRM
- **ReportWorker** (`frc_report`): aggregate campaign data from your CRM and generate a PDF report with charts using a reporting engine, uploading it to Salesforce Files or your document store

Update outreach channels or pledge tracking tools and the campaign pipeline adapts transparently.

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
fundraising-campaign/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/fundraisingcampaign/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FundraisingCampaignExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CloseWorker.java
│       ├── LaunchWorker.java
│       ├── PlanWorker.java
│       ├── ReportWorker.java
│       └── TrackWorker.java
└── src/test/java/fundraisingcampaign/workers/
    ├── PlanWorkerTest.java        # 1 tests
    └── ReportWorkerTest.java        # 1 tests

```
