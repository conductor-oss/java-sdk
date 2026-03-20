# NPS Scoring in Java Using Conductor

A Java Conductor workflow example demonstrating NPS Scoring. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Your product team wants to measure user satisfaction after a quarterly release. The team needs to collect NPS survey responses (scores 0-10) from the user base, calculate the NPS score by categorizing respondents into promoters, passives, and detractors, segment users into actionable groups with tailored follow-up strategies, and trigger the appropriate actions for each segment (referral programs for promoters, engagement campaigns for passives, outreach calls for detractors). Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the survey-collection, NPS-calculation, segmentation, and follow-up workers. Conductor handles the scoring pipeline and segment-based action routing.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CollectResponsesWorker gathers survey scores, CalculateNpsWorker computes the promoter-passive-detractor breakdown, SegmentWorker groups respondents, and ActWorker triggers segment-specific follow-ups like referral programs or outreach calls.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `nps_act` | Triggers segment-specific actions: referral programs for promoters, engagement boosts for passives, outreach calls for detractors |
| **CalculateNpsWorker** | `nps_calculate` | Computes the NPS score from survey responses by counting promoters (9-10), passives (7-8), and detractors (0-6) |
| **CollectResponsesWorker** | `nps_collect_responses` | Collects NPS survey responses for a campaign and period, returning individual user scores |
| **SegmentWorker** | `nps_segment` | Segments respondents into promoter, passive, and detractor groups with assigned follow-up actions |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
nps_collect_responses
    │
    ▼
nps_calculate
    │
    ▼
nps_segment
    │
    ▼
nps_act
```

## Example Output

```
=== Nps Scoring Demo ===

Step 1: Registering task definitions...
  Registered: nps_collect_responses, nps_calculate, nps_segment, nps_act

Step 2: Registering workflow 'nps_scoring'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [act] Triggering actions based on NPS score
  [calculate] Computing NPS from
  [collect_responses] Collecting NPS responses for campaign
  [segment] Segmenting users: promoters=

  Status: COMPLETED
  Output: {actionsTriggered=..., totalActions=..., promoters=..., passives=...}

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
java -jar target/nps-scoring-1.0.0.jar
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
java -jar target/nps-scoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nps_scoring \
  --version 1 \
  --input '{"campaignId": "NPS-2024-Q4", "NPS-2024-Q4": "period", "period": "2024-Q4", "2024-Q4": "sample-2024-Q4"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nps_scoring -s COMPLETED -c 5
```

## How to Extend

Each worker handles one NPS step .  connect your survey platform (Delighted, SurveyMonkey, Typeform) for collection and your CRM (Salesforce, HubSpot) for segment-based follow-up actions, and the NPS workflow stays the same.

- **CollectResponsesWorker** (`nps_collect_responses`): pull survey responses from your survey platform (Delighted, SurveyMonkey, Typeform) or query your database for in-app NPS submissions
- **CalculateNpsWorker** (`nps_calculate`): compute the NPS score using standard methodology and store historical scores in your analytics database for trend tracking
- **SegmentWorker** (`nps_segment`): create user segments in your CRM or marketing platform (HubSpot, Intercom) and tag users for targeted follow-up campaigns
- **ActWorker** (`nps_act`): trigger automated actions: enroll promoters in a referral program via your referral platform, create engagement campaigns in SendGrid for passives, and create support tickets in Zendesk for detractor outreach

Connect your survey platform and marketing tools and the NPS scoring pipeline with segment-based actions keeps running unchanged.

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
nps-scoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/npsscoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NpsScoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── CalculateNpsWorker.java
│       ├── CollectResponsesWorker.java
│       └── SegmentWorker.java
└── src/test/java/npsscoring/workers/
    ├── ActWorkerTest.java        # 4 tests
    ├── CalculateNpsWorkerTest.java        # 5 tests
    ├── CollectResponsesWorkerTest.java        # 4 tests
    └── SegmentWorkerTest.java        # 4 tests
```
