# Lambda Integration in Java Using Conductor

A Java Conductor workflow that orchestrates an AWS Lambda invocation .  preparing the payload, invoking the Lambda function, processing the response, and logging the execution result. Given a function name, qualifier, and input data, the pipeline produces the Lambda response, execution duration, and a log entry. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the prepare-invoke-process-log pipeline.

## Invoking Lambda Functions with Proper Orchestration

Invoking a Lambda function is more than a single API call. The payload needs to be prepared and validated, the function needs to be invoked with the right qualifier (version/alias), the response needs to be parsed and processed for downstream use, and the execution needs to be logged for auditing. Each step depends on the previous one .  you cannot invoke without a payload, and you cannot process without a response.

Without orchestration, you would chain AWS SDK calls manually, manage payloads and response objects between steps, and build custom logging. Conductor sequences the pipeline and passes function names, payloads, and execution results between workers automatically.

## The Solution

**You just write the Lambda orchestration workers. Payload preparation, function invocation, response processing, and execution logging. Conductor handles payload-to-log sequencing, Lambda invocation retries, and execution metadata routing between stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers orchestrate Lambda invocations: PreparePayloadWorker validates and formats input, InvokeLambdaWorker calls the function, ProcessResponseWorker parses the output, and LogResultWorker records the execution for auditing.

| Worker | Task | What It Does |
|---|---|---|
| **PreparePayloadWorker** | `lam_prepare_payload` | Prepares the Lambda invocation payload .  validates input data, formats the JSON payload, and determines the function name and qualifier (version/alias) |
| **InvokeLambdaWorker** | `lam_invoke` | Invokes the AWS Lambda function .  calls the specified function with the prepared payload and returns the response body and execution duration |
| **ProcessResponseWorker** | `lam_process_response` | Processes the Lambda response .  parses the response body, extracts relevant fields, and prepares the output for downstream use |
| **LogResultWorker** | `lam_log_result` | Logs the execution result .  records the function name, duration, status, and processed output for auditing |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### The Workflow

```
lam_prepare_payload
    │
    ▼
lam_invoke
    │
    ▼
lam_process_response
    │
    ▼
lam_log_result

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
java -jar target/lambda-integration-1.0.0.jar

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
| `AWS_ACCESS_KEY_ID` | _(none)_ | AWS access key ID. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in AWS SDK for production. |
| `AWS_SECRET_ACCESS_KEY` | _(none)_ | AWS secret access key. Required alongside `AWS_ACCESS_KEY_ID` for production use. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/lambda-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow lambda_integration_449 \
  --version 1 \
  --input '{"functionName": "test", "inputData": {"key": "value"}, "qualifier": "sample-qualifier"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w lambda_integration_449 -s COMPLETED -c 5

```

## How to Extend

Swap in AWS SDK LambdaClient.invoke() for the invocation worker, your payload validation logic for preparation, and CloudWatch Logs or your audit platform for logging. The workflow definition stays exactly the same.

- **InvokeLambdaWorker** (`lam_invoke`): use the AWS SDK LambdaClient.invoke() to call real Lambda functions
- **LogResultWorker** (`lam_log_result`): push execution logs to CloudWatch Logs or a centralized logging platform
- **ProcessResponseWorker** (`lam_process_response`): add real response parsing, error detection, and data transformation logic

Replace each simulation with real AWS SDK Lambda calls while keeping the same return fields, and the invocation pipeline runs without modification.

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
lambda-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/lambdaintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── LambdaIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── InvokeLambdaWorker.java
│       ├── LogResultWorker.java
│       ├── PreparePayloadWorker.java
│       └── ProcessResponseWorker.java
└── src/test/java/lambdaintegration/workers/
    ├── InvokeLambdaWorkerTest.java        # 2 tests
    ├── LogResultWorkerTest.java        # 2 tests
    ├── PreparePayloadWorkerTest.java        # 2 tests
    └── ProcessResponseWorkerTest.java        # 2 tests

```
