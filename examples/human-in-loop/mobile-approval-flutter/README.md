# Mobile Approval with Flutter in Java Using Conductor :  Request Submission, FCM Push Notification, WAIT for Mobile Response, and Finalization

A Java Conductor workflow example for mobile-first approvals .  submitting a request, sending a push notification via Firebase Cloud Messaging (FCM) to the approver's mobile device, pausing at a WAIT task until the Flutter app sends back the decision, and finalizing the result. Demonstrates how approvers on the go can approve directly from their phone via push notification. Uses [Conductor](https://github.## Mobile Users Need Push Notifications for Approvals

When an approval is needed, the approver might not be at their desk. The workflow submits the request, sends a push notification to the approver's mobile device, and pauses at a WAIT task until the mobile app sends back the decision. If the push notification fails to send, you need to retry it without re-submitting the request. And you need to track whether the approval came via mobile or another channel.

## The Solution

**You just write the request-submission, push-notification, and finalization workers. Conductor handles the durable wait for the mobile response.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging .  your code handles the decision logic.

### What You Write: Workers

MobSubmitWorker validates the request, MobSendPushWorker delivers the FCM notification, and MobFinalizeWorker records the mobile decision. None of them poll for the Flutter app's response.

| Worker | Task | What It Does |
|---|---|---|
| **MobSubmitWorker** | `mob_submit` | Submits the approval request .  validates the request and identifies the approver's device token for push delivery |
| **MobSendPushWorker** | `mob_send_push` | Sends an FCM push notification to the approver's mobile device with the request details and approve/reject action buttons |
| *WAIT task* | `mob_approval` | Pauses until the Flutter app sends the approver's decision back via `POST /tasks/{taskId}` when they tap approve or reject in the push notification | Built-in Conductor WAIT .  no worker needed |
| **MobFinalizeWorker** | `mob_finalize` | Finalizes the approval .  records the decision, the channel (mobile), and triggers downstream actions |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### The Workflow

```
mob_submit
    │
    ▼
mob_send_push
    │
    ▼
mobile_response [WAIT]
    │
    ▼
mob_finalize
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
java -jar target/mobile-approval-flutter-1.0.0.jar
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
java -jar target/mobile-approval-flutter-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mobile_approval_flutter \
  --version 1 \
  --input '{"requestId": "TEST-001", "userId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mobile_approval_flutter -s COMPLETED -c 5
```

## How to Extend

Each worker handles one step of the mobile approval flow .  connect Firebase Cloud Messaging for push delivery and your Flutter app's callback for decision capture, and the mobile-approval workflow stays the same.

- **MobFinalizeWorker** (`mob_finalize`): push the approval decision to downstream systems, update the record status, and send confirmation to the requester
- **MobSendPushWorker** (`mob_send_push`): send real push notifications via Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNS), or a service like OneSignal
- **MobSubmitWorker** (`mob_submit`): enrich the submission with context from your business systems and determine the correct approver based on org chart rules

Replace the simulated push notification with Firebase Cloud Messaging and the mobile approval flow: push, wait, finalize, keeps working as defined.

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
mobile-approval-flutter/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/mobileapprovalflutter/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MobileApprovalFlutterExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MobFinalizeWorker.java
│       ├── MobSendPushWorker.java
│       └── MobSubmitWorker.java
└── src/test/java/mobileapprovalflutter/workers/
    ├── MobFinalizeWorkerTest.java        # 6 tests
    ├── MobSendPushWorkerTest.java        # 5 tests
    └── MobSubmitWorkerTest.java        # 5 tests
```
