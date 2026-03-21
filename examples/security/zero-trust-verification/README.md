# Implementing Zero Trust Verification in Java with Conductor :  Identity Verification, Device Assessment, Context Evaluation, and Policy Enforcement

A Java Conductor workflow example for zero trust verification. verifying user identity, assessing device health and compliance, evaluating request context (location, time, risk score), and enforcing access policy based on all factors.

## The Problem

Zero trust means "never trust, always verify". every access request must be verified regardless of network location. When a user requests access to a resource, you must verify their identity (MFA, certificate), assess their device (patched, encrypted, managed), evaluate the context (is this a normal login time? is the location consistent?), and enforce the combined policy (allow, deny, step-up authentication).

Without orchestration, zero trust checks are scattered across different systems. identity in Okta, device health in MDM, context in the SIEM, and none of them share a unified decision point. Each system makes independent allow/deny decisions, and there's no single place that evaluates all trust signals together.

## The Solution

**You just write the identity checks and device posture assessments. Conductor handles the strict verification sequence so no access is granted without all trust signals evaluated, retries when identity providers are slow, and a complete audit of every access decision with all contributing signals.**

Each trust signal is evaluated by an independent worker. identity verification, device assessment, context evaluation, and policy enforcement. Conductor runs them in sequence: verify identity, assess device, evaluate context, then enforce the combined policy. Every access decision is tracked with all trust signals,  you can audit exactly why access was granted or denied. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four trust-signal evaluators work in sequence: VerifyIdentityWorker checks MFA and certificates, AssessDeviceWorker validates patch level and encryption, EvaluateContextWorker analyzes location and behavior, and EnforcePolicyWorker computes a composite trust score to grant or deny access.

| Worker | Task | What It Does |
|---|---|---|
| **AssessDeviceWorker** | `zt_assess_device` | Checks device compliance. patch level, disk encryption, and MDM enrollment status |
| **EnforcePolicyWorker** | `zt_enforce_policy` | Computes a composite trust score from all signals and grants or denies access |
| **EvaluateContextWorker** | `zt_evaluate_context` | Evaluates request context. network location, time of day, and behavioral anomalies |
| **VerifyIdentityWorker** | `zt_verify_identity` | Verifies user identity via MFA and returns a trust score |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
zt_verify_identity
    │
    ▼
zt_assess_device
    │
    ▼
zt_evaluate_context
    │
    ▼
zt_enforce_policy

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
java -jar target/zero-trust-verification-1.0.0.jar

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
java -jar target/zero-trust-verification-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow zero_trust_verification_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "deviceId": "TEST-001", "requestedResource": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w zero_trust_verification_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker evaluates one trust signal. connect VerifyIdentityWorker to Okta MFA, AssessDeviceWorker to Intune or Jamf for device posture, and the identity-device-context-policy workflow stays the same.

- **AssessDeviceWorker** (`zt_assess_device`): check device posture via MDM (Intune, Jamf). encryption enabled, OS patched, endpoint protection active
- **EnforcePolicyWorker** (`zt_enforce_policy`): apply access policy based on combined trust score. allow, deny, require step-up auth, or restrict to read-only
- **EvaluateContextWorker** (`zt_evaluate_context`): evaluate risk signals. impossible travel detection, unusual login hours, known-bad IP reputation

Integrate with your MFA provider and MDM, and the zero trust evaluation pipeline works without touching the workflow definition.

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
zero-trust-verification-zero-trust-verification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/zerotrustverification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessDeviceWorker.java
│       ├── EnforcePolicyWorker.java
│       ├── EvaluateContextWorker.java
│       └── VerifyIdentityWorker.java
└── src/test/java/zerotrustverification/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
