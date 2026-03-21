# Compliance Scanning in Java with Conductor :  Resource Discovery, Policy Scanning, Report Generation, and Auto-Remediation

Orchestrates infrastructure compliance scanning using [Conductor](https://github.com/conductor-oss/conductor). This workflow discovers all resources in a target environment, scans them against a compliance framework's policies (SOC 2, HIPAA, PCI-DSS, CIS benchmarks), generates an audit-ready compliance report with pass/fail results per policy, and auto-remediates fixable violations like open security groups or unencrypted storage.

## Staying Compliant at Scale

Your auditor asks for evidence that every S3 bucket has encryption enabled, every security group restricts SSH access, and every database has automated backups configured. You have 200 resources across three AWS accounts. Manually checking each one takes days, and by the time you finish, someone has already created a new unencrypted bucket. You need automated discovery of all resources in the environment, policy scanning against the specified compliance framework, a report that the auditor can read, and automatic remediation of the violations you can fix without human intervention.

Without orchestration, you'd write a single compliance script that inventories resources, checks policies, generates a PDF, and fixes violations in one pass. If remediation fails on one resource, there's no record of which policies passed and which didn't. There's no visibility into how many resources were scanned, how many violations were found, or whether auto-remediation actually fixed them. Re-running the script after fixing one issue means re-scanning everything from scratch.

## The Solution

**You write the compliance checks and remediation logic. Conductor handles scan-to-report sequencing, remediation gating, and audit trail generation.**

Each stage of the compliance pipeline is a simple, independent worker. The resource discoverer inventories all infrastructure resources in the target environment. EC2 instances, S3 buckets, RDS databases, security groups, IAM roles. The policy scanner checks each discovered resource against the specified compliance framework's rules and flags violations. The report generator produces an audit-ready compliance report with pass/fail results, violation details, and remediation recommendations. The remediator auto-fixes violations that have safe automated remediation paths (enabling encryption, restricting overly-permissive security groups, enabling logging). Conductor executes them in strict sequence, ensures remediation only runs after the report is generated, retries if the cloud API is rate-limited, and tracks resource counts and violation counts at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers execute the compliance pipeline. Discovering cloud resources, scanning against policy frameworks, generating audit reports, and auto-remediating violations.

| Worker | Task | What It Does |
|---|---|---|
| **DiscoverResourcesWorker** | `cs_discover_resources` | Inventories all cloud resources (EC2, S3, RDS, etc.) in the target environment |
| **GenerateReportWorker** | `cs_generate_report` | Produces a compliance audit report summarizing pass/fail status per policy |
| **RemediateWorker** | `cs_remediate` | Auto-remediates critical compliance findings (e.g., enabling encryption, restricting public access) |
| **ScanPoliciesWorker** | `cs_scan_policies` | Evaluates discovered resources against the specified compliance framework (e.g., CIS-AWS) |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### The Workflow

```
cs_discover_resources
    │
    ▼
cs_scan_policies
    │
    ▼
cs_generate_report
    │
    ▼
cs_remediate

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
java -jar target/compliance-scanning-1.0.0.jar

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
java -jar target/compliance-scanning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow compliance_scanning_workflow \
  --version 1 \
  --input '{"environment": "staging", "framework": "sample-framework"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w compliance_scanning_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one compliance stage .  plug in AWS Config, Open Policy Agent, or Checkov for real resource discovery and policy enforcement, and the scanning workflow runs unchanged.

- **DiscoverResourcesWorker** → inventory real cloud resources: AWS Config for multi-account resource listing, GCP Asset Inventory, Azure Resource Graph, or Kubernetes API for cluster resources
- **ScanPoliciesWorker** → run real policy checks: Open Policy Agent (OPA) with Rego policies, AWS Config Rules, Checkov for Terraform compliance, or kube-bench for CIS Kubernetes benchmarks
- **GenerateReportWorker** → produce real compliance artifacts: PDF reports for auditors, SARIF format for security tooling integration, or push findings to GRC platforms (Vanta, Drata, Secureframe)
- **RemediateWorker** → auto-fix real violations: enable S3 bucket encryption via AWS SDK, restrict security group rules, enable CloudTrail logging, or apply Kubernetes NetworkPolicies

Swap in AWS Config or Open Policy Agent for real scanning; the compliance pipeline maintains the same interface.

**Add new stages** by inserting tasks in `workflow.json`, for example, an approval step (using Conductor's WAIT task) that requires security team sign-off before auto-remediation, a drift detection step that compares current state against a known-good baseline, or a notification step that alerts the security channel when critical violations are found.

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
compliance-scanning-compliance-scanning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancescanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DiscoverResourcesWorker.java
│       ├── GenerateReportWorker.java
│       ├── RemediateWorker.java
│       └── ScanPoliciesWorker.java
└── src/test/java/compliancescanning/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
