# Compliance Monitoring in Java Using Conductor :  Resource Scanning, Policy Evaluation, Remediation, and Logging

A Java Conductor workflow example for compliance monitoring .  scanning infrastructure resources, evaluating them against compliance policies (CIS, SOC2, HIPAA), automatically remediating violations when possible, and logging compliant resources for audit evidence.

## The Problem

You need to continuously verify that your infrastructure complies with security and regulatory policies. Resources must be scanned for their current configuration, evaluated against compliance rules (encryption enabled, public access blocked, logging configured), violations must be remediated (close an open port, enable encryption), and compliant resources must be logged as evidence for auditors.

Without orchestration, compliance is a periodic manual audit. Scans run weekly, violations are tracked in spreadsheets, remediation is manual and delayed, and compliance evidence is scattered across tools. By the time a violation is found and fixed, weeks have passed and auditors are unhappy.

## The Solution

**You just write the policy evaluation rules and remediation actions. Conductor handles the scan-evaluate-remediate cycle with conditional routing, retries when cloud config APIs are unavailable, and timestamped proof of every compliance check and remediation action.**

Each compliance concern is an independent worker .  resource scanning, policy evaluation, remediation, and compliance logging. Conductor runs them in sequence with conditional routing: violations route to remediation, clean resources route to logging. Every compliance check is tracked ,  you can prove exactly when resources were evaluated, which policies were applied, and what remediation was performed. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers run the compliance loop: ScanResourcesWorker inventories infrastructure, EvaluatePoliciesWorker checks against CIS/SOC2/HIPAA rules, RemediateWorker auto-fixes violations where possible, and LogCompliantWorker records passing resources as audit evidence.

| Worker | Task | What It Does |
|---|---|---|
| **EvaluatePoliciesWorker** | `cpm_evaluate_policies` | Evaluates scanned resources against compliance policies, returning an overall status, compliance score, and list of violations |
| **LogCompliantWorker** | `cpm_log_compliant` | Logs successful compliance checks as audit evidence and issues a compliance certificate |
| **RemediateWorker** | `cpm_remediate` | Initiates remediation for violations. Auto-fixes where possible, creates tickets for manual remediation |
| **ScanResourcesWorker** | `cpm_scan_resources` | Scans infrastructure resources for a given compliance framework, returning resource count and findings |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cpm_scan_resources
    │
    ▼
cpm_evaluate_policies
    │
    ▼
SWITCH (cpm_switch_ref)
    ├── compliant: cpm_log_compliant
    └── default: cpm_remediate
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
java -jar target/compliance-monitoring-1.0.0.jar
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
java -jar target/compliance-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow compliance_monitoring_428 \
  --version 1 \
  --input '{"framework": "test-value", "scope": "test-value", "autoRemediate": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w compliance_monitoring_428 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one compliance step .  connect the scanner to AWS Config rules, the remediator to infrastructure-as-code (Terraform, CloudFormation), and the scan-evaluate-remediate-log workflow stays the same.

- **EvaluatePoliciesWorker** (`cpm_evaluate_policies`): evaluate against real policy frameworks using Open Policy Agent (OPA), AWS Config Rules, or custom rule engines
- **LogCompliantWorker** (`cpm_log_compliant`): record compliance evidence to a GRC platform (Vanta, Drata, Tugboat Logic) or an audit database
- **RemediateWorker** (`cpm_remediate`): auto-remediate violations .  close open ports, enable encryption, restrict public access ,  via cloud provider APIs

Integrate with AWS Config or your cloud provider's compliance tools, and the scan-remediate cycle carries forward without any workflow adjustments.

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
compliance-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/compliancemonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ComplianceMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EvaluatePoliciesWorker.java
│       ├── LogCompliantWorker.java
│       ├── RemediateWorker.java
│       └── ScanResourcesWorker.java
└── src/test/java/compliancemonitoring/workers/
    └── ScanResourcesWorkerTest.java        # 2 tests
```
