# SNS SQS Integration in Java Using Conductor

A Java Conductor workflow that runs an AWS SNS/SQS messaging pipeline end-to-end .  publishing a message to an SNS topic, subscribing an SQS queue to that topic, receiving the delivered message from the queue, and processing and deleting it. Given a topic ARN, queue URL, and message body, the pipeline produces publish confirmation, subscription ARN, and processing status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the publish-subscribe-receive-process pipeline.

## Publishing Messages Through SNS to SQS

Fan-out messaging with SNS and SQS involves a strict sequence: publish a message to an SNS topic, ensure the target SQS queue is subscribed to that topic, poll the queue for the delivered message, and process it (which typically includes parsing the SNS envelope and deleting the message from the queue). Each step depends on the previous one .  you cannot receive a message that has not been published, and you need the subscription in place before messages will flow to the queue.

Without orchestration, you would chain AWS SDK calls manually, manage message IDs, subscription ARNs, and receipt handles between steps, and handle partial failures like a message published but not yet delivered. Conductor sequences the pipeline and routes topic ARNs, message IDs, and receipt handles between workers automatically.

## The Solution

**You just write the messaging workers. SNS publishing, SQS subscription, message receiving, and message processing. Conductor handles publish-to-process sequencing, SQS polling retries, and message ID and receipt handle routing between stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run the messaging pipeline: SnsPublishWorker sends to an SNS topic, SubscribeQueueWorker links the SQS queue, ReceiveMessageWorker polls for delivered messages, and ProcessMessageWorker parses and deletes them.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessMessageWorker** | `sns_process_message` | Processes a received SQS message. |
| **ReceiveMessageWorker** | `sns_receive_message` | Receives a message from an SQS queue. |
| **SnsPublishWorker** | `sns_publish` | Publishes a message to an SNS topic. |
| **SubscribeQueueWorker** | `sns_subscribe_queue` | Subscribes an SQS queue to an SNS topic. |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### The Workflow

```
sns_publish
    │
    ▼
sns_subscribe_queue
    │
    ▼
sns_receive_message
    │
    ▼
sns_process_message

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
java -jar target/sns-sqs-integration-1.0.0.jar

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
java -jar target/sns-sqs-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sns_sqs_integration_448 \
  --version 1 \
  --input '{"topicArn": "microservices best practices", "queueUrl": "https://example.com", "messageBody": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sns_sqs_integration_448 -s COMPLETED -c 5

```

## How to Extend

Swap in AWS SDK SnsClient.publish() for topic publishing, SqsClient.receiveMessage() for queue polling, and your message processing logic for the consumer step. The workflow definition stays exactly the same.

- **SnsPublishWorker** (`sns_publish`): use the AWS SDK SnsClient.publish() to send real messages to an SNS topic
- **SubscribeQueueWorker** (`sns_subscribe_queue`): use the AWS SDK SnsClient.subscribe() to create a real SQS subscription on an SNS topic
- **ReceiveMessageWorker** (`sns_receive_message`): use the AWS SDK SqsClient.receiveMessage() to poll for real messages from an SQS queue

Replace each simulation with real AWS SDK messaging calls while preserving output fields, and the publish-subscribe pipeline runs without modification.

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
sns-sqs-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/snssqsintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SnsSqsIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessMessageWorker.java
│       ├── ReceiveMessageWorker.java
│       ├── SnsPublishWorker.java
│       └── SubscribeQueueWorker.java
└── src/test/java/snssqsintegration/workers/
    ├── ProcessMessageWorkerTest.java        # 2 tests
    ├── ReceiveMessageWorkerTest.java        # 2 tests
    ├── SnsPublishWorkerTest.java        # 2 tests
    └── SubscribeQueueWorkerTest.java        # 2 tests

```
