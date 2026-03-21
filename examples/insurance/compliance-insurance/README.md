# Insurance Compliance in Java with Conductor :  Audit, Assess, File Reports, Track, Certify

A Java Conductor workflow example demonstrating compliance-insurance Compliance Insurance. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Insurance Compliance Spans Multiple Regulatory Bodies

An insurance company must comply with state DOI regulations (rate filings, market conduct), NAIC requirements (financial reporting, RBC ratios), federal regulations (OFAC sanctions screening, anti-money laundering), and industry standards (data security, claims handling best practices). Each regulatory body has different reporting requirements, filing deadlines, and audit expectations.

Compliance gaps create regulatory risk. fines, license revocation, or consent orders. The compliance workflow audits current practices, identifies gaps against requirements, files required reports before deadlines, tracks remediation of any identified gaps, and certifies compliance for each regulatory body. Missing a filing deadline or failing to remediate a gap can trigger regulatory action.

## The Solution

**You just write the audit execution, gap assessment, regulatory filing, tracking, and certification logic. Conductor handles assessment retries, remediation routing, and regulatory audit trails.**

`AuditWorker` examines current practices against regulatory requirements. reviewing rate filings, claims handling procedures, financial ratios, and data security controls. `AssessWorker` identifies compliance gaps and risk areas with severity ratings and remediation priorities. `FileReportsWorker` prepares and submits required regulatory filings,  financial statements, market conduct reports, and statutory filings. `TrackWorker` monitors remediation progress for identified gaps with deadlines and accountable owners. `CertifyWorker` generates compliance certifications confirming adherence to regulatory requirements. Conductor tracks the full compliance lifecycle for regulatory examination readiness.

### What You Write: Workers

Regulation mapping, control assessment, gap identification, and remediation tracking workers each address one phase of insurance regulatory compliance.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `cpi_audit` | Audits current practices against regulatory requirements. reviews rate filings, claims handling procedures, financial ratios, and data security controls for the compliance period, outputting detailed findings |
| **AssessWorker** | `cpi_assess` | Assesses compliance gaps. analyzes audit findings to identify issues (2 minor issues found), assigns severity ratings, and creates a remediation plan with priorities and remediation items |
| **FileReportsWorker** | `cpi_file_reports` | Files required regulatory reports. prepares and submits filings to the specified regulatory body using the assessment data, returns the filingId for tracking |
| **TrackWorker** | `cpi_track` | Tracks remediation progress. monitors resolution of identified compliance items with deadlines and accountable owners, confirms all items resolved |
| **CertifyWorker** | `cpi_certify` | Generates the compliance certification. confirms all remediation items are resolved and produces the certification with complianceStatus and certificationId for the regulatory body |

Workers implement insurance operations. claim intake, assessment, settlement,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### The Workflow

```
cpi_audit
    │
    ▼
cpi_assess
    │
    ▼
cpi_file_reports
    │
    ▼
cpi_track
    │
    ▼
cpi_certify

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
java -jar target/compliance-insurance-1.0.0.jar

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
java -jar target/compliance-insurance-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cpi_compliance_insurance \
  --version 1 \
  --input '{"companyId": "TEST-001", "regulatoryBody": "sample-regulatoryBody", "compliancePeriod": "sample-compliancePeriod"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cpi_compliance_insurance -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real compliance stack. your GRC platform for audit tracking, your regulatory filing portals for report submission, your certification system for compliance attestation, and the workflow runs identically in production.

- **FileReportsWorker** (`cpi_file_reports`): submit NAIC annual statements via the NAIC filing system, state-specific reports via SERFF, and federal filings via FinCEN for BSA/AML compliance
- **AuditWorker** (`cpi_audit`): implement automated compliance checks against NAIC model laws, state-specific regulations, and OFAC sanctions lists using screening APIs
- **TrackWorker** (`cpi_track`): use GRC platforms (ServiceNow GRC, LogicGate) for tracking remediation tasks with regulatory deadline monitoring and escalation workflows

Update regulatory mappings or control frameworks and the compliance pipeline operates identically.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
compliance-insurance-compliance-insurance/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/complianceinsurance/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceInsuranceExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── AuditWorker.java
│       ├── CertifyWorker.java
│       ├── FileReportsWorker.java
│       └── TrackWorker.java
└── src/test/java/complianceinsurance/workers/
    ├── AuditWorkerTest.java        # 1 tests
    └── CertifyWorkerTest.java        # 1 tests

```
