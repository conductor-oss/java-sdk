# Vendor Onboarding in Java with Conductor :  Application, Credential Verification, Evaluation, Approval, and System Activation

A Java Conductor workflow example for vendor onboarding. receiving a new vendor application with business details, verifying their credentials (business licenses, insurance, certifications), evaluating the vendor on financial stability and capability, approving or rejecting based on the evaluation score, and activating approved vendors in the procurement system so they can receive purchase orders. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to onboard new vendors into your supply chain. A prospective vendor submits an application with their business details, category, and country of operation. Their credentials must be verified. business registration, insurance coverage, industry certifications (ISO, SOC 2). The vendor must be evaluated on financial health (D&B rating, credit check) and operational capability. Based on the evaluation score, procurement approves or rejects the application. Approved vendors must be activated in the ERP vendor master so buyers can issue purchase orders to them.

Without orchestration, vendor applications arrive via email, sit in someone's inbox for weeks, and credential checks happen over phone calls with no record. Vendors get activated in the ERP before their insurance is verified, exposing the company to liability risk. When compliance asks which vendors were onboarded last quarter and what checks were performed, nobody can produce the documentation without hours of email archaeology.

## The Solution

**You just write the onboarding workers. Application intake, credential verification, evaluation, approval, and system activation. Conductor handles credential verification retries, enforced approval gates, and timestamped records for compliance audits.**

Each stage of the vendor onboarding process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so applications are received before verification, credentials are verified before evaluation, evaluation gates approval, and activation only happens for approved vendors. If the credential verification API is temporarily unavailable, Conductor retries without losing the application data. Every application, verification result, evaluation score, approval decision, and activation timestamp is recorded for compliance audits and vendor management analytics.

### What You Write: Workers

Five workers manage vendor onboarding: ApplyWorker receives the application, VerifyWorker checks credentials and insurance, EvaluateWorker scores financial health, ApproveWorker decides eligibility, and ActivateWorker enables the vendor in the ERP.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `von_activate` | Activates the vendor in the system. |
| **ApplyWorker** | `von_apply` | Submits a vendor application. |
| **ApproveWorker** | `von_approve` | Approves or rejects a vendor based on score. |
| **EvaluateWorker** | `von_evaluate` | Evaluates the vendor and assigns a score. |
| **VerifyWorker** | `von_verify` | Verifies vendor credentials. |

Workers implement supply chain operations. inventory checks, shipment tracking, supplier coordination,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### The Workflow

```
von_apply
    │
    ▼
von_verify
    │
    ▼
von_evaluate
    │
    ▼
von_approve
    │
    ▼
von_activate

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
java -jar target/vendor-onboarding-1.0.0.jar

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
java -jar target/vendor-onboarding-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow von_vendor_onboarding \
  --version 1 \
  --input '{"vendorName": "test", "category": "general", "country": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w von_vendor_onboarding -s COMPLETED -c 5

```

## How to Extend

Connect VerifyWorker to your background check provider (D&B, Aravo), EvaluateWorker to your vendor risk scoring model, and ActivateWorker to your ERP vendor master. The workflow definition stays exactly the same.

- **ApplyWorker** (`von_apply`): receive applications via your supplier portal (SAP Ariba, Coupa) or a custom form, validating required fields and document uploads
- **VerifyWorker** (`von_verify`): check business registration against government databases, verify insurance certificates, and validate certifications (ISO, SOC 2) against issuing body registries
- **EvaluateWorker** (`von_evaluate`): pull financial health data from Dun & Bradstreet or Experian, run sanctions/watchlist screening (OFAC, EU sanctions lists), and compute a composite vendor risk score
- **ApproveWorker** (`von_approve`): route the approval to the category manager based on the evaluation score, auto-approve high-score vendors, or escalate borderline cases for committee review
- **ActivateWorker** (`von_activate`): create the vendor master record in your ERP (SAP, Oracle, NetSuite), set payment terms, assign commodity codes, and enable the vendor for purchase order receipt

Connect any worker to your vendor management platform while preserving its output schema, and the onboarding pipeline needs no reconfiguration.

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
vendor-onboarding/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/vendoronboarding/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VendorOnboardingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActivateWorker.java
│       ├── ApplyWorker.java
│       ├── ApproveWorker.java
│       ├── EvaluateWorker.java
│       └── VerifyWorker.java
└── src/test/java/vendoronboarding/workers/
    ├── ActivateWorkerTest.java        # 2 tests
    ├── ApplyWorkerTest.java        # 2 tests
    ├── ApproveWorkerTest.java        # 3 tests
    ├── EvaluateWorkerTest.java        # 2 tests
    └── VerifyWorkerTest.java        # 2 tests

```
