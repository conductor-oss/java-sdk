# Event Notification in Java Using Conductor

A customer's payment fails. They need to know immediately: via email, SMS, and push. But your notification code sends them sequentially: email first, then SMS, then push. The SMS provider is having a bad day and hangs for 30 seconds before timing out. Now your push notification arrives a minute late, and the email, which actually succeeded, is sitting in a retry loop because the whole pipeline is blocked. The customer sees nothing, retries the payment, gets double-charged, and opens a support ticket. This workflow sends notifications across all channels in parallel, so a slow SMS never blocks the email, and every delivery attempt is tracked. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to send notifications about events across multiple channels simultaneously. When an event occurs, the recipient must be notified via email, SMS, and push notification, all in parallel to minimize delivery latency. After all channels complete, the delivery status for each channel must be recorded for tracking and compliance. A single slow channel (e.g., email SMTP delays) should not block the other channels.

Without orchestration, you'd spawn threads for each notification channel, manage per-channel retry logic with different backoff strategies, aggregate delivery receipts from multiple providers, and handle partial failures where email succeeds but SMS fails.

## The Solution

**You just write the event-parse, email, SMS, push, and delivery-recording workers. Conductor handles parallel multi-channel delivery, per-channel retry isolation, and unified delivery status tracking.**

Each notification channel is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of parsing the event, sending across all three channels in parallel via FORK_JOIN, recording delivery status for each, retrying any failed channel independently, and tracking the entire notification lifecycle. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers deliver multi-channel notifications: ParseEventWorker extracts recipient and content, then SendEmailWorker, SendSmsWorker, and SendPushWorker deliver in parallel via FORK_JOIN, and RecordDeliveryWorker logs the outcome per channel.

| Worker | Task | What It Does |
|---|---|---|
| **ParseEventWorker** | `en_parse_event` | Parses an incoming event and prepares notification content for all channels. |
| **RecordDeliveryWorker** | `en_record_delivery` | Records the delivery status across all notification channels. |
| **SendEmailWorker** | `en_send_email` | Sends an email notification to a recipient. |
| **SendPushWorker** | `en_send_push` | Sends a push notification to a recipient. |
| **SendSmsWorker** | `en_send_sms` | Sends an SMS notification to a recipient. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
en_parse_event
    │
    ▼
FORK_JOIN
    ├── en_send_email
    ├── en_send_sms
    └── en_send_push
    │
    ▼
JOIN (wait for all branches)
en_record_delivery

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
java -jar target/event-notification-1.0.0.jar

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
java -jar target/event-notification-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_notification \
  --version 1 \
  --input '{"event": {"type": "order.shipped", "message": "Your order has been shipped!"}, "recipientId": "user-9001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_notification -s COMPLETED -c 5

```

## How to Extend

Connect each channel worker to your real email (SES, SendGrid), SMS (Twilio), and push notification (Firebase, APNs) services, the parse-send-record notification workflow stays exactly the same.

- **Email sender**: send via SendGrid, AWS SES, or SMTP with template rendering and attachment support
- **SMS sender**: send via Twilio, Vonage, or AWS SNS with phone number validation and opt-out checking
- **Push notification sender**: send via Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNS)
- **Delivery recorder**: persist delivery receipts to your notification tracking database for compliance and analytics

Connecting SendEmailWorker to SES, SendSmsWorker to Twilio, or SendPushWorker to Firebase requires no workflow changes.

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
event-notification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventnotification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventNotificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ParseEventWorker.java
│       ├── RecordDeliveryWorker.java
│       ├── SendEmailWorker.java
│       ├── SendPushWorker.java
│       └── SendSmsWorker.java
└── src/test/java/eventnotification/workers/
    ├── ParseEventWorkerTest.java        # 9 tests
    ├── RecordDeliveryWorkerTest.java        # 9 tests
    ├── SendEmailWorkerTest.java        # 8 tests
    ├── SendPushWorkerTest.java        # 8 tests
    └── SendSmsWorkerTest.java        # 8 tests

```
