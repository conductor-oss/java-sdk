# Implementing Privileged Access Management in Java with Conductor :  Just-In-Time Access Requests, Approval, Grant, and Auto-Revocation

A Java Conductor workflow example for just-in-time privileged access management (PAM) .  receiving access requests with justification, running security approval, granting time-limited access to sensitive resources like production databases, and automatically revoking credentials when the window expires. Uses [Conductor](https://github.

## The Problem

You need to manage privileged access to sensitive resources .  production databases, cloud admin consoles, SSH bastion hosts. Engineers request elevated access tied to an incident or task, someone approves it, credentials are provisioned, and access must be revoked automatically after the approved duration (e.g., 2 hours). If revocation fails or gets skipped, you have standing privileged access that violates least-privilege and creates audit findings.

Without orchestration, you'd build a request queue with a background job for approvals, a separate cron for revocation, and hope they stay in sync. If the grant succeeds but the revocation timer never fires, the engineer keeps production database access indefinitely. If the process crashes between approval and grant, the request sits in limbo with no visibility into what happened.

## The Solution

**You just write the credential provisioning and revocation logic. Conductor handles guaranteed revocation after the access window, retries if the credential provisioning API fails, and a full compliance record of every privileged access grant and revocation.**

Each stage of the PAM lifecycle is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so requests are validated before approval, access is only granted after approval completes, and revocation is guaranteed to execute after the access window. If the revocation worker fails, Conductor retries it automatically. Every request, approval decision, grant, and revocation is recorded with full inputs and outputs for compliance audits.

### What You Write: Workers

Four workers manage just-in-time access: PamRequestWorker validates the access request and justification, PamApproveWorker routes to the security approver, PamGrantAccessWorker provisions time-limited credentials, and PamRevokeAccessWorker automatically revokes access when the window expires.

| Worker | Task | What It Does |
|---|---|---|
| **PamApproveWorker** | `pam_approve` | Approves the privileged access request after security review. |
| **PamGrantAccessWorker** | `pam_grant_access` | Grants temporary privileged access to the requested resource. |
| **PamRequestWorker** | `pam_request` | Receives a privileged access request and validates the justification. |
| **PamRevokeAccessWorker** | `pam_revoke_access` | Automatically revokes privileged access after expiry. |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
Input -> PamApproveWorker -> PamGrantAccessWorker -> PamRequestWorker -> PamRevokeAccessWorker -> Output

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
java -jar target/privileged-access-1.0.0.jar

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
java -jar target/privileged-access-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow privileged_access \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w privileged_access -s COMPLETED -c 5

```

## How to Extend

Each worker manages one PAM phase .  connect PamGrantAccessWorker to HashiCorp Vault dynamic secrets or AWS STS, PamRevokeAccessWorker to revoke leases on expiry, and the request-approve-grant-revoke workflow stays the same.

- **PamRequestWorker** (`pam_request`): validate the request against your ticketing system (Jira, ServiceNow) to confirm the incident ID exists and the requester is authorized for the resource
- **PamApproveWorker** (`pam_approve`): integrate with Slack or PagerDuty to route approval requests to the on-call security engineer, or auto-approve based on policy rules (e.g., pre-approved for break-glass incidents)
- **PamGrantAccessWorker** (`pam_grant_access`): provision temporary credentials via HashiCorp Vault dynamic secrets, AWS STS AssumeRole, or database user creation with an expiry timestamp
- **PamRevokeAccessWorker** (`pam_revoke_access`): revoke Vault leases, delete temporary IAM roles, or drop the database user; Conductor guarantees this runs even if prior steps had transient failures

Wire in HashiCorp Vault dynamic secrets or AWS STS, and the request-approve-grant-revoke lifecycle operates without any workflow definition changes.

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
privileged-access/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/privilegedaccess/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PrivilegedAccessExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PamApproveWorker.java
│       ├── PamGrantAccessWorker.java
│       ├── PamRequestWorker.java
│       └── PamRevokeAccessWorker.java
└── src/test/java/privilegedaccess/workers/
    ├── PamApproveWorkerTest.java        # 8 tests
    ├── PamGrantAccessWorkerTest.java        # 8 tests
    ├── PamRequestWorkerTest.java        # 8 tests
    └── PamRevokeAccessWorkerTest.java        # 8 tests

```
