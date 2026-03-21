# Content Personalization in Java Using Conductor :  User Profiling, Segmentation, Content Selection, Ranking, and Serving

A Java Conductor workflow example that orchestrates content personalization. collecting user profiles (interests, demographics, region, account age), segmenting users into behavioral cohorts with confidence scores, selecting content candidates matching the segment, ranking candidates by predicted relevance using ML models, and serving personalized content with sub-200ms response time tracking and experiment assignment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Personalization Pipelines Need Orchestration

Personalizing content for each user request requires a real-time decision pipeline. You collect the user's profile. interests, demographics, geographic region, account age. You assign them to a behavioral segment (power user, casual browser, new visitor) with a confidence score. You select content candidates that match the segment's preferences. You rank those candidates using a personalization model that weighs recency, relevance, and engagement history. Finally, you serve the ranked list and record the experiment ID for A/B analysis.

Each stage refines the previous one's output. segmentation needs profile data, content selection needs the segment, ranking needs the candidate list. If the profile lookup is slow, the entire personalization response is slow. If you skip segmentation and serve generic content, engagement drops. Without orchestration, you'd build a monolithic personalization service that mixes user data fetching, ML segmentation, content retrieval, and ranking,  making it impossible to upgrade your ranking model, test new segments independently, or measure which stage contributes most to response latency.

## How This Workflow Solves It

**You just write the personalization workers. Profile collection, user segmentation, content selection, ranking, and serving. Conductor handles real-time sequencing, model endpoint retries, and response-time tracking for latency optimization.**

Each personalization stage is an independent worker. collect profile, segment user, select content, rank results, serve content. Conductor sequences them, passes user profiles and candidate lists between stages, retries if a model endpoint times out, and tracks response time and cache hit rates for performance optimization.

### What You Write: Workers

Five workers run the personalization pipeline: CollectProfileWorker gathers user interests and demographics, SegmentUserWorker assigns behavioral cohorts, SelectContentWorker finds matching candidates, RankContentWorker orders by predicted relevance, and ServeContentWorker delivers results with experiment tracking.

| Worker | Task | What It Does |
|---|---|---|
| **CollectProfileWorker** | `per_collect_profile` | Collect Profile. Computes and returns interests, demographics, region, account age |
| **RankContentWorker** | `per_rank_content` | Ranks the content |
| **SegmentUserWorker** | `per_segment_user` | Segments the user |
| **SelectContentWorker** | `per_select_content` | Handles select content |
| **ServeContentWorker** | `per_serve_content` | Handles serve content |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
per_collect_profile
    │
    ▼
per_segment_user
    │
    ▼
per_select_content
    │
    ▼
per_rank_content
    │
    ▼
per_serve_content

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
java -jar target/personalization-1.0.0.jar

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
java -jar target/personalization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow personalization_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "sessionId": "TEST-001", "pageContext": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w personalization_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectProfileWorker to your user data store, SegmentUserWorker to your ML segmentation model, and RankContentWorker to your personalization ranking service. The workflow definition stays exactly the same.

- **CollectProfileWorker** (`per_collect_profile`): fetch real user data from your user service, CRM, or CDP (Segment, mParticle) including interests, demographics, and behavioral signals
- **SegmentUserWorker** (`per_segment_user`): assign users to segments using your segmentation model or rules engine, returning segment labels, sub-segments, and confidence scores
- **SelectContentWorker** (`per_select_content`): query your content catalog (Elasticsearch, Algolia, custom API) for candidates matching the segment's preferences and context
- **RankContentWorker** (`per_rank_content`): score and rank candidates using your personalization model (collaborative filtering, content-based, hybrid) deployed on SageMaker or TensorFlow Serving
- **ServeContentWorker** (`per_serve_content`): return the personalized content list to the client API, record the experiment assignment, and track response time and cache efficiency

Connect any worker to your user data store or ML ranking service while keeping its return fields, and the personalization flow needs no changes.

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
personalization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/personalization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PersonalizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectProfileWorker.java
│       ├── RankContentWorker.java
│       ├── SegmentUserWorker.java
│       ├── SelectContentWorker.java
│       └── ServeContentWorker.java
└── src/test/java/personalization/workers/
    ├── CollectProfileWorkerTest.java        # 2 tests
    └── SegmentUserWorkerTest.java        # 2 tests

```
