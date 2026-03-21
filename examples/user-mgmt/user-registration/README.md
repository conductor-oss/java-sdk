# User Registration in Java Using Conductor :  Validation, Account Creation, Confirmation, and Activation

A Java Conductor workflow example for user registration .  validating username and email format, creating the user record with a unique ID, sending a confirmation email, and activating the account. Uses [Conductor](https://github.

## The Problem

You need a registration flow that guards against invalid input before creating any records. The username must meet minimum length requirements, the email must be well-formed, and neither can already be taken. Only after validation passes should the system create a user record, send a confirmation email with the new user ID, and finally flip the account to active status. Each step depends on the one before it .  you can't confirm a user that was never created, and you shouldn't activate an account before confirmation is sent.

Without orchestration, you'd write a single registration endpoint that validates, inserts into the database, calls the email service, and updates the active flag .  all in one transaction. If the email service times out after the record is created, the user exists but never receives confirmation. If activation fails, there's no record of where the flow stopped. Retrying means risking duplicate accounts, and debugging a stuck registration means manually querying multiple systems.

## The Solution

**You just write the validation, account-creation, confirmation, and activation workers. Conductor handles the registration sequence and user ID propagation.**

Each registration step .  validation, record creation, confirmation, activation ,  is a simple, independent worker. Conductor runs them in strict sequence, passes the validation result into account creation, threads the generated user ID into confirmation and activation, retries any step that fails without creating duplicates, and tracks the full registration journey for every user. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ValidateWorker checks username length and email format, CreateWorker generates a unique user ID, ConfirmWorker sends the confirmation email, and ActivateWorker flips the account to active.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateWorker** | `ur_validate` | Checks username length (minimum 3 chars), email format (contains @), and uniqueness .  returns validation result and per-check breakdown |
| **CreateWorker** | `ur_create` | Creates the user record with a unique ID (USR-XXXXXXXX) and timestamps the creation |
| **ConfirmWorker** | `ur_confirm` | Sends a confirmation email to the new user's address with their user ID |
| **ActivateWorker** | `ur_activate` | Flips the account status from pending to active, making the user fully registered |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
ur_validate
    │
    ▼
ur_create
    │
    ▼
ur_confirm
    │
    ▼
ur_activate

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
java -jar target/user-registration-1.0.0.jar

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
java -jar target/user-registration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ur_user_registration \
  --version 1 \
  --input '{"username": "test", "email": "user@example.com", "password": "sample-password"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ur_user_registration -s COMPLETED -c 5

```

## How to Extend

Each worker handles one registration step .  connect your user database for account creation and your email service (SendGrid, SES) for confirmation delivery, and the registration workflow stays the same.

- **ValidateWorker** (`ur_validate`): add uniqueness checks against your user database, integrate CAPTCHA verification, or enforce password strength policies
- **CreateWorker** (`ur_create`): persist to PostgreSQL/DynamoDB, provision the user in Auth0/Cognito, or publish a `user.registered` event to Kafka for downstream systems
- **ConfirmWorker** (`ur_confirm`): send real confirmation emails via SendGrid/SES with a signed verification link that expires after 24 hours
- **ActivateWorker** (`ur_activate`): update account status in your identity provider, grant initial role/permissions, and trigger post-activation hooks (e.g., provisioning default resources)

Plug in Auth0 or PostgreSQL for real account creation and the validate-create-confirm-activate registration flow operates as designed.

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
user-registration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/userregistration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserRegistrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateWorker.java
│       ├── ConfirmWorker.java
│       ├── CreateWorker.java
│       └── ValidateWorker.java
└── src/test/java/userregistration/workers/
    ├── ActivateWorkerTest.java        # 3 tests
    ├── ConfirmWorkerTest.java        # 3 tests
    ├── CreateWorkerTest.java        # 4 tests
    └── ValidateWorkerTest.java        # 5 tests

```
