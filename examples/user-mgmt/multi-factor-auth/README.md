# Multi Factor Auth in Java Using Conductor

A Java Conductor workflow example demonstrating Multi Factor Auth. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

You need to authenticate a user with multi-factor authentication. Validating their username and password as the primary factor, determining which second factor to use (TOTP app, SMS code, hardware key) based on their preference and enrollment, verifying the second factor, and granting access only if both factors succeed. Each step depends on the previous one's output.

If primary auth succeeds but the second factor method selection picks a method the user hasn't enrolled in, the login fails with a confusing error. If factor verification succeeds but the access grant step fails to issue the session token, the user proved their identity but can't get in. Without orchestration, you'd build a monolithic login handler that mixes password hashing, TOTP validation, SMS delivery, and session management. Making it impossible to add new factor types (passkeys, biometrics), enforce step-up authentication for sensitive operations, or track which authentication methods are most reliable for your user base.

## The Solution

**You just write the primary-auth, factor-selection, factor-verification, and access-grant workers. Conductor handles the MFA sequence and factor-method routing.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

PrimaryAuthWorker validates credentials, SelectMethodWorker picks TOTP/SMS/email based on enrollment, VerifyFactorWorker checks the second-factor code, and GrantAccessWorker issues a session token.

| Worker | Task | What It Does |
|---|---|---|
| **GrantAccessWorker** | `mfa_grant` | Issues a session token and grants access to the authenticated user with a configurable expiration |
| **PrimaryAuthWorker** | `mfa_primary_auth` | Validates the user's username and password, returning a user ID on success |
| **SelectMethodWorker** | `mfa_select_method` | Determines the second factor method (TOTP, SMS, or email) based on user preference and available options |
| **VerifyFactorWorker** | `mfa_verify_factor` | Verifies the submitted second-factor code against the selected method, tracking attempt count |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
mfa_primary_auth
    │
    ▼
mfa_select_method
    │
    ▼
mfa_verify_factor
    │
    ▼
mfa_grant
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
java -jar target/multi-factor-auth-1.0.0.jar
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
java -jar target/multi-factor-auth-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow mfa_multi_factor_auth \
  --version 1 \
  --input '{"username": "test", "password": "test-value", "preferredMethod": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w mfa_multi_factor_auth -s COMPLETED -c 5
```

## How to Extend

Each worker handles one auth step .  connect your identity provider (Auth0, Okta, Duo) for factor verification and your session store (Redis, DynamoDB) for token issuance, and the MFA workflow stays the same.

- **PrimaryAuthWorker** (`mfa_primary_auth`): validate credentials against your identity provider (Auth0, Cognito, Okta) using their authentication API
- **SelectMethodWorker** (`mfa_select_method`): query the user's enrolled MFA methods from Auth0 or Cognito and select the appropriate one based on device context and user preference
- **VerifyFactorWorker** (`mfa_verify_factor`): verify TOTP codes against your authenticator app integration, SMS codes via Twilio Verify API, or email codes via SendGrid
- **GrantAccessWorker** (`mfa_grant`): issue a JWT or session token via your identity provider's token endpoint and store the session in Redis with appropriate TTL

Integrate a real TOTP library and SMS provider and the multi-factor authentication pipeline operates without any workflow changes.

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
multi-factor-auth/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multifactorauth/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiFactorAuthExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GrantAccessWorker.java
│       ├── PrimaryAuthWorker.java
│       ├── SelectMethodWorker.java
│       └── VerifyFactorWorker.java
└── src/test/java/multifactorauth/workers/
    ├── GrantAccessWorkerTest.java        # 4 tests
    ├── PrimaryAuthWorkerTest.java        # 3 tests
    ├── SelectMethodWorkerTest.java        # 4 tests
    └── VerifyFactorWorkerTest.java        # 3 tests
```
