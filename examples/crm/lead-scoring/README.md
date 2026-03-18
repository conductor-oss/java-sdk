# Lead Scoring in Java with Conductor -- Collect Signals, Score, Classify, and Route Leads to Sales

Your top rep just spent three weeks nurturing a lead who was never going to buy -- meanwhile, a VP of Engineering who visited your pricing page four times and downloaded your security whitepaper sat untouched in the queue until a competitor closed them. This happens constantly when every lead looks the same in the CRM: salespeople guess who to call based on gut feel, hot leads cool off waiting for attention, and cold leads waste hours of expensive human outreach. This workflow scores leads automatically -- collecting behavioral signals, computing a numeric score, classifying urgency, and routing to the right rep -- so your team works the leads that are actually ready to buy. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step scoring pipeline.

## Focusing Sales on the Leads Most Likely to Convert

Sales teams cannot pursue every lead equally. A lead who visited the pricing page three times and downloaded a whitepaper is more likely to convert than one who opened a single marketing email. Lead scoring quantifies buying intent by aggregating behavioral signals into a numeric score, classifying the lead by urgency, and routing hot leads to senior reps while cold leads go to automated nurturing.

This workflow processes one lead through the scoring pipeline. The signal collector gathers behavioral data -- page visits, email opens, content downloads, demo requests. The scorer weighs those signals and computes a numeric score. The classifier maps the score to a category: hot (high score, ready to buy), warm (moderate engagement, needs nurturing), or cold (low engagement, not ready). The router assigns the lead to the right sales rep or automation track based on the classification.

## The Solution

**You just write the signal-collection, scoring, classification, and routing workers. Conductor handles the scoring pipeline and lead data flow.**

Four workers form the scoring pipeline -- signal collection, scoring, classification, and routing. The signal collector gathers engagement data across channels. The scorer computes a weighted score from those signals. The classifier labels the lead as hot, warm, or cold. The router assigns the lead to a sales rep or nurture track. Conductor sequences the four steps and passes signals, scores, and classifications between them via JSONPath.

### What You Write: Workers

CollectSignalsWorker gathers page visits and email engagement, ScoreWorker computes a weighted score, ClassifyWorker labels the lead as hot/warm/cold, and RouteWorker assigns them to a sales rep or nurture track.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **ClassifyWorker** | `ls_classify` | Classifies a lead based on score into hot/warm/cold. | Simulated |
| **CollectSignalsWorker** | `ls_collect_signals` | Collects behavioral signals for a lead. | Simulated |
| **RouteWorker** | `ls_route` | Routes a lead to the appropriate sales rep based on classification. | Simulated |
| **ScoreWorker** | `ls_score` | Calculates lead score from collected signals. | Simulated |

Workers simulate CRM operations -- lead scoring, contact enrichment, deal updates -- with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ls_collect_signals
    │
    ▼
ls_score
    │
    ▼
ls_classify
    │
    ▼
ls_route
```

## Example Output

```
=== Example 622: Lead Scoring ===

Step 1: Registering task definitions...
  Registered: ls_collect_signals, ls_score, ls_classify, ls_route

Step 2: Registering workflow 'ls_lead_scoring'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 4cff8158-cf74-73ed-c62f-a4dda902bf8a

  [signals] Collected behavioral signals for lead LEAD-622
  [score] Lead score calculated: 85
  [classify] Lead classified as: high (score: 85)
  [route] Lead LEAD-622 routed to team-lead


  Status: COMPLETED
  Output: {leadScore=85, classification=high, assignedTo=team-lead}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/lead-scoring-1.0.0.jar
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
java -jar target/lead-scoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ls_lead_scoring \
  --version 1 \
  --input '{"leadId": "LEAD-622", "email": "henry@techcorp.com", "company": "TechCorp"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ls_lead_scoring -s COMPLETED -c 5
```

## How to Extend

Each worker handles one scoring step -- connect your CRM (Salesforce, HubSpot) for behavioral signals and your sales engagement platform (Outreach, Salesloft) for rep routing, and the lead-scoring workflow stays the same.

- **ClassifyWorker** (`ls_classify`) -- use an ML model trained on historical conversion data for more accurate hot/warm/cold thresholds
- **CollectSignalsWorker** (`ls_collect_signals`) -- integrate with analytics tools (Mixpanel, Amplitude, Google Analytics) for real behavioral data
- **RouteWorker** (`ls_route`) -- connect to your CRM (Salesforce, HubSpot) to assign leads and trigger sales workflows

Wire up your CRM's behavioral data and the signal-score-classify-route pipeline continues to prioritize leads without modification.

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
lead-scoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/leadscoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LeadScoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── CollectSignalsWorker.java
│       ├── RouteWorker.java
│       └── ScoreWorker.java
└── src/test/java/leadscoring/workers/
    ├── ClassifyWorkerTest.java        # 4 tests
    ├── CollectSignalsWorkerTest.java        # 3 tests
    ├── RouteWorkerTest.java        # 3 tests
    └── ScoreWorkerTest.java        # 3 tests
```
