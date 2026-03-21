# Serverless Function Chain in Java Using Conductor :  Parse, Enrich, Score, Aggregate

A Java Conductor workflow example for serverless function orchestration. invoking a parse function to extract structured data from an event, enriching it with external context, scoring the enriched data, and aggregating the final results. Uses [Conductor](https://github.

## Chaining Lambda Functions Without Losing Control

You have four Lambda functions that need to run in sequence: one parses raw event payloads into structured data, one enriches the parsed data with external API lookups, one scores the enriched records (fraud score, relevance score, risk score), and one aggregates the scored results into a summary. Each function's output is the next function's input.

Chaining Lambdas natively (Step Functions, direct invocation) works until you need retries with backoff on the enrichment call, timeout handling on the scoring function, and a complete trace of which event produced which score. You end up building orchestration logic inside your Lambda code, defeating the purpose of serverless.

## The Solution

**You write each function's invocation logic. Conductor handles the chain, cold-start retries, and end-to-end tracing.**

`SvlInvokeParseWorker` calls the parse function to extract structured fields from the raw event payload. `SvlInvokeEnrichWorker` calls the enrichment function to augment the parsed data with external context. `SvlInvokeScoreWorker` calls the scoring function to compute a score on the enriched data. `SvlAggregateWorker` combines the scored results into a final summary. Conductor chains these invocations, retries any that fail (cold start timeouts, transient API errors), and records the full input/output of each function call.

### What You Write: Workers

Four workers chain serverless invocations: event parsing, external enrichment, scoring computation, and result aggregation, each wrapping one Lambda function call with retry-safe orchestration.

| Worker | Task | What It Does |
|---|---|---|
| **SvlAggregateWorker** | `svl_aggregate` | Aggregates results from the serverless function chain. |
| **SvlInvokeEnrichWorker** | `svl_invoke_enrich` | Invokes the enrich serverless function to add user context. |
| **SvlInvokeParseWorker** | `svl_invoke_parse` | Invokes the parse serverless function for an incoming event. |
| **SvlInvokeScoreWorker** | `svl_invoke_score` | Invokes the score serverless function to compute engagement score. |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
svl_invoke_parse
    │
    ▼
svl_invoke_enrich
    │
    ▼
svl_invoke_score
    │
    ▼
svl_aggregate

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
java -jar target/serverless-orchestration-1.0.0.jar

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
java -jar target/serverless-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow serverless_orchestration_demo \
  --version 1 \
  --input '{"eventId": "TEST-001", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w serverless_orchestration_demo -s COMPLETED -c 5

```

## How to Extend

Each worker wraps one serverless function invocation. replace the simulated Lambda calls with real AWS Lambda or Cloud Functions APIs and the function chain runs unchanged.

- **SvlInvokeParseWorker** (`svl_invoke_parse`): invoke a real AWS Lambda function (`lambda.invoke()`), Google Cloud Function, or Azure Function to parse event payloads
- **SvlInvokeEnrichWorker** (`svl_invoke_enrich`): call real enrichment APIs: Clearbit for company data, MaxMind for geo-IP, or your own microservice for domain-specific context
- **SvlInvokeScoreWorker** (`svl_invoke_score`): invoke a real scoring Lambda that runs an ML model (SageMaker endpoint, custom TensorFlow Serving) and returns fraud/risk/relevance scores

The function invocation contract stays fixed. Swap the simulated Lambda calls for real AWS SDK invocations or Azure Functions triggers and the parse-enrich-score chain runs unchanged.

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
serverless-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/serverlessorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ServerlessOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── SvlAggregateWorker.java
│       ├── SvlInvokeEnrichWorker.java
│       ├── SvlInvokeParseWorker.java
│       └── SvlInvokeScoreWorker.java
└── src/test/java/serverlessorchestration/workers/
    ├── SvlAggregateWorkerTest.java        # 8 tests
    ├── SvlInvokeEnrichWorkerTest.java        # 8 tests
    ├── SvlInvokeParseWorkerTest.java        # 8 tests
    └── SvlInvokeScoreWorkerTest.java        # 8 tests

```
