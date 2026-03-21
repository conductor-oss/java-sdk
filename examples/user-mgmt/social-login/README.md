# Social Login in Java Using Conductor

A Java Conductor workflow example demonstrating Social Login. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A user clicks "Sign in with Google" on your login page. The system needs to detect which OAuth provider was selected, validate the OAuth token against the provider's API to retrieve the user's profile, link the social identity to an existing account or create a new one, and issue a session token so the user can access the application. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the provider-detection, token-validation, account-linking, and session-issuance workers. Conductor handles the OAuth login sequence and identity flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

DetectProviderWorker identifies Google/GitHub/Facebook, OAuthWorker validates the token and retrieves the profile, LinkAccountWorker connects the social identity, and CreateSessionWorker issues a session token.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSessionWorker** | `slo_session` | Issues a session token for the authenticated user with a configurable expiration |
| **DetectProviderWorker** | `slo_detect_provider` | Identifies the OAuth provider (Google, GitHub, Facebook) and resolves its authentication endpoint |
| **LinkAccountWorker** | `slo_link_account` | Links the social identity to an existing user account or creates a new account, tracking linked providers |
| **OAuthWorker** | `slo_auth` | Validates the OAuth token against the provider's API and retrieves the user's profile (name, avatar) |

Workers implement user lifecycle operations. account creation, verification, profile setup,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
slo_detect_provider
    │
    ▼
slo_auth
    │
    ▼
slo_link_account
    │
    ▼
slo_session

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
java -jar target/social-login-1.0.0.jar

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
java -jar target/social-login-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow slo_social_login \
  --version 1 \
  --input '{"provider": "TEST-001", "oauthToken": "sample-oauthToken", "email": "user@example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w slo_social_login -s COMPLETED -c 5

```

## How to Extend

Each worker handles one login step. connect your OAuth providers (Google, GitHub, Apple) for token validation and your user store for account linking, and the social-login workflow stays the same.

- **DetectProviderWorker** (`slo_detect_provider`): look up the provider configuration from your Auth0 social connections or Cognito identity providers to resolve the correct OAuth endpoints and client credentials
- **OAuthWorker** (`slo_auth`): exchange the authorization code for tokens via the provider's OAuth endpoint (Google, GitHub, Facebook) and fetch the user profile using the access token
- **LinkAccountWorker** (`slo_link_account`): search for an existing user by email in your identity provider (Auth0, Cognito) and link the social identity, or create a new user if none exists
- **CreateSessionWorker** (`slo_session`): issue a JWT session token via your identity provider and store the session in Redis with device fingerprinting for security

Integrate a real OAuth client library and the detect-validate-link-session social login flow continues without modification.

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
social-login/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sociallogin/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SocialLoginExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateSessionWorker.java
│       ├── DetectProviderWorker.java
│       ├── LinkAccountWorker.java
│       └── OAuthWorker.java
└── src/test/java/sociallogin/workers/
    ├── CreateSessionWorkerTest.java        # 3 tests
    ├── DetectProviderWorkerTest.java        # 3 tests
    ├── LinkAccountWorkerTest.java        # 3 tests
    └── OAuthWorkerTest.java        # 3 tests

```
