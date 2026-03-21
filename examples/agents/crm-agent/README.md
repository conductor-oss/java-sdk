# CRM Agent in Java Using Conductor :  Customer Lookup, History Check, Record Update, Response Generation

CRM Agent. lookup customer, check history, update record, and generate response through a sequential pipeline. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Personalized Responses Require Full Customer Context

A customer contacts support saying "My order hasn't arrived." A generic response helps nobody. A good response requires knowing who they are (gold tier member, customer since 2019), what their history looks like (three prior shipping issues, last contacted two weeks ago about a different order), updating the CRM with this new interaction, and generating a response that acknowledges their loyalty and history.

Each step builds context for the next. the customer lookup provides the profile, the history check reveals patterns, the record update ensures continuity for future interactions, and the response generation uses all accumulated context. If the CRM API is slow, you need to retry the lookup without losing the original inquiry. And every interaction must be recorded for future reference.

## The Solution

**You write the customer lookup, history retrieval, record updates, and response generation. Conductor handles the CRM pipeline, retries on API failures, and interaction tracking.**

`LookupCustomerWorker` retrieves the customer profile by ID. name, tier, account status, and metadata. `CheckHistoryWorker` queries the interaction history for past issues, resolutions, and patterns. `UpdateRecordWorker` logs the current inquiry to the CRM with timestamp, channel, and inquiry details. `GenerateResponseWorker` produces a personalized response using the full customer context,  acknowledging their tier, referencing relevant history, and addressing the specific inquiry. Conductor chains these four steps and records the complete interaction context for analytics.

### What You Write: Workers

Four workers handle the CRM interaction. Looking up the customer profile, checking interaction history, updating the record, and generating a personalized response.

| Worker | Task | What It Does |
|---|---|---|
| **CheckHistoryWorker** | `cm_check_history` | Checks the interaction history for a customer, returning recent issues, total interactions, average satisfaction, sen... |
| **GenerateResponseWorker** | `cm_generate_response` | Generates a personalized response email for the customer based on their profile, tier, inquiry, recent issues, and se... |
| **LookupCustomerWorker** | `cm_lookup_customer` | Looks up a customer by ID and returns their profile information including name, email, tier, account age, contract va... |
| **UpdateRecordWorker** | `cm_update_record` | Updates the customer's CRM record with the new interaction. Creates a ticket, increments the interaction count, and r... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cm_lookup_customer
    │
    ▼
cm_check_history
    │
    ▼
cm_update_record
    │
    ▼
cm_generate_response

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
java -jar target/crm-agent-1.0.0.jar

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
java -jar target/crm-agent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow crm_agent \
  --version 1 \
  --input '{"customerId": "TEST-001", "inquiry": "sample-inquiry", "channel": "email"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w crm_agent -s COMPLETED -c 5

```

## How to Extend

Each worker handles one CRM operation. Integrate Salesforce or HubSpot for customer profiles, query real interaction histories, and use an LLM for personalized response generation, and the lookup-history-update-respond workflow runs unchanged.

- **LookupCustomerWorker** (`cm_lookup_customer`): integrate with Salesforce SOQL queries, HubSpot Contacts API, or Zendesk Users API to retrieve real customer profiles
- **CheckHistoryWorker** (`cm_check_history`): query Salesforce Case history, HubSpot Engagement API, or Intercom Conversations API for real interaction records with sentiment analysis
- **GenerateResponseWorker** (`cm_generate_response`): use GPT-4 with the customer's full context as system prompt to generate personalized, empathetic responses that reference specific history

Connect to Salesforce or HubSpot for real customer data; the CRM pipeline keeps the same lookup-update-respond interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
crm-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/crmagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CrmAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckHistoryWorker.java
│       ├── GenerateResponseWorker.java
│       ├── LookupCustomerWorker.java
│       └── UpdateRecordWorker.java
└── src/test/java/crmagent/workers/
    ├── CheckHistoryWorkerTest.java        # 8 tests
    ├── GenerateResponseWorkerTest.java        # 9 tests
    ├── LookupCustomerWorkerTest.java        # 8 tests
    └── UpdateRecordWorkerTest.java        # 9 tests

```
