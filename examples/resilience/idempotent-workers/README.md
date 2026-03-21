# Implementing Idempotent Workers in Java with Conductor :  Safe Retry of Charges and Emails

A Java Conductor workflow example demonstrating idempotent workers.  processing a payment charge and sending a confirmation email where both operations handle duplicate execution safely, preventing double charges and duplicate emails on retry.

## The Problem

Conductor may retry a worker that actually succeeded but failed to report back (network blip between worker and Conductor). If your charge worker runs twice, the customer is double-charged. If your email worker runs twice, the customer gets duplicate confirmation emails. Every worker that has side effects must be idempotent .  producing the same result whether executed once or multiple times with the same input.

Without orchestration awareness, idempotency is an afterthought. Developers build workers without considering retries, then discover double-charge bugs in production. Each worker implements its own deduplication logic inconsistently .  some check a database flag, some use a Redis lock, some don't handle it at all.

## The Solution

**You just write the idempotent charge and notification logic. Conductor handles safe retries knowing each worker is idempotent, sequencing charge-then-notify, and tracking every execution so you can verify retry attempts produced identical results.**

Each worker is designed to be idempotent from the start. The charge worker uses the order ID as an idempotency key .  if it's already been charged, it returns the existing result. The email worker checks whether a confirmation was already sent for this order. Conductor safely retries any worker knowing the result will be the same. Every execution is tracked, so you can see retry attempts and confirm they produced identical results. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ChargeWorker uses the order ID as an idempotency key to prevent double charges on retry, and SendEmailWorker deduplicates confirmation emails so customers never receive duplicate notifications.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `idem_charge` | Idempotent charge worker .  check-before-act pattern with a Map cache. Uses orderId as the idempotency key. On the fir.. |
| **SendEmailWorker** | `idem_send_email` | Idempotent email worker .  deduplication with a Set. Uses orderId:email as the dedup key. If a notification has alread.. |

Workers simulate success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
idem_charge
    │
    ▼
idem_send_email

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
java -jar target/idempotent-workers-1.0.0.jar

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
java -jar target/idempotent-workers-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow idempotent_workers_demo \
  --version 1 \
  --input '{"orderId": "TEST-001", "amount": 100, "email": "user@example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w idempotent_workers_demo -s COMPLETED -c 5

```

## How to Extend

Each worker uses idempotency keys to prevent duplicates .  connect the charge worker to Stripe with its idempotency key support, the email worker to SES with deduplication, and the charge-then-notify workflow stays the same.

- **ChargeWorker** (`idem_charge`): integrate with Stripe's idempotency keys or PayPal's duplicate detection to ensure charges are processed exactly once
- **SendEmailWorker** (`idem_send_email`): use a sent-message log in Redis/DynamoDB keyed by order ID to skip duplicate sends, or use SES's message deduplication

Integrate with Stripe's idempotency keys and your email provider's deduplication, and the charge-then-notify pipeline handles retries safely without any workflow changes.

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
idempotent-workers/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/idempotentworkers/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IdempotentWorkersExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChargeWorker.java
│       └── SendEmailWorker.java
└── src/test/java/idempotentworkers/workers/
    ├── ChargeWorkerTest.java        # 6 tests
    └── SendEmailWorkerTest.java        # 7 tests

```
