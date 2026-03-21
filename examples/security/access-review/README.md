# Implementing Access Review in Java with Conductor :  Entitlement Collection, Anomaly Detection, Certification, and Enforcement

A Java Conductor workflow example for access review. collecting user entitlements across systems, identifying anomalous access (excessive permissions, dormant accounts), requesting manager certification, and enforcing revocation decisions.

## The Problem

Compliance frameworks (SOX, SOC2, ISO 27001) require periodic access reviews. verifying that every user's permissions are still appropriate. You must collect entitlements from all systems (Active Directory, AWS IAM, SaaS apps), identify anomalies (users with admin access who changed roles, dormant accounts still provisioned), send certification requests to managers, and revoke access for denied items.

Without orchestration, access reviews are quarterly spreadsheet exercises. Someone exports user lists from each system, manually highlights anomalies, emails managers for certification, and hopes they respond before the deadline. Revocation is manual and often forgotten.

## The Solution

**You just write the entitlement queries and revocation calls. Conductor handles the multi-step review sequence, retries when identity providers are unavailable, and a compliance-ready record of every entitlement reviewed and decision made.**

Each review step is an independent worker. entitlement collection, anomaly detection, certification requests, and enforcement. Conductor runs them in sequence with conditional routing: anomalous access gets flagged for extra scrutiny. Every review cycle is tracked with entitlements collected, anomalies found, certifications received, and revocations applied. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The review pipeline uses CollectEntitlementsWorker to gather permissions across systems, IdentifyAnomaliesWorker to flag excessive or dormant access, RequestCertificationWorker to obtain manager approvals, and EnforceDecisionsWorker to revoke denied items.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEntitlementsWorker** | `ar_collect_entitlements` | Collects all user entitlements across systems for a department (e.g., 45 users, 312 entitlements) |
| **EnforceDecisionsWorker** | `ar_enforce_decisions` | Revokes access grants that were denied during certification (e.g., 5 revocations across 3 systems) |
| **IdentifyAnomaliesWorker** | `ar_identify_anomalies` | Detects excessive access grants and dormant accounts that need review |
| **RequestCertificationWorker** | `ar_request_certification` | Sends certification requests to managers and records their approve/deny decisions |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
ar_collect_entitlements
    │
    ▼
ar_identify_anomalies
    │
    ▼
ar_request_certification
    │
    ▼
ar_enforce_decisions

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
java -jar target/access-review-1.0.0.jar

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
java -jar target/access-review-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow access_review_workflow \
  --version 1 \
  --input '{"department": "engineering", "reviewCycle": "sample-reviewCycle"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w access_review_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker covers one review phase. connect CollectEntitlementsWorker to Active Directory and Okta, EnforceDecisionsWorker to your IAM APIs, and the entitlement-review-certification workflow stays the same.

- **CollectEntitlementsWorker** (`ar_collect_entitlements`): query Active Directory, AWS IAM, Okta, and SaaS apps for user permissions and group memberships
- **EnforceDecisionsWorker** (`ar_enforce_decisions`): revoke access via identity provider APIs, remove IAM policies, and deactivate SaaS accounts for denied items
- **IdentifyAnomaliesWorker** (`ar_identify_anomalies`): detect excessive permissions, orphaned accounts, and privilege escalation using role-mining algorithms

Plug in your real IAM APIs and the entitlement review orchestration adapts without any workflow definition changes.

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
access-review-access-review/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/accessreview/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectEntitlementsWorker.java
│       ├── EnforceDecisionsWorker.java
│       ├── IdentifyAnomaliesWorker.java
│       └── RequestCertificationWorker.java
└── src/test/java/accessreview/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
