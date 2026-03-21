# Email Verification in Java Using Conductor

A Java Conductor workflow example demonstrating Email Verification. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to verify a user's email address after registration. Generating a unique verification code, sending it to the user's email, waiting for them to submit the code, checking that the submitted code matches the expected one, and activating their account only if verification succeeds. Each step depends on the previous one's output.

If the code is sent but the verification step compares against a stale or wrong expected code, legitimate users get locked out of their own accounts. If verification succeeds but account activation fails, the user sees "verified" but can't log in. Without orchestration, you'd build a monolithic verification handler that mixes code generation, email delivery, code comparison, and account state updates. Making it impossible to add code expiration, support SMS as an alternative channel, or track how long users take to verify for conversion analytics.

## The Solution

**You just write the code-generation, code-verification, and account-activation workers. Conductor handles the verification sequence and code-matching data flow.**

SendCodeWorker generates a unique verification code and emails it to the user's address, returning the expected code for later comparison. WaitInputWorker simulates the user receiving the email and submitting their verification code. In production, this would be a WAIT task that pauses until the user clicks the verification link or enters the code. VerifyCodeWorker compares the submitted code against the expected code and returns whether they match. ActivateAccountWorker flips the user's account status to active only if verification succeeded, completing the email ownership proof. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

SendCodeWorker generates and emails a verification code, WaitInputWorker simulates the user submitting it, VerifyCodeWorker compares codes, and ActivateAccountWorker flips the account to active on match.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateAccountWorker** | `emv_activate` | Activates the user account after email verification. |
| **SendCodeWorker** | `emv_send_code` | Sends a verification code to the user's email. |
| **VerifyCodeWorker** | `emv_verify` | Verifies the submitted code against the expected code. |
| **WaitInputWorker** | `emv_wait_input` | Simulates waiting for user to submit their verification code. |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
emv_send_code
    │
    ▼
emv_wait_input
    │
    ▼
emv_verify
    │
    ▼
emv_activate

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
java -jar target/email-verification-1.0.0.jar

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
java -jar target/email-verification-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow emv_email_verification \
  --version 1 \
  --input '{"email": "user@example.com", "userId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w emv_email_verification -s COMPLETED -c 5

```

## How to Extend

Each worker handles one verification step .  connect your email delivery service (SendGrid, SES, Mailgun) for code sending and your user store for account activation, and the verification workflow stays the same.

- **SendCodeWorker** (`emv_send_code`): generate a cryptographically secure code, store it in Redis with a TTL for expiration, and send it via SendGrid, SES, or Mailgun with a branded verification email template
- **WaitInputWorker** (`emv_wait_input`): replace with a Conductor WAIT task that pauses the workflow until the user clicks the verification link or submits the code via your API, which completes the task via the Conductor REST API
- **VerifyCodeWorker** (`emv_verify`): compare the submitted code against the stored code in Redis, check expiration, and enforce a maximum number of verification attempts to prevent brute-force guessing
- **ActivateAccountWorker** (`emv_activate`): update the user's status in your identity provider (Auth0, Cognito, Okta) and your application database, triggering any post-activation workflows like welcome emails or onboarding flows

Connect SendGrid for real email delivery and the code-send-verify-activate sequence continues to work as designed.

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
email-verification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/emailverification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EmailVerificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateAccountWorker.java
│       ├── SendCodeWorker.java
│       ├── VerifyCodeWorker.java
│       └── WaitInputWorker.java
└── src/test/java/emailverification/workers/
    ├── ActivateAccountWorkerTest.java        # 3 tests
    ├── SendCodeWorkerTest.java        # 4 tests
    ├── VerifyCodeWorkerTest.java        # 4 tests
    └── WaitInputWorkerTest.java        # 3 tests

```
