# Twilio Integration in Java Using Conductor

A Java Conductor workflow that runs a two-way SMS conversation via Twilio .  sending an outbound SMS, waiting for the recipient's reply, processing the response to generate contextual follow-up content, and sending a reply SMS back. Given a to/from phone number pair and message body, the pipeline produces the original message SID, the received response, and the reply SID. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the send-wait-process-reply pipeline.

## Running Two-Way SMS Conversations Through Twilio

Two-way SMS involves more than sending a single message. You send an outbound SMS, wait for the recipient to reply (polling or webhook), process the reply to determine the appropriate response (parsing keywords, looking up context, generating a follow-up), and send the reply back. Each step depends on the previous one .  you cannot wait for a reply without a message SID from the send step, and you cannot generate a reply without the response body.

Without orchestration, you would chain Twilio REST API calls manually, manage message SIDs and response bodies between steps, and build custom polling or webhook handling for inbound messages. Conductor sequences the pipeline and routes message SIDs, response content, and reply text between workers automatically.

## The Solution

**You just write the SMS workers. Outbound sending, reply waiting, response processing, and follow-up replies. Conductor handles send-wait-reply sequencing, Twilio API retries, and message SID routing between outbound and inbound stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers run two-way SMS conversations: SendSmsWorker delivers the outbound message, WaitResponseWorker captures the reply, ProcessResponseWorker generates contextual follow-up content, and SendReplyWorker transmits the response.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessResponseWorker** | `twl_process_response` | Processes an SMS response. |
| **SendReplyWorker** | `twl_send_reply` | Sends a reply SMS. |
| **SendSmsWorker** | `twl_send_sms` | Sends an SMS via Twilio. |
| **WaitResponseWorker** | `twl_wait_response` | Waits for an SMS response. |

The workers auto-detect Twilio credentials at startup. When `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN` are set, SendSmsWorker and SendReplyWorker use the real Twilio API to deliver messages. Without credentials, they fall back to simulated mode with realistic output shapes so the workflow runs end-to-end without a Twilio account.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
twl_send_sms
    │
    ▼
twl_wait_response
    │
    ▼
twl_process_response
    │
    ▼
twl_send_reply
```

## Example Output

```
=== Example 436: Twilio Integratio ===

Step 1: Registering task definitions...
  Registered: twl_send_sms, twl_wait_response, twl_process_response, twl_send_reply

Step 2: Registering workflow 'twilio_integration_436'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [SIMULATED][process] Response \"" + responseBody + "\" -> reply: \"" + replyMessage + "\"
  [reply] SMS
  [send] SMS
  [SIMULATED][wait] Response from

  Status: COMPLETED
  Output: {replyMessage=..., intent=..., messageSid=..., responseBody=...}

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
java -jar target/twilio-integration-1.0.0.jar
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
| `TWILIO_ACCOUNT_SID` | _(none)_ | Twilio Account SID. When set with `TWILIO_AUTH_TOKEN`, enables live SMS sending. |
| `TWILIO_AUTH_TOKEN` | _(none)_ | Twilio Auth Token. When set with `TWILIO_ACCOUNT_SID`, enables live SMS sending. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/twilio-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow twilio_integration_436 \
  --version 1 \
  --input '{"toNumber": 5, "+15551234567": "sample-+15551234567", "fromNumber": 5, "+15559832143": "sample-+15559832143", "messageBody": "Sample messageBody"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w twilio_integration_436 -s COMPLETED -c 5
```

## How to Extend

SendSmsWorker and SendReplyWorker already use the real Twilio Java SDK (Message.creator()) when credentials are provided. The remaining workers are simulated:

- **WaitResponseWorker** (`twl_wait_response`): integrate with Twilio webhooks or use the Twilio Messages API to poll for real inbound replies by message SID
- **ProcessResponseWorker** (`twl_process_response`): add your own business logic for parsing responses, looking up context, or generating follow-up content

Replace each simulation with real API calls while keeping the same output fields, and the conversational pipeline stays unchanged.

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
twilio-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/twiliointegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TwilioIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessResponseWorker.java
│       ├── SendReplyWorker.java
│       ├── SendSmsWorker.java
│       └── WaitResponseWorker.java
└── src/test/java/twiliointegration/workers/
    ├── ProcessResponseWorkerTest.java        # 2 tests
    ├── SendReplyWorkerTest.java        # 2 tests
    ├── SendSmsWorkerTest.java        # 2 tests
    └── WaitResponseWorkerTest.java        # 2 tests
```
