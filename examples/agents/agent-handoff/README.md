# Agent Handoff in Java Using Conductor -- Triage Customer Messages and Route to Specialist Agents

A customer writes "I was charged twice" and your first-line agent can't figure out whether that's billing or technical -- so the customer gets bounced to tech support, who says "not my department," then back to billing, who asks the customer to repeat everything from scratch. Three handoffs, zero resolution, one furious customer. This example uses [Conductor](https://github.com/conductor-oss/conductor) to classify the message once with a triage worker, then route it through a `SWITCH` task to the right specialist (billing, technical, or general) -- no if/else spaghetti, no misroutes, and a full audit trail of which agent handled every message.

## The Problem

A customer writes "I was charged twice for my subscription." That needs a billing specialist who can look up charges, initiate refunds, and send confirmation emails. If it goes to a general agent, the customer gets a generic response and has to repeat themselves. If it goes to tech support, the agent wastes time looking for a technical problem that doesn't exist.

Triage requires understanding the message content (billing keyword detection, error code recognition, general inquiry classification), assigning a confidence score and urgency level, then routing to the right specialist with that context attached. The billing agent needs the customer ID, the urgency level, and the triage notes. The tech agent needs the same metadata plus root-cause diagnostics. Without orchestration, you'd build nested if/else routing with each specialist hardcoded into the triage logic -- making it impossible to add a new specialist without modifying the triage code.

## The Solution

**You write the triage logic and specialist handlers. Conductor handles routing, retries, and message tracking.**

`TriageWorker` classifies the customer message into a category (billing, technical, or general) with a deterministic confidence score based on keyword match count, urgency level (normal or high), and matched keyword list. Conductor's `SWITCH` task routes to the matching specialist: `BillingWorker` handles refund processing and dispute resolution, `TechWorker` diagnoses infrastructure and API issues with root-cause analysis, and `GeneralWorker` handles plan inquiries, cancellations, and everything else. Each specialist receives the customer ID, original message, triage notes, and urgency level -- and produces a deterministic ticket ID, resolution, and action list based on the input. Conductor records which specialist handled each message and what resolution was provided.

### What You Write: Workers

The triage worker classifies incoming messages, and Conductor routes them to the appropriate specialist -- billing, technical, or general support.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **TriageWorker** | `ah_triage` | Classifies message by keyword matching against technical (api, error, timeout, crash, bug, 500, latency, outage) and billing (bill, charge, invoice, refund, payment, subscription, overcharged, credit) keyword sets. Returns category, confidence (0.6-0.95 based on match count), urgency, and matched keywords. | Real -- deterministic keyword classification |
| **BillingWorker** | `ah_billing` | Routes billing issues by message content: refund requests get charge review and refund initiation; invoice requests get document retrieval; other billing inquiries get account review. Returns a deterministic ticket ID, resolution, and action list. High-urgency tickets are flagged for review. | Simulated -- swap in Stripe/PayPal API for production |
| **TechWorker** | `ah_tech` | Performs root-cause analysis by message content: rate limiting gets quota increase, timeouts get connection pool restart, 500 errors get deployment rollback, unclassified issues get escalation to engineering. Returns diagnostics with latency, error rate, and root cause. High-urgency tickets get P1 priority. | Simulated -- swap in PagerDuty/Datadog API for production |
| **GeneralWorker** | `ah_general` | Routes general inquiries by message content: plan/pricing questions get comparison details and upgrade recommendations, cancellation requests get retention callbacks, everything else gets a follow-up. | Simulated -- swap in CRM/knowledge base API for production |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call -- the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Conditional routing** | `SWITCH` task routes to billing, technical, or general specialist based on triage output -- no if/else chains in your code |
| **Retries with backoff** | If a specialist worker fails (network blip, timeout), Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every triage decision and specialist resolution is tracked with inputs, outputs, timing, and status -- routing analytics without custom logging |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ah_triage
    |
    v
SWITCH (route_to_specialist_ref)
    |-- billing: ah_billing
    |-- technical: ah_tech
    +-- default: ah_general
```

## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/agent-handoff-1.0.0.jar
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

## Example Output

```
=== Agent Handoff Demo: Triage and Route to Specialist ===

Step 1: Registering task definitions...
  Registered: ah_triage, ah_billing, ah_tech, ah_general

Step 2: Registering workflow 'agent_handoff'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  [ah_triage] Classifying message from customer CUST-9042
  [ah_tech] Handling technical issue for customer CUST-9042

  Workflow ID: 9a1b2c3d-4e5f-6789-abcd-ef0123456789

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {category=technical, confidence=0.8, resolution=Detected elevated API latency for CUST-9042. ..., ticketId=TECH-5738}

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/agent-handoff-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Route a billing issue
conductor workflow start \
  --workflow agent_handoff \
  --version 1 \
  --input '{"customerId": "CUST-4420", "message": "I was charged twice for my subscription and need a refund"}'

# Route a technical issue
conductor workflow start \
  --workflow agent_handoff \
  --version 1 \
  --input '{"customerId": "CUST-8111", "message": "API timeout errors on every request for the past hour"}'

# Route a general inquiry
conductor workflow start \
  --workflow agent_handoff \
  --version 1 \
  --input '{"customerId": "CUST-1058", "message": "What pricing plans do you offer for teams?"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agent_handoff -s COMPLETED -c 5
```

## How to Extend

Each worker encapsulates one agent specialization -- replace the simulated responses with real LLM classifiers (Dialogflow, OpenAI) and domain-specific APIs (Stripe, PagerDuty, Zendesk), and the triage-to-specialist routing runs unchanged.

- **TriageWorker** (`ah_triage`) -- replace keyword matching with an LLM classifier (OpenAI function calling to return structured `{category, confidence}` JSON, or LangChain `RouterChain` for multi-agent dispatch), or integrate with Dialogflow CX for intent detection with built-in entity extraction and context management
- **BillingWorker** (`ah_billing`) -- connect to Stripe's Refund API or PayPal's Disputes API to process refunds directly, query billing databases for charge history, and send confirmation via SendGrid
- **TechWorker** (`ah_tech`) -- integrate with PagerDuty for incident lookup, query Datadog/New Relic for recent error spikes matching the customer's issue, and check service status pages programmatically
- **GeneralWorker** (`ah_general`) -- connect to a knowledge base API (Zendesk Guide, Confluence) for automated FAQ responses, or integrate with a CRM (Salesforce, HubSpot) to pull customer context before responding
- **Add a new specialist** -- create a new worker class, add a case to the `SWITCH` in `workflow.json`, and update the triage keywords. No existing code changes needed.

Replace the simulated specialist logic with real CRM and billing APIs; the routing workflow preserves the same handoff interface.

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
agent-handoff/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agenthandoff/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgentHandoffExample.java     # Main entry point (supports --workers mode)
│   └── workers/
│       ├── TriageWorker.java        # Keyword classification with confidence scoring
│       ├── BillingWorker.java       # Refund, invoice, and account review routing
│       ├── TechWorker.java          # Root-cause analysis with diagnostics
│       └── GeneralWorker.java       # Plan inquiries, cancellations, general follow-up
└── src/test/java/agenthandoff/workers/
    ├── TriageWorkerTest.java        # 23 tests
    ├── BillingWorkerTest.java       # 13 tests
    ├── TechWorkerTest.java          # 12 tests
    └── GeneralWorkerTest.java       # 11 tests
```
