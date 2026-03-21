# Vendor Compliance Management in Java with Conductor :  Assessment, Audit, Certification, and Ongoing Monitoring

A Java Conductor workflow example for vendor compliance management. assessing a vendor's adherence to standards like ISO 27001, conducting formal audits of their controls and practices, issuing or renewing compliance certifications, and setting up ongoing monitoring for compliance drift. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to verify that your supply chain vendors meet required compliance standards. For a vendor like SecureData Partners, you must assess their current posture against ISO 27001 controls, conduct a formal audit covering data handling, access management, and incident response processes, issue a certification if they pass (or document gaps if they don't), and establish continuous monitoring to catch compliance drift before the next audit cycle. Procurement cannot issue new purchase orders to non-compliant vendors.

Without orchestration, compliance assessments live in spreadsheets, audit findings in Word docs, and certification status in someone's memory. If the audit step reveals issues, there is no automated linkage back to the assessment criteria. Certifications expire without notice because no system tracks renewal dates. When a regulator asks for proof that Vendor X was compliant on a specific date, you spend days reconstructing the timeline.

## The Solution

**You just write the compliance workers. Posture assessment, controls audit, certification issuance, and drift monitoring. Conductor handles step sequencing, automatic retries, and tamper-evident records for regulatory evidence.**

Each stage of the vendor compliance lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so assessment results feed the audit scope, audit findings determine certification eligibility, and monitoring is configured based on the certification terms. If the audit worker fails mid-evaluation, Conductor retries without losing assessment data. Every assessment score, audit finding, certification decision, and monitoring configuration is recorded with timestamps for regulatory evidence.

### What You Write: Workers

Four workers manage the vendor compliance lifecycle: AssessWorker evaluates posture against ISO 27001, AuditWorker reviews controls, CertifyWorker issues certifications, and MonitorWorker watches for compliance drift.

| Worker | Task | What It Does |
|---|---|---|
| **AssessWorker** | `vcm_assess` | Assesses the vendor's current posture against the compliance standard and returns a score. |
| **AuditWorker** | `vcm_audit` | Conducts a formal audit of the vendor's controls, data handling, and incident response practices. |
| **CertifyWorker** | `vcm_certify` | Issues or renews a compliance certification based on audit results. |
| **MonitorWorker** | `vcm_monitor` | Configures ongoing monitoring to detect compliance drift before the next audit cycle. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
vcm_assess
    │
    ▼
vcm_audit
    │
    ▼
vcm_certify
    │
    ▼
vcm_monitor

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
java -jar target/compliance-vendor-1.0.0.jar

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
java -jar target/compliance-vendor-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow vcm_vendor_compliance \
  --version 1 \
  --input '{"vendorId": "TEST-001", "vendorName": "test", "complianceStandard": "sample-complianceStandard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w vcm_vendor_compliance -s COMPLETED -c 5

```

## How to Extend

Connect AssessWorker to your GRC platform (ServiceNow, OneTrust) and AuditWorker to your compliance evidence repository to go live. The workflow definition stays exactly the same.

- **AssessWorker** (`vcm_assess`): pull the vendor's self-assessment questionnaire responses from your GRC platform (OneTrust, Prevalent, or ServiceNow VRM) and score them against the target compliance framework
- **AuditWorker** (`vcm_audit`): schedule and record formal audit activities, upload evidence documents, and track finding resolution via your audit management system
- **CertifyWorker** (`vcm_certify`): issue digital compliance certificates with expiry dates, store them in your vendor master database, and update procurement system eligibility flags
- **MonitorWorker** (`vcm_monitor`): configure continuous monitoring via SecurityScorecard, BitSight, or RiskRecon APIs to detect compliance drift between audit cycles

Swap in real GRC platform calls while keeping the same output fields, and the workflow operates without modification.

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
compliance-vendor/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancevendor/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceVendorExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── AuditWorker.java
│       ├── CertifyWorker.java
│       └── MonitorWorker.java
└── src/test/java/compliancevendor/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── AuditWorkerTest.java        # 2 tests
    ├── CertifyWorkerTest.java        # 2 tests
    └── MonitorWorkerTest.java        # 2 tests

```
