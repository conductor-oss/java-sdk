# User Feedback Processing in Java Using Conductor :  Collection, Classification, Routing, and Auto-Response

A Java Conductor workflow example for processing user feedback. ingesting submissions from any channel, classifying them by category and priority, routing to the right internal team, and sending an automatic acknowledgment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to handle incoming user feedback from multiple sources (in-app forms, email, support portals). Each submission must be ingested, classified as a bug report, feature request, or general feedback, assigned a priority based on severity signals like "crash" or "urgent," routed to the appropriate team (engineering for bugs, product for feature requests, support for everything else), and followed up with a personalized acknowledgment so the user knows their feedback landed.

Without orchestration, you'd chain all of this in a single service. parsing text for keywords, looking up team routing tables, composing response messages, and wrapping every step in try/catch. When classification logic changes or a new feedback channel appears, you're editing a monolith. Failures in the response step can silently swallow the whole submission, and you have no visibility into which feedback items were classified but never routed.

## The Solution

**You just write the feedback-ingestion, classification, routing, and auto-response workers. Conductor handles the processing pipeline and team routing.**

Each concern. ingestion, classification, routing, response,  is a simple, independent worker. Conductor runs them in sequence, passes the classification output into routing and routing output into the response, retries any step that fails, and tracks every feedback submission end-to-end. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CollectFeedbackWorker ingests submissions, ClassifyFeedbackWorker categorizes by type and priority, RouteFeedbackWorker assigns to the right team, and RespondFeedbackWorker sends personalized acknowledgments.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFeedbackWorker** | `ufb_collect` | Ingests a feedback submission, assigns a unique feedback ID and timestamp |
| **ClassifyFeedbackWorker** | `ufb_classify` | Scans feedback text for keywords to determine category (bug, feature_request, general) and priority (high, medium) |
| **RouteFeedbackWorker** | `ufb_route` | Maps the classified category to an internal team (engineering, product, support) and creates a ticket |
| **RespondFeedbackWorker** | `ufb_respond` | Sends the user a personalized auto-response referencing their category and assigned team |

Workers implement user lifecycle operations. account creation, verification, profile setup,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
ufb_collect
    │
    ▼
ufb_classify
    │
    ▼
ufb_route
    │
    ▼
ufb_respond

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
java -jar target/user-feedback-1.0.0.jar

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
java -jar target/user-feedback-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ufb_user_feedback \
  --version 1 \
  --input '{"userId": "TEST-001", "feedbackText": "Process this order for customer C-100", "source": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ufb_user_feedback -s COMPLETED -c 5

```

## How to Extend

Each worker handles one feedback step. connect your feedback tool (Intercom, Canny, UserVoice) for ingestion and your project tracker (Jira, Linear) for team routing, and the feedback workflow stays the same.

- **CollectFeedbackWorker** (`ufb_collect`): persist feedback to PostgreSQL/DynamoDB and publish to a Kafka topic for analytics
- **ClassifyFeedbackWorker** (`ufb_classify`): replace keyword matching with an ML classifier or LLM-based sentiment analysis for richer categorization
- **RouteFeedbackWorker** (`ufb_route`): create real tickets in Jira, Linear, or Zendesk using their APIs, with priority-based SLA assignment
- **RespondFeedbackWorker** (`ufb_respond`): send real acknowledgment emails via SendGrid/SES, or push in-app notifications with personalized follow-up timelines

Wire up Jira for ticket creation and SendGrid for auto-responses and the feedback classification pipeline stays intact.

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
user-feedback/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/userfeedback/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserFeedbackExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyFeedbackWorker.java
│       ├── CollectFeedbackWorker.java
│       ├── RespondFeedbackWorker.java
│       └── RouteFeedbackWorker.java
└── src/test/java/userfeedback/workers/
    ├── ClassifyFeedbackWorkerTest.java        # 6 tests
    ├── CollectFeedbackWorkerTest.java        # 2 tests
    ├── RespondFeedbackWorkerTest.java        # 2 tests
    └── RouteFeedbackWorkerTest.java        # 4 tests

```
