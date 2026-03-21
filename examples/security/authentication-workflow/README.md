# Implementing Authentication Workflow in Java with Conductor :  Credential Validation, MFA Verification, Risk Assessment, and Token Issuance

A Java Conductor workflow example implementing a multi-step authentication pipeline. validating user credentials (password, biometric, API key), verifying a second factor via MFA, assessing login risk based on device and location signals, and issuing a signed JWT session token only after all checks pass. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to authenticate users through multiple verification layers before granting access. First, the submitted credentials (password hash, biometric template, or API key) must be validated against the identity store. Then, if MFA is enabled, a second factor (TOTP, SMS code, hardware key) must be verified. Before issuing a token, a risk assessment must evaluate whether the login attempt looks suspicious. new device, unusual geolocation, impossible travel, or velocity anomalies. Only after all three checks pass should a JWT be minted with the appropriate claims, scopes, and expiration. If any step fails, the login must be denied and the failure recorded for security monitoring.

Without orchestration, authentication logic is a deeply nested chain of if/else blocks. validate credentials, then check MFA, then assess risk, then issue a token. Each step calls a different backend (identity provider, MFA service, risk engine, token service), and a failure in one requires careful cleanup of the others. When the MFA provider times out, users get stuck in a half-authenticated state. When the risk engine is slow, login latency spikes. Nobody can tell from logs which step actually failed for a given login attempt, making it impossible to debug "why can't I log in?" support tickets.

## The Solution

**You just write the credential validation, MFA check, and token signing logic. Conductor handles strict ordering so no token is minted without MFA and risk checks, retries when the MFA provider is temporarily unavailable, and a full audit trail of every login attempt.**

Each authentication concern is a simple, independent worker. one validates credentials against the identity store, one verifies the MFA challenge, one scores login risk from device and location signals, one mints the JWT with appropriate claims. Conductor takes care of executing them in strict order so no token is issued without MFA verification and risk assessment, retrying if the MFA provider is temporarily unavailable, and maintaining a complete audit trail of every login attempt with inputs, outputs, and timing for each verification step. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The authentication pipeline sequences ValidateCredentialsWorker to check passwords or biometrics, CheckMfaWorker to verify the second factor, RiskAssessmentWorker to evaluate device and location signals, and IssueTokenWorker to mint a signed JWT only after all checks pass.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateCredentialsWorker** | `auth_validate_credentials` | Validates the submitted credentials (password hash, biometric template, or API key) against the identity store and returns a success/failure result with the user ID |
| **CheckMfaWorker** | `auth_check_mfa` | Verifies the second authentication factor. TOTP code, SMS one-time password, or hardware security key challenge, and confirms MFA status |
| **RiskAssessmentWorker** | `auth_risk_assessment` | Scores the login attempt for risk signals. device fingerprint, geolocation, impossible travel, login velocity, and returns a risk level (low/medium/high) |
| **IssueTokenWorker** | `auth_issue_token` | Mints a signed JWT with user claims, scopes, and expiration after all verification steps have passed |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
auth_validate_credentials
    │
    ▼
auth_check_mfa
    │
    ▼
auth_risk_assessment
    │
    ▼
auth_issue_token

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
java -jar target/authentication-workflow-1.0.0.jar

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
java -jar target/authentication-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow authentication_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "authMethod": "sample-authMethod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w authentication_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one authentication layer. connect ValidateCredentialsWorker to Auth0 or Okta, CheckMfaWorker to Duo Security, and the credential-MFA-risk-token workflow stays the same.

- **ValidateCredentialsWorker** (`auth_validate_credentials`): verify credentials against your identity provider (Auth0, Okta, AWS Cognito, Active Directory) using their authentication APIs
- **CheckMfaWorker** (`auth_check_mfa`): integrate with Duo Security, Google Authenticator TOTP verification, or Twilio Verify for SMS/voice second-factor challenges
- **RiskAssessmentWorker** (`auth_risk_assessment`): call a risk engine (Castle, Sift, or a custom model) with device fingerprints, IP geolocation, and login history to score the attempt
- **IssueTokenWorker** (`auth_issue_token`): generate signed JWTs with your private key, set claims from the identity provider, and store refresh tokens in Redis or your session store

Integrate with your real IdP and MFA service, and the credential-MFA-risk-token orchestration transfers directly to production.

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
authentication-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/authenticationworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AuthenticationWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckMfaWorker.java
│       ├── IssueTokenWorker.java
│       ├── RiskAssessmentWorker.java
│       └── ValidateCredentialsWorker.java
└── src/test/java/authenticationworkflow/workers/
    ├── CheckMfaWorkerTest.java        # 8 tests
    ├── IssueTokenWorkerTest.java        # 8 tests
    ├── RiskAssessmentWorkerTest.java        # 8 tests
    └── ValidateCredentialsWorkerTest.java        # 8 tests

```
