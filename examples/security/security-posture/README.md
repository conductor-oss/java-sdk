# Implementing Security Posture Assessment in Java with Conductor :  Infrastructure, Application, and Compliance Scoring

A Java Conductor workflow example for security posture assessment. evaluating infrastructure security, application security, and compliance status in parallel via FORK/JOIN, then computing a unified security posture score.

## The Problem

You need a unified view of your organization's security posture. not just infrastructure (firewalls, encryption, patching) or application security (code vulnerabilities, dependency risks) or compliance (SOC2, HIPAA controls) in isolation, but all three combined into a single score that executives can understand and track over time.

Without orchestration, security posture is measured in silos. the infrastructure team has their metrics, the application security team has theirs, and compliance has a separate assessment. Nobody can answer "how secure are we overall?" because the data isn't consolidated.

## The Solution

**You just write the domain-specific security assessments. Conductor handles parallel assessment execution, score aggregation across domains, and trend tracking of posture scores over time.**

Conductor's FORK/JOIN evaluates infrastructure, application, and compliance security in parallel. A scoring worker combines all three assessments into a unified security posture score with breakdown by domain. Every assessment is tracked with detailed findings per domain and trend data over time. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four domain-specific assessors run in parallel: AssessInfrastructureWorker evaluates firewalls and patching, AssessApplicationWorker scores code vulnerabilities, AssessComplianceWorker checks framework adherence, and CalculateScoreWorker merges them into a unified posture grade.

| Worker | Task | What It Does |
|---|---|---|
| **AssessApplicationWorker** | `sp_assess_application` | Scores application security. counts critical vulnerabilities in production code and dependencies |
| **AssessComplianceWorker** | `sp_assess_compliance` | Scores compliance status against frameworks (SOC2, HIPAA) and flags overdue reviews |
| **AssessInfrastructureWorker** | `sp_assess_infrastructure` | Scores infrastructure security. evaluates firewall rules, encryption, and patching gaps |
| **CalculateScoreWorker** | `sp_calculate_score` | Computes a unified security posture score (letter grade and numeric) from all domain assessments |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
sp_assess_infrastructure
    │
    ▼
sp_assess_application
    │
    ▼
sp_assess_compliance
    │
    ▼
sp_calculate_score

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
java -jar target/security-posture-1.0.0.jar

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
java -jar target/security-posture-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow security_posture_workflow \
  --version 1 \
  --input '{"organization": "sample-organization", "assessmentScope": "sample-assessmentScope"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w security_posture_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker assesses one security domain. connect AssessInfrastructureWorker to AWS Config, AssessApplicationWorker to your SAST/DAST tools, AssessComplianceWorker to your GRC platform, and the parallel assessment workflow stays the same.

- **AssessApplicationWorker** (`sp_assess_application`): assess application security. SAST/DAST findings, dependency vulnerabilities, security header configuration
- **AssessComplianceWorker** (`sp_assess_compliance`): check compliance status against SOC2, HIPAA, PCI-DSS, or ISO 27001 using your GRC platform or manual controls
- **AssessInfrastructureWorker** (`sp_assess_infrastructure`): evaluate real infrastructure security. firewall rules, encryption at rest/transit, patching status, network segmentation

Connect each assessor to your real security tools, and the parallel assessment-to-score pipeline continues functioning unmodified.

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
security-posture-security-posture/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/securityposture/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessApplicationWorker.java
│       ├── AssessComplianceWorker.java
│       ├── AssessInfrastructureWorker.java
│       └── CalculateScoreWorker.java
└── src/test/java/securityposture/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
