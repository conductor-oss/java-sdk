# Sendgrid Integration in Java Using Conductor

A Java Conductor workflow that sends personalized emails via SendGrid .  composing an email from a template, personalizing it for a specific recipient, sending it through the SendGrid API, and enabling open tracking. Given a template ID, campaign ID, and recipient details, the pipeline produces a personalized email, delivery confirmation, and tracking status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the compose-personalize-send-track pipeline.

## Sending Personalized Campaign Emails with Tracking

Sending a marketing email involves more than calling a send API. You need to load the email template, personalize it for the recipient (inserting their name, customizing the subject line, tailoring the content), send it through SendGrid, and set up tracking so you know who opens the email. Each step depends on the previous one .  you cannot personalize without a template, and you cannot track without a message ID from the send step.

Without orchestration, you would chain SendGrid API calls manually, manage template objects and message IDs between steps, and build custom tracking setup logic. Conductor sequences the pipeline and routes templates, personalized content, and message IDs between workers automatically.

## The Solution

**You just write the email workers. Template composition, recipient personalization, SendGrid delivery, and open tracking. Conductor handles template-to-tracking sequencing, SendGrid API retries, and message ID routing between send and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers handle email delivery: ComposeEmailWorker loads templates, PersonalizeWorker inserts recipient-specific content, SendEmailWorker delivers via SendGrid, and TrackOpensWorker enables engagement tracking.

| Worker | Task | What It Does |
|---|---|---|
| **ComposeEmailWorker** | `sgd_compose_email` | Composes an email from a template. |
| **PersonalizeWorker** | `sgd_personalize` | Personalizes an email for a recipient. |
| **SendEmailWorker** | `sgd_send_email` | Sends an email via SendGrid. |
| **TrackOpensWorker** | `sgd_track_opens` | Tracks email opens. |

The workers auto-detect SendGrid credentials at startup. When `SENDGRID_API_KEY` is set, SendEmailWorker uses the real SendGrid Mail Send API (v3, via `java.net.http`) to deliver emails. Without the key, it falls back to simulated mode with realistic output shapes so the workflow runs end-to-end without a SendGrid account.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sgd_compose_email
    │
    ▼
sgd_personalize
    │
    ▼
sgd_send_email
    │
    ▼
sgd_track_opens
```

## Example Output

```
=== Example 437: SendGrid Integratio ===

Step 1: Registering task definitions...
  Registered: sgd_compose_email, sgd_personalize, sgd_send_email, sgd_track_opens

Step 2: Registering workflow 'sendgrid_integration_437'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [SIMULATED][compose] Loading template
  [SIMULATED][personalize] Personalized for
  [send] Email
  [SIMULATED][track] Open tracking enabled for

  Status: COMPLETED
  Output: {template=..., subject=..., htmlBody=..., messageId=...}

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
java -jar target/sendgrid-integration-1.0.0.jar
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
| `SENDGRID_API_KEY` | _(none)_ | SendGrid API key. When set, enables live email sending via the SendGrid Mail Send API. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/sendgrid-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sendgrid_integration_437 \
  --version 1 \
  --input '{"recipientEmail": "user@example.com", "user@example.com": "recipientName", "recipientName": "Alice", "Alice": "templateId", "templateId": "tmpl-welcome-001", "tmpl-welcome-001": "campaignId", "campaignId": "camp-onboard-q1", "camp-onboard-q1": "sample-camp-onboard-q1"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sendgrid_integration_437 -s COMPLETED -c 5
```

## How to Extend

SendEmailWorker already uses the real SendGrid Mail Send API (v3, via java.net.http) when `SENDGRID_API_KEY` is provided. The remaining workers are simulated:

- **ComposeEmailWorker** (`sgd_compose_email`): use the SendGrid Dynamic Templates API to load real email templates
- **TrackOpensWorker** (`sgd_track_opens`): use SendGrid's open/click tracking settings or webhooks for real engagement tracking

Replace each simulation with real API calls while keeping the same output fields, and the compose-to-track pipeline needs no changes.

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
sendgrid-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sendgridintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SendgridIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComposeEmailWorker.java
│       ├── PersonalizeWorker.java
│       ├── SendEmailWorker.java
│       └── TrackOpensWorker.java
└── src/test/java/sendgridintegration/workers/
    ├── ComposeEmailWorkerTest.java        # 2 tests
    ├── PersonalizeWorkerTest.java        # 2 tests
    ├── SendEmailWorkerTest.java        # 2 tests
    └── TrackOpensWorkerTest.java        # 2 tests
```
