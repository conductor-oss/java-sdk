# Email Agent in Java Using Conductor :  Analyze Intent, Draft, Review Tone, Send

Email Agent. analyze request, draft email, review tone, and send through a sequential pipeline. Uses [Conductor](https://github.

## Writing the Right Email in the Right Tone

"Send a follow-up to Sarah about the Q4 proposal, keep it professional but warm." This requires understanding the intent (follow-up), identifying the context (Q4 proposal discussion), drafting content that addresses the topic, and then separately reviewing the tone. is it actually professional but warm, or did it come across as cold and transactional?

Tone review is a separate cognitive task from drafting. The same content can be delivered in drastically different tones, and getting the tone wrong (too casual in a formal context, too stiff in a friendly exchange) undermines the message. By separating drafting from tone review, you can iterate on tone without regenerating the entire email, and you can catch tone mismatches before sending.

## The Solution

**You write the intent analysis, drafting, tone review, and sending logic. Conductor handles the email pipeline, version tracking, and delivery confirmation.**

`AnalyzeRequestWorker` parses the user's intent (compose, reply, follow-up), extracts the recipient, context, and desired tone from the request. `DraftEmailWorker` generates the email body with subject line based on the analyzed intent and context. `ReviewToneWorker` evaluates the draft against the desired tone, scores the match, and adjusts wording where the tone deviates. `SendEmailWorker` delivers the tone-reviewed email to the recipient. Conductor chains these four steps and records the draft, tone adjustments, and final version for quality tracking.

### What You Write: Workers

Four workers handle the email workflow. Analyzing the request intent, drafting the content, reviewing tone against the desired style, and sending the message.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeRequestWorker** | `ea_analyze_request` | Analyzes the user's email intent and extracts structured information such as email type, key points, urgency, and for... |
| **DraftEmailWorker** | `ea_draft_email` | Drafts the email subject and body based on the analyzed request, key points, recipient, and desired tone. |
| **ReviewToneWorker** | `ea_review_tone` | Reviews the drafted email's tone against the desired tone. Returns a tone score, approval status, analysis breakdown,... |
| **SendEmailWorker** | `ea_send_email` | Simulates sending the approved email. Returns a fixed message ID, timestamp, and delivery status. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ea_analyze_request
    │
    ▼
ea_draft_email
    │
    ▼
ea_review_tone
    │
    ▼
ea_send_email

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
java -jar target/email-agent-1.0.0.jar

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
java -jar target/email-agent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow email_agent \
  --version 1 \
  --input '{"intent": "sample-intent", "recipient": "sample-recipient", "context": "Process this order for customer C-100", "desiredTone": "sample-desiredTone"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w email_agent -s COMPLETED -c 5

```

## How to Extend

Each worker handles one stage of the email composition pipeline. Use an LLM for context-aware drafting, a tone analysis model for review, and SendGrid or Amazon SES for delivery, and the analyze-draft-review-send workflow runs unchanged.

- **DraftEmailWorker** (`ea_draft_email`): use GPT-4 or Claude with the conversation thread as context for replies, or integrate with email templates for standardized communications (invoices, onboarding sequences)
- **ReviewToneWorker** (`ea_review_tone`): use a dedicated tone analysis model or LLM with rubric-based scoring (formality 1-5, warmth 1-5, urgency 1-5) and automatic rewording of flagged sentences
- **SendEmailWorker** (`ea_send_email`): integrate with SendGrid, Amazon SES, or Gmail API for actual delivery, with tracking pixels for open rates and link click tracking

Plug in an LLM for drafting and a real SMTP/SES sender; the email pipeline keeps the same analyze-draft-review-send interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

A Java Conductor workflow that handles email composition end-to-end. analyzing the user's intent and context, drafting the email with appropriate content, reviewing and adjusting the tone (formal, friendly, urgent), and sending the final version. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step email pipeline as independent workers,  you write the drafting and tone-review logic, Conductor handles sequencing, retries, durability, and observability.

## Project Structure

```
email-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/emailagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EmailAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeRequestWorker.java
│       ├── DraftEmailWorker.java
│       ├── ReviewToneWorker.java
│       └── SendEmailWorker.java
└── src/test/java/emailagent/workers/
    ├── AnalyzeRequestWorkerTest.java        # 8 tests
    ├── DraftEmailWorkerTest.java        # 8 tests
    ├── ReviewToneWorkerTest.java        # 9 tests
    └── SendEmailWorkerTest.java        # 8 tests

```
