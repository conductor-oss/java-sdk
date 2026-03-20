# AWS Integration in Java Using Conductor

A Java Conductor workflow that coordinates writes across three AWS services in parallel .  uploading an object to S3, writing an item to DynamoDB, and publishing a notification to SNS ,  then verifying all three completed successfully. Uses a FORK_JOIN to run the three service calls simultaneously, followed by a verification step. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the parallel fan-out and verification.

## Writing to Multiple AWS Services Reliably

When an event arrives, you often need to persist it across multiple AWS services simultaneously .  store the raw payload in S3 for archival, write a record to DynamoDB for querying, and publish a notification to SNS for downstream consumers. These three operations are independent of each other and can run in parallel, but all three must succeed for the operation to be considered complete. If any one fails, you need visibility into which service failed and the ability to retry just that step.

Without orchestration, you would manage three async calls manually, implement your own fan-out/join logic, handle partial failures, and build retry loops for each service. Conductor's FORK_JOIN handles all of this declaratively.

## The Solution

**You just write the AWS service workers. S3 upload, DynamoDB write, SNS publish, and cross-service verification. Conductor handles parallel fan-out via FORK_JOIN, per-service retries, and cross-service verification tracking.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers coordinate AWS writes: AwsS3UploadWorker stores objects, AwsDynamoDbWriteWorker persists items, AwsSnsNotifyWorker publishes notifications, and AwsVerifyWorker confirms all three parallel branches completed successfully.

| Worker | Task | What It Does |
|---|---|---|
| **AwsS3UploadWorker** | `aws_s3_upload` | Uploads the payload to S3 .  stores the object at `data/{id}.json` in the specified bucket and returns the s3Key |
| **AwsDynamoDbWriteWorker** | `aws_dynamodb_write` | Writes the payload as a DynamoDB item .  inserts the document into the specified table and returns the itemId |
| **AwsSnsNotifyWorker** | `aws_sns_notify` | Publishes a notification to SNS .  sends the payload as a message to the specified topicArn and returns the messageId |
| **AwsVerifyWorker** | `aws_verify` | Verifies all three services completed .  checks the s3Key, DynamoDB itemId, and SNS messageId from the parallel branches and confirms the fan-out succeeded |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
FORK_JOIN
    ├── aws_s3_upload
    ├── aws_dynamodb_write
    └── aws_sns_notify
    │
    ▼
JOIN (wait for all branches)
aws_verify
```

## Example Output

```
=== Example 441: AWS Integratio ===

Step 1: Registering task definitions...
  Registered: aws_s3_upload, aws_dynamodb_write, aws_sns_notify, aws_verify

Step 2: Registering workflow 'aws_integration'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [DynamoDB] Written item
  [S3] Uploaded to s3://
  [SNS] Published
  [verify] S3:

  Status: COMPLETED
  Output: {itemId=..., tableName=..., consumedCapacity=..., s3Key=...}

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
java -jar target/aws-integration-1.0.0.jar
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
java -jar target/aws-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow aws_integration \
  --version 1 \
  --input '{"bucket": "my-data-bucket", "my-data-bucket": "tableName", "tableName": "events-table", "events-table": "topicArn", "topicArn": "arn:aws:sns:us-east-1:123456789:data-events", "arn:aws:sns:us-east-1:123456789:data-events": "payload", "payload": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w aws_integration -s COMPLETED -c 5
```

## How to Extend

Swap in the AWS SDK calls. S3Client.putObject() in the upload worker, DynamoDbClient.putItem() in the write worker, and SnsClient.publish() in the notify worker. The workflow definition stays exactly the same.

- **AwsDynamoDbWriteWorker** (`aws_dynamodb_write`): use the AWS SDK DynamoDbClient to write real items to a DynamoDB table
- **AwsS3UploadWorker** (`aws_s3_upload`): use the AWS SDK S3Client to upload real objects to an S3 bucket
- **AwsSnsNotifyWorker** (`aws_sns_notify`): use the AWS SDK SnsClient to publish real messages to an SNS topic

Replace each simulated worker with a real AWS SDK call while keeping the same output fields, and the FORK_JOIN fan-out continues unchanged.

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
aws-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/awsintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AwsIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AwsDynamoDbWriteWorker.java
│       ├── AwsS3UploadWorker.java
│       ├── AwsSnsNotifyWorker.java
│       └── AwsVerifyWorker.java
└── src/test/java/awsintegration/workers/
    ├── AwsDynamoDbWriteWorkerTest.java        # 8 tests
    ├── AwsS3UploadWorkerTest.java        # 8 tests
    ├── AwsSnsNotifyWorkerTest.java        # 8 tests
    └── AwsVerifyWorkerTest.java        # 8 tests
```
