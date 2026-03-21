# Implementing Error Notification in Java with Conductor :  Order Processing with Automated Failure Alerts

A Java Conductor workflow example demonstrating error notification .  processing orders with a failure workflow that automatically triggers parallel alerts via Slack, email, and PagerDuty when the order processing step fails.

## The Problem

You have an order processing pipeline, and when it fails, the right people need to know immediately .  the on-call engineer via PagerDuty, the ops team via Slack, and the account manager via email. The notification must happen automatically and in parallel, because a slow email send shouldn't delay the PagerDuty page.

Without orchestration, failure notifications are afterthoughts .  a catch block that sends one email and hopes it works. Adding Slack means another catch block. Adding PagerDuty means another. If the Slack webhook times out, the PagerDuty page never fires. Nobody knows which notifications actually sent for a given failure.

## The Solution

**You just write the order processor and notification channel integrations. Conductor handles failure detection, parallel notification dispatch across all channels, retries on delivery failures, and a record of which alerts were sent for every failed order.**

The order processing worker handles business logic. When it fails, Conductor's failure workflow automatically triggers, running Slack, email, and PagerDuty notifications in parallel. Even if one notification channel is down, the others still fire. Every notification attempt is tracked .  you can see which alerts were sent, which failed, and how long each took. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ProcessOrderWorker handles the business logic, and when it fails, Conductor's failure workflow automatically fires SendSlackWorker, SendEmailWorker, and SendPagerDutyWorker in parallel so all channels are notified simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessOrderWorker** | `en_process_order` | Worker for en_process_order. Processes an order. If the input contains shouldFail=true, the task returns FAILED sta.. |
| **SendEmailWorker** | `en_send_email` | Worker for en_send_email. Sends an email notification. Returns sent=true and the recipient address from input (defa.. |
| **SendPagerDutyWorker** | `en_send_pagerduty` | Worker for en_send_pagerduty. Sends a PagerDuty alert. Returns sent=true. |
| **SendSlackWorker** | `en_send_slack` | Worker for en_send_slack. Sends a Slack notification. Returns sent=true and the channel from input (defaults to "#a.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
en_process_order

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
java -jar target/error-notification-1.0.0.jar

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
java -jar target/error-notification-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow order_with_alerts \
  --version 1 \
  --input '{"shouldFail": "sample-shouldFail"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w order_with_alerts -s COMPLETED -c 5

```

## How to Extend

Each worker sends to one notification channel .  connect the Slack worker to your webhook, the email worker to SES or SendGrid, the PagerDuty worker to your integration key, and the parallel-notify-on-failure workflow stays the same.

- **ProcessOrderWorker** (`en_process_order`): connect to your real order processing system .  validate inventory, charge payment, create shipment
- **SendEmailWorker** (`en_send_email`): send via SendGrid/SES with templated failure reports including stack traces and remediation steps
- **SendPagerDutyWorker** (`en_send_pagerduty`): create real PagerDuty incidents via Events API v2 with severity levels based on error type

Wire each notification worker to your real Slack webhook, SES account, and PagerDuty integration key, and the parallel alert pipeline works without any orchestration changes.

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
error-notification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/errornotification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ErrorNotificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ProcessOrderWorker.java
│       ├── SendEmailWorker.java
│       ├── SendPagerDutyWorker.java
│       └── SendSlackWorker.java
└── src/test/java/errornotification/workers/
    ├── ProcessOrderWorkerTest.java        # 8 tests
    ├── SendEmailWorkerTest.java        # 4 tests
    ├── SendPagerDutyWorkerTest.java        # 3 tests
    └── SendSlackWorkerTest.java        # 4 tests

```
