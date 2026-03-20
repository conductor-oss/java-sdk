# Webhook Security in Java Using Conductor

Webhook security workflow that computes an HMAC signature, verifies it against the provided signature, and routes to process or reject the webhook via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to verify the authenticity of incoming webhooks using HMAC signatures. The workflow computes an HMAC hash of the payload using the shared secret, compares it to the signature provided by the sender, and routes to processing (if valid) or rejection (if forged). Without signature verification, an attacker can send forged webhooks that trigger unauthorized actions in your system.

Without orchestration, you'd embed HMAC verification in middleware, manually computing hashes, comparing signatures with timing-safe equality, handling missing or malformed signatures, and logging every verification result for security audit .  hoping the verification is never accidentally bypassed by a code change.

## The Solution

**You just write the HMAC-compute, signature-verify, process, and reject workers. Conductor handles SWITCH-based accept/reject routing, guaranteed verification before processing, and a security audit trail for every webhook.**

Each security concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of computing the HMAC, verifying the signature, routing via a SWITCH task to process (valid) or reject (invalid), and tracking every webhook's verification result for security audit. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers verify webhook authenticity: ComputeHmacWorker generates the expected signature, VerifySignatureWorker compares it to the sender's value, ProcessWebhookWorker handles verified payloads, and RejectWebhookWorker blocks forged requests.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeHmacWorker** | `ws_compute_hmac` | Computes an HMAC signature for a webhook payload. |
| **ProcessWebhookWorker** | `ws_process_webhook` | Processes a verified webhook payload. |
| **RejectWebhookWorker** | `ws_reject_webhook` | Rejects a webhook with an invalid signature. |
| **VerifySignatureWorker** | `ws_verify_signature` | Verifies a webhook signature by comparing expected vs provided values. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
ws_compute_hmac
    │
    ▼
ws_verify_signature
    │
    ▼
SWITCH (validity_switch_ref)
    ├── valid: ws_process_webhook
    ├── invalid: ws_reject_webhook
```

## Example Output

```
=== Webhook Security Workflow Demo ===

Step 1: Registering task definitions...
  Registered: ws_compute_hmac, ws_verify_signature, ws_process_webhook, ws_reject_webhook

Step 2: Registering workflow 'webhook_security_wf'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ws_compute_hmac] Computing HMAC for payload length:
  [ws_process_webhook] Processing verified webhook payload...
  [ws_reject_webhook] Rejecting webhook:
  [ws_verify_signature] Comparing signatures...

  Status: COMPLETED
  Output: {computedSignature=..., algorithm=..., processed=..., rejected=...}

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
java -jar target/webhook-security-1.0.0.jar
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
java -jar target/webhook-security-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow webhook_security_wf \
  --version 1 \
  --input '{"payload": "{\"event\":\"push\",\"repo\":\"my-repo\"}", "secret": "webhook-secret-key", "providedSignature": "hmac_sha256_fixedvalue"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w webhook_security_wf -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real HMAC implementation (javax.crypto.Mac with secrets from AWS KMS or Vault), timing-safe signature comparison, and webhook processing logic, the compute-verify-route security workflow stays exactly the same.

- **HMAC computer**: implement HMAC-SHA256 computation using Java's javax.crypto.Mac with the shared secret from your key management service (AWS KMS, HashiCorp Vault)
- **Signature verifier**: use timing-safe comparison (MessageDigest.isEqual) to prevent timing attacks; support multiple signature algorithms (SHA-256, SHA-512)
- **Webhook processor**: implement your actual webhook processing logic for verified requests
- **Rejection handler**: log rejected webhooks with sender IP and payload hash for security monitoring; alert on repeated forgery attempts

Switching the HMAC algorithm or connecting ProcessWebhookWorker to real business logic preserves the compute-verify-route security flow.

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
webhook-security/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webhooksecurity/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebhookSecurityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ComputeHmacWorker.java
│       ├── ProcessWebhookWorker.java
│       ├── RejectWebhookWorker.java
│       └── VerifySignatureWorker.java
└── src/test/java/webhooksecurity/workers/
    ├── ComputeHmacWorkerTest.java        # 9 tests
    ├── ProcessWebhookWorkerTest.java        # 8 tests
    ├── RejectWebhookWorkerTest.java        # 8 tests
    └── VerifySignatureWorkerTest.java        # 9 tests
```
