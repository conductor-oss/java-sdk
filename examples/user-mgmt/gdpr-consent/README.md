# GDPR Consent in Java Using Conductor

A Java Conductor workflow example demonstrating GDPR Consent. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to manage a user's GDPR consent preferences. Presenting the available consent options (cookies, analytics, marketing), recording which options the user accepted or declined, propagating those preferences to all downstream systems that process the user's data, and creating an immutable audit trail that proves when and what the user consented to. Each step depends on the previous one's output.

If consent is recorded but downstream systems aren't updated, your analytics platform keeps tracking a user who opted out, a direct GDPR Article 7 violation. If the audit trail fails to capture the exact consent choices and timestamp, you can't demonstrate lawful basis for processing during a regulatory inquiry. Without orchestration, you'd build a monolithic consent handler that mixes consent UI rendering, database writes, cross-system propagation, and audit logging, making it impossible to add new consent categories, integrate additional downstream systems, or produce consent histories for individual users on demand.

## The Solution

**You just write the consent-presentation, recording, system-propagation, and audit workers. Conductor handles the GDPR compliance pipeline and cross-system sync.**

PresentOptionsWorker loads the consent categories available for the user: cookies, analytics tracking, marketing communications, third-party data sharing, and returns the options that need to be displayed. RecordConsentWorker saves the user's consent choices (accepted and declined categories) with a timestamp, creating the official consent record. UpdateSystemsWorker propagates the consent preferences to all 4 downstream systems, disabling analytics tracking, removing marketing email subscriptions, updating cookie policies, and adjusting data-sharing flags. AuditWorker creates an immutable audit trail entry linking the user, their exact choices, the timestamp, and a unique audit trail ID for regulatory compliance. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

PresentOptionsWorker loads consent categories, RecordConsentWorker saves choices with timestamps, UpdateSystemsWorker propagates to analytics and marketing systems, and AuditWorker creates an immutable compliance trail.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `gdc_audit` | Creates an immutable audit trail entry with a unique audit ID linking the user's consent choices and timestamp |
| **PresentOptionsWorker** | `gdc_present_options` | Loads the available consent categories (analytics, marketing, third-party, cookies) for the user |
| **RecordConsentWorker** | `gdc_record_consent` | Records the user's consent choices with a versioned timestamp, storing the full consent record |
| **UpdateSystemsWorker** | `gdc_update_systems` | Propagates consent preferences to 4 downstream systems: analytics, email, ads, and DMP |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
gdc_present_options
    │
    ▼
gdc_record_consent
    │
    ▼
gdc_update_systems
    │
    ▼
gdc_audit

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
java -jar target/gdpr-consent-1.0.0.jar

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
java -jar target/gdpr-consent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow gdc_gdpr_consent \
  --version 1 \
  --input '{"userId": "TEST-001", "consents": "sample-consents"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w gdc_gdpr_consent -s COMPLETED -c 5

```

## How to Extend

Each worker handles one consent step .  connect your consent management platform (OneTrust, Cookiebot, TrustArc) for preference recording and your downstream systems for opt-out propagation, and the GDPR workflow stays the same.

- **PresentOptionsWorker** (`gdc_present_options`): load consent categories from your consent management platform (OneTrust, Cookiebot, TrustArc) or a configuration store, returning the current options for the user's jurisdiction
- **RecordConsentWorker** (`gdc_record_consent`): persist the consent record in your consent database with cryptographic signing for tamper-proof storage, including the exact text the user agreed to and the timestamp
- **UpdateSystemsWorker** (`gdc_update_systems`): propagate consent changes to Google Analytics, Segment, HubSpot, Mailchimp, and any other system processing user data, disabling or enabling data flows based on the user's choices
- **AuditWorker** (`gdc_audit`): write the audit trail to an append-only log (AWS QLDB, immutable database table) that links the consent record, user identity, and timestamp for regulatory audit and Data Subject Access Requests

Connect your real downstream systems and audit database and the GDPR consent pipeline with cross-system propagation operates unchanged.

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
gdpr-consent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/gdprconsent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GdprConsentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuditWorker.java
│       ├── PresentOptionsWorker.java
│       ├── RecordConsentWorker.java
│       └── UpdateSystemsWorker.java
└── src/test/java/gdprconsent/workers/
    ├── AuditWorkerTest.java        # 3 tests
    ├── PresentOptionsWorkerTest.java        # 3 tests
    ├── RecordConsentWorkerTest.java        # 4 tests
    └── UpdateSystemsWorkerTest.java        # 3 tests

```
