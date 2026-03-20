# Advertising Campaign Pipeline in Java Using Conductor :  Creative Setup, Audience Targeting, Bid Strategy, Ad Serving, and Performance Reporting

A Java Conductor workflow example that orchestrates a digital advertising campaign lifecycle .  creating ad creatives in multiple formats (banner, video, native), defining target audiences by demographics and interest segments, configuring bid strategies (target CPA, daily budgets, max bids), serving ads and tracking impressions/clicks/conversions, and generating campaign performance reports. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Ad Campaign Management Needs Orchestration

Launching a digital ad campaign involves a strict sequence where each step depends on the previous one. You create the campaign creative .  specifying ad formats (banner 300x250, video pre-roll, native cards) and associating creative assets. You define the target audience ,  selecting interest segments (tech professionals), demographic filters (age, location), and reaching an estimated audience of 2.5 million. You configure the bid strategy ,  target CPA, daily budget derived from the total campaign budget, and maximum bid caps. You activate ad serving and collect performance data: 850K impressions, 12.7K clicks, 425 conversions, $8,500 spend. Finally, you generate a campaign report summarizing ROI and delivery metrics.

If audience targeting fails, you must not start serving ads to an undefined audience. If bid configuration returns an error, you cannot proceed to ad serving with uncapped spend. Without orchestration, you'd build a monolithic ad platform integration that mixes creative management, audience APIs, bidding logic, and reporting .  making it impossible to swap your DSP, test bid strategies independently, or audit which targeting parameters drove which performance outcomes.

## How This Workflow Solves It

**You just write the campaign workers. Creative setup, audience targeting, bid configuration, ad serving, and performance reporting. Conductor handles creative-to-report sequencing, ad platform retries, and complete records linking targeting parameters to delivery metrics.**

Each campaign stage is an independent worker .  create campaign, target audience, set bids, serve ads, generate report. Conductor sequences them, passes creative IDs and audience segments between steps, retries if an ad platform API times out, and maintains a complete audit trail linking every campaign configuration to its delivery metrics.

### What You Write: Workers

Five workers orchestrate the ad campaign: CreateCampaignWorker defines creatives, TargetAudienceWorker segments viewers, SetBidsWorker configures bidding strategy, ServeAdsWorker tracks impressions and clicks, and GenerateReportWorker summarizes ROI.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCampaignWorker** | `adv_create_campaign` | Creates the campaign |
| **GenerateReportWorker** | `adv_generate_report` | Generates the report |
| **ServeAdsWorker** | `adv_serve_ads` | Handles serve ads |
| **SetBidsWorker** | `adv_set_bids` | Sets bids |
| **TargetAudienceWorker** | `adv_target_audience` | Targets the audience |

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
adv_create_campaign
    │
    ▼
adv_target_audience
    │
    ▼
adv_set_bids
    │
    ▼
adv_serve_ads
    │
    ▼
adv_generate_report
```

## Example Output

```
=== Example 530: Advertising Workflow ===

Step 1: Registering task definitions...
  Registered: adv_create_campaign, adv_target_audience, adv_set_bids, adv_serve_ads, adv_generate_report

Step 2: Registering workflow 'advertising_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [create] Processing
  [report] Processing
  [serve] Processing
  [bid] Processing
  [target] Processing

  Status: COMPLETED
  Output: {creativeId=..., adFormats=..., createdAt=..., reportUrl=...}

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
java -jar target/advertising-workflow-1.0.0.jar
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
java -jar target/advertising-workflow-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow advertising_workflow \
  --version 1 \
  --input '{"campaignId": "ADC-530-Q1", "ADC-530-Q1": "advertiserId", "advertiserId": "ADV-100", "ADV-100": "budget", "budget": 10000, "conversions": "sample-conversions"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w advertising_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CreateCampaignWorker to your ad platform (Google Ads, Meta Ads Manager), TargetAudienceWorker to your DMP for audience segments, and ServeAdsWorker to your DSP. The workflow definition stays exactly the same.

- **CreateCampaignWorker** (`adv_create_campaign`): call your ad platform API (Google Ads, Meta Ads, The Trade Desk) to create the campaign with creative assets, ad formats, and landing pages
- **TargetAudienceWorker** (`adv_target_audience`): define real audience segments using your DMP or ad platform's audience builder, specifying demographics, interests, lookalike audiences, and retargeting lists
- **SetBidsWorker** (`adv_set_bids`): configure real bidding strategies (target CPA, target ROAS, maximize conversions) with daily budgets and bid caps via the ad platform's bidding API
- **ServeAdsWorker** (`adv_serve_ads`): activate campaign delivery and pull real performance metrics (impressions, clicks, conversions, CTR, spend) from the ad platform's reporting API
- **GenerateReportWorker** (`adv_generate_report`): compile campaign performance into a report, compute ROI and ROAS, and distribute via email or publish to your BI dashboard

Wire each worker to your DSP or ad platform while keeping the same return structure, and the campaign pipeline adapts without modification.

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
advertising-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/advertisingworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AdvertisingWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateCampaignWorker.java
│       ├── GenerateReportWorker.java
│       ├── ServeAdsWorker.java
│       ├── SetBidsWorker.java
│       └── TargetAudienceWorker.java
└── src/test/java/advertisingworkflow/workers/
    ├── CreateCampaignWorkerTest.java        # 2 tests
    └── TargetAudienceWorkerTest.java        # 2 tests
```
