# Notification Preferences in Java Using Conductor

A Java Conductor workflow example demonstrating Notification Preferences. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

A user wants to change their notification settings. Enabling SMS alerts and disabling push notifications. The system needs to load their current preferences (email, SMS, push, Slack), merge the new selections with existing ones, sync the updated channel configuration to all downstream notification services, and send a confirmation that the changes took effect. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the preference-loading, merging, channel-sync, and confirmation workers. Conductor handles the preference update pipeline and multi-service propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

LoadPrefsWorker reads current channel settings, UpdatePrefsWorker merges new selections, SyncChannelsWorker configures each notification service, and ConfirmPrefsWorker notifies the user that changes took effect.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmPrefsWorker** | `np_confirm` | Sends a confirmation notification to the user that their preference changes took effect |
| **LoadPrefsWorker** | `np_load` | Loads the user's current notification preferences across all channels (email, SMS, push, Slack) |
| **SyncChannelsWorker** | `np_sync_channels` | Identifies which channels are now active and syncs the configuration to each notification service |
| **UpdatePrefsWorker** | `np_update` | Merges the new preference selections with existing ones and persists the updated configuration |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
np_load
    │
    ▼
np_update
    │
    ▼
np_sync_channels
    │
    ▼
np_confirm
```

## Example Output

```
=== Example 606: Notification Preferences ===

Step 1: Registering task definitions...
  Registered: np_load, np_update, np_sync_channels, np_confirm

Step 2: Registering workflow 'np_notification_preferences'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [confirm] Confirmation sent for updated notification preferences
  [load] Loading current preferences for
  [sync] Synced
  [update] Preferences updated:

  Status: COMPLETED
  Output: {confirmed=..., current=..., syncedChannels=..., syncedAt=...}

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
java -jar target/notification-preferences-1.0.0.jar
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
java -jar target/notification-preferences-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow np_notification_preferences \
  --version 1 \
  --input '{"userId": "USR-NP001", "USR-NP001": "preferences", "preferences": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w np_notification_preferences -s COMPLETED -c 5
```

## How to Extend

Each worker handles one preference step .  connect your notification services (Twilio for SMS, Firebase for push, Slack API) for channel sync and your user store for preference persistence, and the notification-preferences workflow stays the same.

- **LoadPrefsWorker** (`np_load`): query the user's current notification preferences from your database or user profile service (Auth0, Cognito user attributes)
- **UpdatePrefsWorker** (`np_update`): merge the new preferences with existing ones and persist the updated record to your database or identity provider
- **SyncChannelsWorker** (`np_sync_channels`): propagate channel settings to SendGrid (email), Twilio (SMS), Firebase Cloud Messaging (push), and Slack API based on which channels are active
- **ConfirmPrefsWorker** (`np_confirm`): send a confirmation notification via the user's preferred active channel using SendGrid, Twilio, or push notification service

Connect your real notification services (Twilio, FCM, Slack) and the load-merge-sync-confirm preference pipeline operates unchanged.

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
notification-preferences/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/notificationpreferences/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NotificationPreferencesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmPrefsWorker.java
│       ├── LoadPrefsWorker.java
│       ├── SyncChannelsWorker.java
│       └── UpdatePrefsWorker.java
└── src/test/java/notificationpreferences/workers/
    ├── ConfirmPrefsWorkerTest.java        # 3 tests
    ├── LoadPrefsWorkerTest.java        # 3 tests
    ├── SyncChannelsWorkerTest.java        # 4 tests
    └── UpdatePrefsWorkerTest.java        # 4 tests
```
