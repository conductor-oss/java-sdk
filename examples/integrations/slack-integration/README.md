# Slack Integration in Java Using Conductor

A Java Conductor workflow that processes Slack events end-to-end. receiving an incoming Slack event (message, reaction, or channel event), processing it into a formatted message, posting the message to a Slack channel, and tracking delivery. Given a channel, event type, and payload, the pipeline produces a processed message, post confirmation, and delivery status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the receive-process-post-track pipeline.

## Processing Slack Events into Channel Messages

When a Slack event arrives (a new message, a reaction, a channel join), you typically need to receive and parse the event, process it into a formatted response, post the response to the appropriate channel, and track whether the message was delivered. Each step depends on the previous one. you cannot format a message without the event data, and you cannot track delivery without a message ID from the post step.

Without orchestration, you would chain Slack API calls manually, manage event payloads and message IDs between steps, and build custom delivery tracking. Conductor sequences the pipeline and routes event data, formatted messages, and message IDs between workers automatically.

## The Solution

**You just write the Slack workers. Event reception, message formatting, channel posting, and delivery tracking. Conductor handles event-to-delivery sequencing, Slack API rate-limit retries, and message ID routing between posting and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers process Slack events: ReceiveEventWorker parses incoming payloads, ProcessEventWorker formats the response message, PostMessageWorker sends to the channel, and TrackDeliveryWorker confirms message delivery.

| Worker | Task | What It Does |
|---|---|---|
| **PostMessageWorker** | `slk_post_message` | Posts a message to a Slack channel. |
| **ProcessEventWorker** | `slk_process_event` | Processes a Slack event and produces a formatted message. |
| **ReceiveEventWorker** | `slk_receive_event` | Receives an incoming Slack event and extracts the event type and data. |
| **TrackDeliveryWorker** | `slk_track_delivery` | Tracks delivery status of a posted Slack message. |

The workers auto-detect Slack credentials at startup. When `SLACK_BOT_TOKEN` is set, PostMessageWorker uses the real Slack Web API (chat.postMessage, via `java.net.http`) to post messages to channels. Without the token, it falls back to demo mode with realistic output shapes so the workflow runs end-to-end without a Slack bot token.

### The Workflow

```
Input -> PostMessageWorker -> ProcessEventWorker -> ReceiveEventWorker -> TrackDeliveryWorker -> Output

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
java -jar target/slack-integration-1.0.0.jar

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
| `SLACK_BOT_TOKEN` | _(none)_ | Slack Bot User OAuth Token (xoxb-...). When set, enables live message posting via the Slack Web API. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/slack-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow slack_integration \
  --version 1 \
  --input '{"channel": "email", "eventType": "standard", "payload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w slack_integration -s COMPLETED -c 5

```

## How to Extend

PostMessageWorker already uses the real Slack Web API (chat.postMessage, via java.net.http) when `SLACK_BOT_TOKEN` is provided. The remaining workers are demo:

- **ReceiveEventWorker** (`slk_receive_event`): integrate with the Slack Events API or Socket Mode for real event ingestion
- **ProcessEventWorker** (`slk_process_event`): add your own business logic for parsing events and generating formatted responses
- **TrackDeliveryWorker** (`slk_track_delivery`): use the Slack Web API conversations.history to verify real message delivery

Replace each simulation with real API calls while keeping the same return structure, and the event processing pipeline stays intact.

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
slack-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/slackintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SlackIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PostMessageWorker.java
│       ├── ProcessEventWorker.java
│       ├── ReceiveEventWorker.java
│       └── TrackDeliveryWorker.java
└── src/test/java/slackintegration/workers/
    ├── PostMessageWorkerTest.java        # 8 tests
    ├── ProcessEventWorkerTest.java        # 8 tests
    ├── ReceiveEventWorkerTest.java        # 8 tests
    └── TrackDeliveryWorkerTest.java        # 8 tests

```
