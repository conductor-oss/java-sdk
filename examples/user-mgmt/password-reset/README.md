# Password Reset Workflow in Java Using Conductor

User clicks "Reset Password." The email takes eight minutes because your SMTP relay is backed up. They click "Reset" again. Now two tokens are live. The first email arrives, they click it, but that token expired after five minutes. Locked out. They try the second link: it works, but the password update succeeds while the confirmation email fails, so they don't know the reset went through and submit a third request. Support gets a ticket from a frustrated user who "can't log in" with a trail of three tokens, two expired, one used, and no audit log of what happened. This example orchestrates the password reset flow with Conductor: account lookup, token validation, credential update, and confirmation notification, each step sequenced, retriable, and fully auditable. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the identity logic, Conductor handles retries, failure routing, durability, and observability.

## The Forgot-Password Flow

A user clicks "Forgot Password" and enters their email. The system needs to look up the account, validate the reset token (checking it was issued for this user and hasn't expired), update the credential store with the new password hash, and send a confirmation email, all in the correct order, with no step skipped. If the token is valid but the password update fails, the user is stuck: the token may be consumed but the password unchanged. If the notification fails, the user doesn't know the reset succeeded and submits another request.

Without orchestration, you'd chain all of this in a single servlet or controller method. Catching exceptions at each step, manually rolling back on failure, and hoping the email service doesn't time out while you're holding a database transaction open. That code becomes brittle, hard to test, and impossible to audit when security reviews ask "show me every reset that happened last month."

## The Solution

**You just write the account-lookup, token-validation, password-update, and confirmation workers. Conductor handles the secure reset sequence and retry logic.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

RequestWorker looks up the account by email, VerifyTokenWorker validates the reset token, ResetWorker updates the password hash, and NotifyWorker sends a confirmation email, each handles one step of the secure reset flow.

| Worker | Task | What It Does |
|---|---|---|
| `RequestWorker` | `pwd_request` | Looks up the user account by email and returns the user ID (`USR-A1B2C3`) and a timestamp |
| `VerifyTokenWorker` | `pwd_verify` | Validates the reset token against the user ID, confirming it is valid with 900 seconds remaining |
| `ResetWorker` | `pwd_reset` | Updates the user's password in the credential store and records the update timestamp |
| `NotifyWorker` | `pwd_notify` | Sends a password-change confirmation email to the user's address |

Workers implement user lifecycle operations: account creation, verification, profile setup, with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
pwd_request
    |
    v
pwd_verify
    |
    v
pwd_reset
    |
    v
pwd_notify

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
java -jar target/password-reset-1.0.0.jar

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
java -jar target/password-reset-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pwd_password_reset \
  --version 1 \
  --input '{"email": "carol@example.com", "newPassword": "S3cure!Pass#2026"}'

```

> **Production note:** In production, avoid passing passwords as plain workflow input.
> On Orkes Cloud / Conductor Enterprise, use
> [Conductor Secrets](https://orkes.io/content/developer-guides/secrets-in-conductor)
> and reference the password as `${workflow.secrets.reset_password}` in the
> workflow definition so the value never appears in workflow history.

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pwd_password_reset -s COMPLETED -c 5

```

## How to Extend

Each worker handles one reset step. Connect your identity provider (Auth0, Cognito, Okta) for credential updates and your email service (SendGrid, SES) for confirmation delivery, and the password-reset workflow stays the same.

- **`RequestWorker`**: Look up the user in your identity provider (Auth0, Cognito, Keycloak, or a database) and generate a time-limited reset token stored in Redis or a token table.

- **`VerifyTokenWorker`**: Validate the token against the stored hash, check expiration, and enforce single-use by marking it consumed after verification.

- **`ResetWorker`**: Hash the new password with bcrypt/scrypt and update the credential store, invalidating all existing sessions for the user.

- **`NotifyWorker`**: Send a confirmation email via SendGrid, SES, or your transactional email service, including device/location metadata for security awareness.

Connect your credential store and email service and the lookup-verify-reset-notify password flow operates without any workflow changes.

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
password-reset/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/passwordreset/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PasswordResetExample.java    # Main entry point (supports --workers mode)
│   └── workers/
│       ├── NotifyWorker.java        # Sends password-change confirmation email
│       ├── RequestWorker.java       # Looks up user account by email
│       ├── ResetWorker.java         # Updates password in credential store
│       └── VerifyTokenWorker.java   # Validates reset token and expiration
└── src/test/java/passwordreset/workers/
    ├── NotifyWorkerTest.java
    ├── RequestWorkerTest.java
    ├── ResetWorkerTest.java
    └── VerifyTokenWorkerTest.java

```
