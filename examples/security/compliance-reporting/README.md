# Implementing Compliance Reporting in Java with Conductor :  Evidence Collection, Control Mapping, Gap Assessment, and Report Generation

A Java Conductor workflow example automating compliance report generation .  collecting evidence artifacts (access logs, configuration snapshots, policy documents) for a target framework (SOC 2, ISO 27001, HIPAA, PCI-DSS), mapping each evidence item to the control objectives it satisfies, identifying gaps where controls lack sufficient evidence, and generating the final compliance report for auditor review. Uses [Conductor](https://github.## The Problem

You need to produce compliance reports that demonstrate your organization meets the requirements of a regulatory framework. This means gathering hundreds of evidence items .  access review logs, network diagrams, encryption configurations, incident response records, change management tickets, and mapping each one to the specific control objective it satisfies (e.g., SOC 2 CC6.1 for logical access, ISO 27001 A.12.3 for backup verification). After mapping, you must identify gaps where a control has insufficient or missing evidence, so remediation can happen before the audit. The final report must be structured for auditor consumption, with clear traceability from each control to its supporting evidence.

Without orchestration, compliance reporting is a quarterly fire drill. Someone manually collects screenshots and exports from a dozen tools (AWS Config, Jira, Confluence, Slack, HR systems), dumps them into a shared drive, and tries to match them to a spreadsheet of control objectives. When evidence is missing, nobody notices until the auditor asks for it. The mapping is done in spreadsheets that drift out of sync with the actual controls framework. Different team members collect evidence differently, so the report quality varies wildly. And when the auditor requests a re-run with updated evidence, the entire manual process starts over.

## The Solution

**You just write the evidence collection and control mapping logic. Conductor handles the evidence-to-report pipeline, retries when source systems are temporarily unavailable, and a proof chain showing when evidence was collected and how controls were mapped.**

Each compliance reporting step is a simple, independent worker .  one collects evidence artifacts from your systems for the specified framework and reporting period, one maps each evidence item to the control objectives it satisfies, one identifies gaps where controls lack sufficient evidence, one generates the structured compliance report for auditor review. Conductor takes care of executing them in strict order so no report is generated without a complete gap assessment, retrying if an evidence source is temporarily unavailable, and maintaining a complete audit trail that proves when evidence was collected, how controls were mapped, and what gaps were identified for every reporting cycle. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The reporting pipeline uses CollectEvidenceWorker to gather artifacts from connected systems, MapControlsWorker to link evidence to control objectives, AssessGapsWorker to identify insufficient coverage, and GenerateReportWorker to produce the auditor-ready package.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEvidenceWorker** | `cr_collect_evidence` | Gathers evidence artifacts (access logs, config snapshots, policy documents, change tickets) from connected systems for the specified compliance framework and reporting period |
| **MapControlsWorker** | `cr_map_controls` | Maps each collected evidence item to the control objectives it satisfies (e.g., SOC 2 CC6.1, ISO 27001 A.12.3) and tracks coverage across the full controls matrix |
| **AssessGapsWorker** | `cr_assess_gaps` | Identifies controls that lack sufficient evidence .  missing artifacts, expired certifications, stale configurations, and flags them for remediation before audit |
| **GenerateReportWorker** | `cr_generate_report` | Produces the structured compliance report with control-to-evidence traceability, gap summaries, and remediation status for auditor review |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
cr_collect_evidence
    │
    ▼
cr_map_controls
    │
    ▼
cr_assess_gaps
    │
    ▼
cr_generate_report
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
java -jar target/compliance-reporting-1.0.0.jar
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
java -jar target/compliance-reporting-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow compliance_reporting \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w compliance_reporting -s COMPLETED -c 5
```

## How to Extend

Each worker covers one reporting phase .  connect CollectEvidenceWorker to AWS Config and Jira, MapControlsWorker to your GRC platform (Vanta, Drata), and the collect-map-assess-report workflow stays the same.

- **CollectEvidenceWorker** (`cr_collect_evidence`): pull evidence from real sources: AWS Config rules, CloudTrail logs, Jira change tickets, Confluence policy pages, HR onboarding/offboarding records, and vulnerability scanner exports (Qualys, Nessus)
- **MapControlsWorker** (`cr_map_controls`): use a controls framework database (Drata, Vanta, or a custom mapping table) to automatically match evidence artifacts to control objectives based on evidence type and source
- **AssessGapsWorker** (`cr_assess_gaps`): query your GRC platform (ServiceNow GRC, Archer, LogicGate) for control status and identify controls with missing, expired, or insufficient evidence for the audit window
- **GenerateReportWorker** (`cr_generate_report`): produce the final report in the auditor's required format (SOC 2 Type II narrative, ISO 27001 Statement of Applicability), export as PDF, and upload to the audit portal

Connect each worker to your real evidence sources and GRC platform, and the compliance reporting pipeline runs at audit time without any workflow edits.

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
compliance-reporting/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancereporting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceReportingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessGapsWorker.java
│       ├── CollectEvidenceWorker.java
│       ├── GenerateReportWorker.java
│       └── MapControlsWorker.java
└── src/test/java/compliancereporting/workers/
    ├── AssessGapsWorkerTest.java        # 8 tests
    ├── CollectEvidenceWorkerTest.java        # 8 tests
    ├── GenerateReportWorkerTest.java        # 8 tests
    └── MapControlsWorkerTest.java        # 8 tests
```
