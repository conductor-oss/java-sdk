# User Onboarding in Java Using Conductor: Account Creation, Email Verification, Preferences, and Welcome

A Java Conductor workflow example that onboards a new user end-to-end: creates an account with a deterministic user ID, verifies the email address, initializes plan-appropriate default preferences (theme, language, timezone, notifications), and sends a personalized welcome email. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

A user signs up with their name, email, and chosen plan. You need to create their account record, verify the email address before they can use the product, set up sensible defaults for their plan tier, and send a welcome email to get them started. Each step depends on the previous one. Preferences and the welcome email both need the user ID generated during account creation, and you don't want to send a welcome email to an unverified address.

Without orchestration, you'd build a single signup service that calls the user database, fires off a verification email, writes preference rows, and sends the welcome message, all in one method. If the email service is down, the account is created but the user never gets verified. If preferences fail to save, the welcome email goes out anyway pointing to settings that don't exist. Debugging which step failed for a specific user means grepping logs across multiple services.

## The Solution

**You just write the account-creation, email-verification, preferences, and welcome-email workers. Conductor handles the onboarding sequence and user ID threading.**

Each onboarding step: account creation, email verification, preference setup, welcome delivery, is a simple, independent worker. Conductor executes them in the correct sequence, threads the generated user ID from account creation into every downstream step, retries if the email service times out or the database hiccups, and tracks the full onboarding journey for every user. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

CreateAccountWorker generates a user ID, VerifyEmailWorker confirms ownership, SetPreferencesWorker initializes plan-appropriate defaults, and WelcomeWorker sends a personalized getting-started email.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAccountWorker** | `uo_create_account` | Generates a deterministic user ID (USR-{hex} derived from email), persists the account record with email, name, and plan |
| **VerifyEmailWorker** | `uo_verify_email` | Sends an email verification link to the new user's address and confirms delivery |
| **SetPreferencesWorker** | `uo_set_preferences` | Initializes default user preferences: theme (light), language (en), notifications (on), timezone (UTC) |
| **WelcomeWorker** | `uo_welcome` | Sends a personalized welcome email to the verified user with getting-started content |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end with reproducible results. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
uo_create_account
    │
    ▼
uo_verify_email
    │
    ▼
uo_set_preferences
    │
    ▼
uo_welcome

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
java -jar target/user-onboarding-1.0.0.jar

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
java -jar target/user-onboarding-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow uo_user_onboarding \
  --version 1 \
  --input '{"username": "alice_johnson", "email": "alice@example.com", "fullName": "Alice Johnson", "plan": "pro"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w uo_user_onboarding -s COMPLETED -c 5

```

## How to Extend

Each worker handles one onboarding step. Connect your identity provider (Auth0, Cognito, Firebase Auth) for account creation and your email service (SendGrid, SES) for verification and welcome delivery, and the onboarding workflow stays the same.

**Example. Make `CreateAccountWorker` real with PostgreSQL:**

```java
// Before (simulated):
String userId = "USR-" + toHexPrefix(email);
result.getOutputData().put("userId", userId);
result.getOutputData().put("createdAt", "2025-01-15T10:30:00Z");

// After (real):
try (var conn = dataSource.getConnection();
     var stmt = conn.prepareStatement(
         "INSERT INTO users (email, full_name, plan) VALUES (?, ?, ?) RETURNING id, created_at")) {
    stmt.setString(1, email);
    stmt.setString(2, fullName);
    stmt.setString(3, plan);
    var rs = stmt.executeQuery();
    rs.next();
    result.getOutputData().put("userId", rs.getString("id"));
    result.getOutputData().put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
}

```

Replace the simulated database with PostgreSQL and the onboarding sequence: account creation through welcome email, operates unchanged.

**Other production swaps:**
- **CreateAccountWorker**: identity provider: Auth0 Management API (`com.auth0:auth0`) or AWS Cognito (`software.amazon.awssdk:cognitoidentityprovider`); publish a `user.created` event to Kafka
- **VerifyEmailWorker**: email provider: SendGrid (`com.sendgrid:sendgrid-java`), AWS SES (`software.amazon.awssdk:ses`), or direct SMTP; send a signed token link and wait for click-through confirmation
- **SetPreferencesWorker**: user profile store: PostgreSQL row per user, DynamoDB item, or a preferences microservice; derive defaults from plan tier (e.g., pro users get dark theme, extended notification options)
- **WelcomeWorker**: transactional email: SendGrid dynamic templates, SES with HTML templates; trigger an in-app onboarding tour or enroll the user in a drip campaign

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
user-onboarding/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/useronboarding/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserOnboardingExample.java   # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateAccountWorker.java # Deterministic account creation
│       ├── VerifyEmailWorker.java   # Email verification
│       ├── SetPreferencesWorker.java # Default preferences
│       └── WelcomeWorker.java       # Welcome email
└── src/test/java/useronboarding/workers/
    ├── CreateAccountWorkerTest.java # 7 tests
    ├── VerifyEmailWorkerTest.java   # 4 tests
    ├── SetPreferencesWorkerTest.java # 4 tests
    └── WelcomeWorkerTest.java       # 4 tests

```
