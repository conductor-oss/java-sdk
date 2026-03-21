# Campaign Automation in Java with Conductor :  Design, Target, Execute, and Measure Marketing Campaigns

A Java Conductor workflow that runs a complete marketing campaign lifecycle .  designing the campaign with creative assets and channel selection, building a targeted audience within budget, executing the campaign across email, social, and display channels, and measuring ROI. Given a `campaignName`, `type`, and `budget`, the pipeline produces a campaign ID, audience size, and return on investment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-phase campaign pipeline.

## Running a Campaign End-to-End

Marketing campaigns involve a strict sequence: design the creative assets and choose channels, build the target audience within the budget, execute the send across all channels, then measure what worked. Each phase depends on the previous one .  you cannot target an audience before the campaign exists, and you cannot measure results before execution. Skipping a step or running them out of order wastes budget and produces unreliable metrics.

This workflow models that lifecycle explicitly. The design step creates a campaign ID with creative assets and channel assignments (email, social, display). The targeting step uses the campaign ID and budget to build an audience segment. The execution step sends to that audience. The measurement step calculates ROI from the execution results. Each step's output flows to the next via JSONPath expressions.

## The Solution

**You just write the campaign design, targeting, execution, and measurement workers. Conductor handles the phase sequencing and data flow between them.**

Four workers handle the campaign lifecycle .  design, targeting, execution, and measurement. The design worker generates a campaign ID and assigns channels. The targeting worker builds an audience segment constrained by budget. The execution worker delivers the campaign to the audience. The measurement worker computes ROI and engagement metrics. Conductor enforces the sequence and passes campaign IDs, audience data, and execution IDs between steps automatically.

### What You Write: Workers

DesignWorker creates campaign assets, TargetWorker builds the audience, ExecuteWorker delivers across channels, and MeasureWorker calculates ROI, each handles one phase of the marketing lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **DesignWorker** | `cpa_design` | Creates a campaign with creative assets and channel assignments (email, social, display). |
| **ExecuteWorker** | `cpa_execute` | Launches the campaign by delivering it to the targeted audience across all assigned channels. |
| **MeasureWorker** | `cpa_measure` | Calculates campaign ROI and engagement metrics (impressions, clicks, conversions). |
| **TargetWorker** | `cpa_target` | Builds a targeted audience segment for the campaign within the specified budget. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
cpa_design
    │
    ▼
cpa_target
    │
    ▼
cpa_execute
    │
    ▼
cpa_measure

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
java -jar target/campaign-automation-1.0.0.jar

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
java -jar target/campaign-automation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cpa_campaign_automation \
  --version 1 \
  --input '{"campaignName": "test", "type": "standard", "budget": "sample-budget"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cpa_campaign_automation -s COMPLETED -c 5

```

## How to Extend

Each worker handles one campaign phase .  connect your marketing platform (HubSpot, Marketo, Mailchimp) for audience targeting and delivery, and your analytics tool (Google Analytics, Mixpanel) for ROI measurement, and the campaign workflow stays the same.

- **DesignWorker** (`cpa_design`): integrate with a CMS or DAM (e.g., Contentful, Adobe AEM) to pull real creative assets
- **ExecuteWorker** (`cpa_execute`): connect to email platforms (SendGrid, Mailchimp) and ad APIs (Facebook Ads, Google Ads) for real delivery
- **MeasureWorker** (`cpa_measure`): pull real engagement data from analytics APIs (Google Analytics, Mixpanel) to compute actual ROI

Swap in your marketing platform APIs and the four-phase campaign lifecycle keeps running as designed.

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
campaign-automation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/campaignautomation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CampaignAutomationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DesignWorker.java
│       ├── ExecuteWorker.java
│       ├── MeasureWorker.java
│       └── TargetWorker.java
└── src/test/java/campaignautomation/workers/
    ├── DesignWorkerTest.java        # 2 tests
    ├── ExecuteWorkerTest.java        # 2 tests
    ├── MeasureWorkerTest.java        # 2 tests
    └── TargetWorkerTest.java        # 2 tests

```
