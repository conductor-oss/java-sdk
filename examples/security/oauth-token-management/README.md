# Implementing OAuth Token Management in Java with Conductor :  Grant Validation, Token Issuance, and Compliance Auditing

A Java Conductor workflow example for OAuth 2.0 token lifecycle management. validating client credentials and grant types, issuing access and refresh tokens, persisting token metadata for revocation, and logging every issuance for compliance. Uses [Conductor](https://github.

## The Problem

You need to handle OAuth 2.0 token requests end-to-end: validate that the client ID is registered and the grant type (authorization_code, client_credentials, etc.) is permitted, issue scoped access and refresh tokens, store token metadata so tokens can be revoked or introspected later, and write an immutable audit trail for every issuance event.

Without orchestration, you'd wire all of this into a single token endpoint handler. checking credentials, generating JWTs, writing to the token store, and appending audit logs in one long method. If the token store write fails after issuance, you have an untracked token in the wild. If the audit log write throws, you either swallow the error or fail the entire request. Retry logic, failure isolation, and observability all get bolted on as afterthoughts, and the result is a brittle, hard-to-audit token service.

## The Solution

**You just write the grant validation and JWT signing logic. Conductor handles the validate-issue-store-audit sequence, retries a failed token store write without re-issuing, and an immutable audit trail of every token minted.**

Each stage of the token lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in sequence, retrying a failed token store write without re-issuing the token, tracking every step with inputs and outputs for audit, and resuming from the exact failure point if the process crashes mid-issuance.

### What You Write: Workers

Four workers manage the token lifecycle: ValidateGrantWorker checks client credentials and grant type, IssueTokensWorker mints access and refresh tokens, StoreTokenWorker persists metadata for revocation, and AuditLogWorker records every issuance event for compliance.

| Worker | Task | What It Does |
|---|---|---|
| **AuditLogWorker** | `otm_audit_log` | Logs token issuance for compliance. |
| **IssueTokensWorker** | `otm_issue_tokens` | Issues access and refresh tokens. |
| **StoreTokenWorker** | `otm_store_token` | Stores token metadata for revocation support. |
| **ValidateGrantWorker** | `otm_validate_grant` | Validates the OAuth grant type and client credentials. |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
Input -> AuditLogWorker -> IssueTokensWorker -> StoreTokenWorker -> ValidateGrantWorker -> Output

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
java -jar target/oauth-token-management-1.0.0.jar

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
java -jar target/oauth-token-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow oauth_token_management \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w oauth_token_management -s COMPLETED -c 5

```

## How to Extend

Each worker handles one token lifecycle step. connect ValidateGrantWorker to your OAuth provider (Keycloak, Auth0), IssueTokensWorker to your JWT signing service, and the validate-issue-store-audit workflow stays the same.

- **ValidateGrantWorker** (`otm_validate_grant`): look up registered clients and allowed grant types in your OAuth provider (Keycloak admin API, Auth0 Management API, or a database of registered applications)
- **IssueTokensWorker** (`otm_issue_tokens`): generate real JWTs with a signing key, set expiry based on grant type, and produce refresh tokens backed by your token store
- **StoreTokenWorker** (`otm_store_token`): persist token metadata (jti, client_id, scopes, expiry) to Redis or DynamoDB so tokens can be revoked or introspected via RFC 7009/9012
- **AuditLogWorker** (`otm_audit_log`): write issuance events to an append-only audit log (CloudWatch Logs, Elasticsearch, or a compliance-grade SIEM)

Integrate with your real OAuth provider and JWT signing service, and the validate-issue-store-audit pipeline transfers directly to production.

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
oauth-token-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/oauthtokenmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OauthTokenManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditLogWorker.java
│       ├── IssueTokensWorker.java
│       ├── StoreTokenWorker.java
│       └── ValidateGrantWorker.java
└── src/test/java/oauthtokenmanagement/workers/
    ├── AuditLogWorkerTest.java        # 8 tests
    ├── IssueTokensWorkerTest.java        # 8 tests
    ├── StoreTokenWorkerTest.java        # 8 tests
    └── ValidateGrantWorkerTest.java        # 8 tests

```
