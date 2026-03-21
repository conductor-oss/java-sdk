# Customer Journey in Java with Conductor :  Track Touchpoints, Map Stages, and Optimize the Path to Conversion

A Java Conductor workflow that maps a customer's journey from first contact to conversion. tracking touchpoints across channels, mapping them into journey stages, analyzing the journey for drop-off points and friction, and generating optimization recommendations. Given a `customerId` and `timeWindow`, the pipeline produces touchpoint counts, journey stage breakdowns, and actionable recommendations to improve conversion. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step journey analysis.

## Seeing the Full Picture of How Customers Convert

Customers interact with your product through dozens of touchpoints. website visits, email opens, support tickets, feature usage, social engagement. These interactions are scattered across systems and channels. Understanding the path from first touch to conversion (or churn) requires collecting all touchpoints, organizing them into stages (awareness, consideration, decision), analyzing where customers drop off, and recommending changes to improve the funnel.

This workflow performs that analysis end-to-end. The touchpoint tracker collects all interactions for a customer within the specified time window. The journey mapper organizes those touchpoints into sequential stages. The analyzer identifies patterns. which stages have the highest drop-off, which channels are most effective, where friction slows the journey. The optimizer generates specific recommendations based on the analysis insights.

## The Solution

**You just write the touchpoint-tracking, journey-mapping, analysis, and optimization workers. Conductor handles the four-step pipeline and data flow.**

Four workers handle the journey analysis. touchpoint tracking, journey mapping, insight analysis, and optimization. The tracker collects cross-channel interactions for the customer. The mapper groups touchpoints into journey stages. The analyzer extracts insights about drop-offs and friction points. The optimizer recommends changes based on those insights. Conductor sequences the four steps and passes touchpoints, journey maps, and insights between them automatically.

### What You Write: Workers

TrackTouchpointsWorker collects cross-channel interactions, MapJourneyWorker organizes them into stages, AnalyzeWorker identifies drop-off points, and OptimizeWorker generates conversion recommendations.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `cjy_analyze` | Analyzes the customer journey for insights. |
| **MapJourneyWorker** | `cjy_map_journey` | Maps customer touchpoints into journey stages. |
| **OptimizeWorker** | `cjy_optimize` | Generates optimization recommendations based on journey insights. |
| **TrackTouchpointsWorker** | `cjy_track_touchpoints` | Tracks customer touchpoints across channels. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
cjy_track_touchpoints
    │
    ▼
cjy_map_journey
    │
    ▼
cjy_analyze
    │
    ▼
cjy_optimize

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
java -jar target/customer-journey-1.0.0.jar

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
java -jar target/customer-journey-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cjy_customer_journey \
  --version 1 \
  --input '{"customerId": "TEST-001", "timeWindow": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cjy_customer_journey -s COMPLETED -c 5

```

## How to Extend

Each worker handles one analysis phase. connect your CDP (Segment, mParticle) for touchpoint collection and your analytics platform (Amplitude, Mixpanel) for journey insights, and the customer-journey workflow stays the same.

- **AnalyzeWorker** (`cjy_analyze`): connect to analytics platforms (Amplitude, Mixpanel) for real funnel and cohort analysis
- **MapJourneyWorker** (`cjy_map_journey`): integrate with a CDP (Segment, mParticle) to pull real cross-channel event streams
- **OptimizeWorker** (`cjy_optimize`): use an LLM to generate personalized recommendations from journey insights

Wire up your CDP or analytics platform and the touchpoint-to-optimization journey analysis continues without workflow changes.

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
customer-journey/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/customerjourney/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomerJourneyExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeWorker.java
│       ├── MapJourneyWorker.java
│       ├── OptimizeWorker.java
│       └── TrackTouchpointsWorker.java
└── src/test/java/customerjourney/workers/
    ├── AnalyzeWorkerTest.java        # 3 tests
    ├── MapJourneyWorkerTest.java        # 4 tests
    ├── OptimizeWorkerTest.java        # 3 tests
    └── TrackTouchpointsWorkerTest.java        # 4 tests

```
