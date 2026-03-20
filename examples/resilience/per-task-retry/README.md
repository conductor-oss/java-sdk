# Implementing Per-Task Retry Configuration in Java with Conductor :  Different Retry Strategies per Step

A Java Conductor workflow example demonstrating per-task retry configuration .  each task in the workflow has its own retry count, retry logic (FIXED or EXPONENTIAL_BACKOFF), and retry delay, allowing fine-grained control over how each step handles transient failures.

## The Problem

Different steps in your pipeline have different failure characteristics. A payment API needs exponential backoff (to avoid hammering a rate-limited endpoint), a validation step needs just 1 fixed retry (data errors won't fix themselves), and a notification step needs 3 retries with short delays (email servers recover quickly). A one-size-fits-all retry policy wastes time on some steps and gives up too early on others.

Without orchestration, per-step retry configuration means building separate retry loops for every step, each with its own delay calculation, max attempt tracking, and backoff strategy. When retry requirements change, you modify business logic code rather than configuration.

## The Solution

**You just write the task logic and declare each task's retry strategy in config. Conductor handles per-task retry timing with configurable strategy (FIXED or EXPONENTIAL_BACKOFF), attempt counting, backoff calculation, and a complete retry history for each step showing delays and outcomes.**

Each task defines its own retry configuration in the workflow definition .  retry count, retry logic (FIXED or EXPONENTIAL_BACKOFF), and delay seconds. The workers just do their job and return success or failure. Conductor handles the retry timing, attempt counting, and backoff calculation per task. Changing a step's retry strategy is a JSON config change, not a code change. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

PtrValidate uses 1 fixed retry for data errors that won't self-resolve, PtrPayment uses 5 retries with exponential backoff for rate-limited payment APIs, and PtrNotify uses 3 fixed retries with short delays for transient email delivery failures.

| Worker | Task | What It Does |
|---|---|---|
| **PtrNotify** | `ptr_notify` | Sends notification for an order. Configured with retryCount:3, FIXED retry, 2s delay. Returns { result: "email_sent",... |
| **PtrPayment** | `ptr_payment` | Processes payment for an order. Configured with retryCount:5, EXPONENTIAL_BACKOFF, 1s base delay. Returns { result: "... |
| **PtrValidate** | `ptr_validate` | Validates an order. Configured with retryCount:1, FIXED retry, 1s delay. Returns { result: "valid", orderId: ... } |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
ptr_validate
    │
    ▼
ptr_payment
    │
    ▼
ptr_notify
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
java -jar target/per-task-retry-1.0.0.jar
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
java -jar target/per-task-retry-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow per_task_retry_demo \
  --version 1 \
  --input '{"orderId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w per_task_retry_demo -s COMPLETED -c 5
```

## How to Extend

Each worker calls a real service with its own retry needs .  connect the payment worker to Stripe with exponential backoff, the notification worker to SES with fixed retries, and the per-task retry configuration stays the same.

- **PtrNotify** (`ptr_notify`): send real notifications via email/SMS/push .  short fixed retries handle temporary delivery failures
- **PtrPayment** (`ptr_payment`): integrate with your payment gateway (Stripe, PayPal) .  the exponential backoff prevents hammering rate-limited endpoints
- **PtrValidate** (`ptr_validate`): replace with your real validation logic .  schema validation, business rule checks, input sanitization

Swap in your real payment gateway and notification service, and adjusting retry strategies is a JSON config change without touching any worker code.

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
per-task-retry/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/pertaskretry/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PerTaskRetryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PtrNotify.java
│       ├── PtrPayment.java
│       └── PtrValidate.java
└── src/test/java/pertaskretry/workers/
    ├── PtrNotifyTest.java        # 4 tests
    ├── PtrPaymentTest.java        # 5 tests
    └── PtrValidateTest.java        # 4 tests
```
