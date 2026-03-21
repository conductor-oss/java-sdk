# Teams Integration in Java Using Conductor

A Java Conductor workflow that processes Microsoft Teams webhook events end-to-end .  receiving an incoming webhook payload, formatting it into an Adaptive Card, posting the card to a Teams channel, and acknowledging delivery. Given a team ID, channel ID, and webhook payload (e.g., a monitoring alert), the pipeline produces a formatted card, post confirmation, and acknowledgment status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the receive-format-post-acknowledge pipeline.

## Turning Webhook Events into Teams Adaptive Cards

When an external system fires a webhook (a monitoring alert, a CI/CD status change, a support ticket update), you typically need to receive the raw payload, transform it into a rich Adaptive Card with severity colors and action buttons, post the card to the right Teams channel, and send an acknowledgment back to the source. Each step depends on the previous one .  you cannot format a card without the event data, and you cannot acknowledge delivery without a message ID from the post step.

Without orchestration, you would chain Microsoft Graph API calls manually, manage webhook payloads and message IDs between steps, and build custom acknowledgment logic. Conductor sequences the pipeline and routes event data, card payloads, and message IDs between workers automatically.

## The Solution

**You just write the Teams workers. Webhook reception, Adaptive Card formatting, channel posting, and delivery acknowledgment. Conductor handles webhook-to-acknowledgment sequencing, Graph API retries, and message ID routing between formatting, posting, and acknowledgment stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers handle Teams notifications: ReceiveWebhookWorker parses incoming events, FormatCardWorker builds Adaptive Cards, PostCardWorker sends to the channel, and AcknowledgeWorker confirms delivery to the source system.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `tms_acknowledge` | Acknowledges a posted Teams message. |
| **FormatCardWorker** | `tms_format_card` | Formats an adaptive card for Teams from event data. |
| **PostCardWorker** | `tms_post_card` | Posts an adaptive card to a Teams channel. |
| **ReceiveWebhookWorker** | `tms_receive_webhook` | Receives a Teams webhook and extracts event data. |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients .  the workflow orchestration and error handling stay the same.

### The Workflow

```
Input -> AcknowledgeWorker -> FormatCardWorker -> PostCardWorker -> ReceiveWebhookWorker -> Output

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
java -jar target/teams-integration-1.0.0.jar

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
| `TEAMS_WEBHOOK_URL` | _(none)_ | Teams Incoming Webhook URL. When set, PostCardWorker posts real cards to Teams. When unset, all workers run in simulated mode with `[SIMULATED]` output prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/teams-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow teams_integration \
  --version 1 \
  --input '{"teamId": "TEST-001", "channelId": "TEST-001", "webhookPayload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w teams_integration -s COMPLETED -c 5

```

## How to Extend

Swap in Microsoft Graph API calls for channel posting, Adaptive Card JSON for rich formatting, and your webhook endpoint for incoming event reception. The workflow definition stays exactly the same.

- **ReceiveWebhookWorker** (`tms_receive_webhook`): integrate with Azure Bot Service or a real webhook endpoint to receive incoming Teams/external events
- **FormatCardWorker** (`tms_format_card`): use the Microsoft Adaptive Cards SDK to build real Adaptive Card JSON with dynamic data, severity colors, and action buttons
- **PostCardWorker** (`tms_post_card`): use the Microsoft Graph API (POST /teams/{teamId}/channels/{channelId}/messages) to post real Adaptive Cards to Teams channels

Connect each worker to the Microsoft Graph API while preserving output fields, and the webhook-to-acknowledge pipeline needs no changes.

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
teams-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/teamsintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TeamsIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcknowledgeWorker.java
│       ├── FormatCardWorker.java
│       ├── PostCardWorker.java
│       └── ReceiveWebhookWorker.java
└── src/test/java/teamsintegration/workers/
    ├── AcknowledgeWorkerTest.java        # 8 tests
    ├── FormatCardWorkerTest.java        # 7 tests
    ├── PostCardWorkerTest.java        # 8 tests
    └── ReceiveWebhookWorkerTest.java        # 8 tests

```
