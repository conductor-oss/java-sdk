# End-to-End App in Java with Conductor: Support Ticket Pipeline. Classify, Assign, Notify

A complete Java Conductor application that processes support tickets end-to-end: classifying the ticket by category (billing, technical, account), assigning it to the right team based on classification, and notifying the customer with a confirmation email. This is a realistic mini-application that shows how all the Conductor pieces fit together: workflow definition, multiple workers, data flow between tasks, and a main entry point. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the ticket pipeline, you write the classification, assignment, and notification logic, Conductor handles sequencing, retries, durability, and observability.

## From Hello World to a Real Application

After learning the basics (workers, workflows, data flow), you need to see how all the pieces come together in a realistic application. This support ticket pipeline is small enough to understand completely but real enough to demonstrate production patterns: a workflow that processes business data, workers that make decisions based on input, and data flowing from classification through assignment to notification.

## The Solution

**You just write the ticket classification, team assignment, and customer notification logic. Conductor handles sequencing, retries, durability, and observability.**

Three workers handle the ticket lifecycle. Classification (determining category from subject and description), assignment (routing to the right team), and notification (confirming receipt to the customer). Conductor sequences them, passing the ticket ID, classification result, and assignment through the pipeline.

### What You Write: Workers

The support ticket pipeline uses three workers: classify, assign, and notify, to show how a complete Conductor application fits together from registration to execution.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyTicketWorker** | `classify_ticket` | Scans subject and description for priority keywords (e.g., "down" or "outage" = CRITICAL, "question" = LOW). Returns a priority level and the keywords that matched. |
| **AssignTicketWorker** | `assign_ticket` | Maps category to team (billing -> Finance Support, technical -> Engineering) and upgrades response time for CRITICAL (30 min) and HIGH (1 hour) tickets. |
| **NotifyCustomerWorker** | `notify_customer` | Logs a notification to the customer with ticket details (priority, assigned team, response time). Returns `sent: true` and `channel: email`. |

### The Workflow

```
classify_ticket
    │
    ▼
assign_ticket
    │
    ▼
notify_customer

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
java -jar target/end-to-end-app-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

### Example Output

```
=== Support Ticket Pipeline: End-to-End Application ===

Step 1: Registering task definitions...
  Registered: classify_ticket, assign_ticket, notify_customer

Step 2: Registering workflow 'support_ticket_pipeline'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Submitting 3 support tickets...

  Submitted TKT-001. Workflow ID: <workflow-id>
  Submitted TKT-002. Workflow ID: <workflow-id>
  Submitted TKT-003. Workflow ID: <workflow-id>

Step 5: Waiting for all tickets to be processed...

=== Ticket Processing Summary ===

  Ticket     | Priority   | Team                   | Response Time
  ---------- | ---------- | ---------------------- | ---------------
  TKT-001    | CRITICAL   | Engineering            | 30 minutes
  TKT-002    | LOW        | Finance Support        | 4 hours
  TKT-003    | MEDIUM     | Account Management     | 8 hours

Result: PASSED. All 3 tickets processed successfully.

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/end-to-end-app-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow support_ticket_pipeline \
  --version 1 \
  --input '{"ticketId": "TKT-100", "subject": "Cannot login to my account", "description": "Password reset is not working", "category": "account", "customerEmail": "user@example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w support_ticket_pipeline -s COMPLETED -c 5

```

## How to Extend

- **ClassifyTicketWorker** (`classify_ticket`): replace keyword matching with an ML classifier or call an NLP API to determine ticket priority from natural language.
- **AssignTicketWorker** (`assign_ticket`): query a team roster database for on-call engineers, or integrate with PagerDuty / Jira to create and assign issues.
- **NotifyCustomerWorker** (`notify_customer`): send a real email via SendGrid / SES, or post a message to the customer's Slack channel.

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
end-to-end-app/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/endtoendapp/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SupportTicketExample.java    # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignTicketWorker.java
│       ├── ClassifyTicketWorker.java
│       └── NotifyCustomerWorker.java
└── src/test/java/endtoendapp/workers/
    ├── AssignTicketWorkerTest.java
    ├── ClassifyTicketWorkerTest.java
    └── NotifyCustomerWorkerTest.java

```
