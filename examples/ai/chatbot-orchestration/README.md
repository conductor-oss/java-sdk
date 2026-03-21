# Chatbot Orchestration in Java with Conductor :  Intent Detection and Response Generation Pipeline

A Java Conductor workflow that processes a chatbot conversation turn. receiving the user message with session context, detecting intent (refund requests, cancellation, general inquiry), generating an appropriate response, and delivering it back to the user. Given a `userId`, `message`, and `sessionId`, the pipeline classifies intent with confidence scores, extracts entities (product names, amounts), and produces a contextual response. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the receive-understand-generate-deliver pipeline.

## Turning a User Message into an Intelligent Response

A chatbot needs to do more than echo text back. Each incoming message must be received with its session context, analyzed for intent (is this a refund request? a cancellation? a general question?), used to generate a relevant response, and delivered back to the user's channel. These steps must happen in sequence. you cannot generate a response without knowing the intent, and you cannot deliver without a response.

This workflow models a single conversation turn. The receive step captures the message and loads session context. The intent classifier analyzes the message text, detecting intents like `request_refund`, `cancel_subscription`, or `general_inquiry` with a confidence score, and extracts entities (product name, dollar amount). The response generator uses the detected intent and entities to craft a reply. The delivery step sends the response back to the user's session.

## The Solution

**You just write the message-receiving, intent-detection, response-generation, and delivery workers. Conductor handles the conversation-turn pipeline.**

Four workers handle one chatbot turn. message receiving, intent understanding, response generation, and delivery. The intent worker scans for keywords like "refund" and "cancel" to classify the message and extract entities like product names and amounts. The response generator uses the classified intent and extracted entities to produce a relevant reply. Conductor sequences the four steps and tracks every conversation turn with full input/output visibility.

### What You Write: Workers

ReceiveWorker captures the message with session context, UnderstandIntentWorker classifies intent and extracts entities, GenerateResponseWorker crafts a reply, and DeliverWorker sends it back. One conversation turn, four independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `cbo_deliver` | Sends the generated response back to the user's session/channel. |
| **GenerateResponseWorker** | `cbo_generate_response` | Crafts a contextual reply based on the detected intent and extracted entities. |
| **ReceiveWorker** | `cbo_receive` | Captures the incoming user message and loads session context. |
| **UnderstandIntentWorker** | `cbo_understand_intent` | Classifies the message intent (refund, cancellation, inquiry) and extracts entities (product name, amount). |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
cbo_receive
    │
    ▼
cbo_understand_intent
    │
    ▼
cbo_generate_response
    │
    ▼
cbo_deliver

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
java -jar target/chatbot-orchestration-1.0.0.jar

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
java -jar target/chatbot-orchestration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cbo_chatbot_orchestration \
  --version 1 \
  --input '{"userId": "TEST-001", "message": "Process this order for customer C-100", "sessionId": "TEST-001"}'d like to request a refund for my last charge", "I'd like to request a refund for my last charge": "sessionId", "sessionId": "SES-CHAT-001", "SES-CHAT-001": "sample-SES-CHAT-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cbo_chatbot_orchestration -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the conversation turn. connect your NLU service (Dialogflow, Rasa, Amazon Lex) for intent detection and your messaging platform (Intercom, Zendesk Chat) for delivery, and the chatbot workflow stays the same.

- **DeliverWorker** (`cbo_deliver`): connect to messaging platforms (Slack, WhatsApp Business API, Intercom) for real delivery
- **GenerateResponseWorker** (`cbo_generate_response`): swap in an LLM (GPT-4, Claude) for dynamic, context-aware response generation
- **ReceiveWorker** (`cbo_receive`): integrate with a session store (Redis, DynamoDB) to load and persist conversation history

Replace the demo NLU with Dialogflow or Amazon Lex and the conversation-turn pipeline continues to function unchanged.

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
chatbot-orchestration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/chatbotorchestration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ChatbotOrchestrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeliverWorker.java
│       ├── GenerateResponseWorker.java
│       ├── ReceiveWorker.java
│       └── UnderstandIntentWorker.java
└── src/test/java/chatbotorchestration/workers/
    ├── DeliverWorkerTest.java        # 2 tests
    ├── GenerateResponseWorkerTest.java        # 2 tests
    ├── ReceiveWorkerTest.java        # 2 tests
    └── UnderstandIntentWorkerTest.java        # 2 tests

```
