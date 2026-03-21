# Implementing Security Awareness Training in Java with Conductor :  Module Assignment, Phishing Simulation, Evaluation, and Compliance Reporting

A Java Conductor workflow example for automated security awareness campaigns .  assigning training modules (e.g., secure coding) to department employees, running phishing simulations to test real-world awareness, evaluating completion rates and click-through results, and generating compliance reports. Uses [Conductor](https://github.

## The Problem

You need to run security awareness campaigns across your organization. Each campaign involves assigning training modules to a department's employees, sending simulated phishing emails to test their real-world response, evaluating who completed the training and who clicked the phishing link, and generating a compliance report showing pass/fail rates. Regulatory frameworks like SOC 2 and ISO 27001 require documented evidence that these campaigns ran and that results were recorded.

Without orchestration, you'd manage training assignments in one spreadsheet, phishing simulations in a separate tool, and manually compile results into a report. If the phishing simulation fails to send to half the department, you don't know which employees were actually tested. Compliance asks for proof that engineering completed the "secure-coding-2024" module, and you spend hours cross-referencing three different systems.

## The Solution

**You just write the LMS integration and phishing simulation logic. Conductor handles campaign sequencing so phishing tests only run after training is assigned, retries if the simulation platform is temporarily down, and timestamped evidence for SOC2 and ISO 27001 auditors.**

Each phase of the awareness campaign is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so training is assigned before phishing tests are sent, simulation results feed directly into the evaluation step, and the compliance report captures everything end-to-end. If the phishing simulation worker fails partway through, Conductor retries without re-sending to employees who already received the test. Every assignment, simulation, and evaluation result is tracked with timestamps for audit evidence.

### What You Write: Workers

Four workers run the awareness campaign: StAssignTrainingWorker assigns modules to employees, StSendPhishingSimWorker delivers simulated phishing emails, StEvaluateResultsWorker analyzes completion rates and click-through data, and StReportComplianceWorker generates the compliance report for auditors.

| Worker | Task | What It Does |
|---|---|---|
| **StAssignTrainingWorker** | `st_assign_training` | Assigns security training modules to department employees. |
| **StEvaluateResultsWorker** | `st_evaluate_results` | Evaluates training completion and phishing simulation results. |
| **StReportComplianceWorker** | `st_report_compliance` | Generates training compliance report. |
| **StSendPhishingSimWorker** | `st_send_phishing_sim` | Sends phishing simulation to department employees. |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
Input -> StAssignTrainingWorker -> StEvaluateResultsWorker -> StReportComplianceWorker -> StSendPhishingSimWorker -> Output

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
java -jar target/security-training-1.0.0.jar

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
java -jar target/security-training-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow security_training \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w security_training -s COMPLETED -c 5

```

## How to Extend

Each worker runs one campaign step .  connect StAssignTrainingWorker to KnowBe4 or your LMS, StSendPhishingSimWorker to GoPhish, and the assign-simulate-evaluate-report workflow stays the same.

- **StAssignTrainingWorker** (`st_assign_training`): assign modules via your LMS API (KnowBe4, Proofpoint Security Awareness, or a custom LMS), pulling employee lists from your HR system or Active Directory
- **StSendPhishingSimWorker** (`st_send_phishing_sim`): launch phishing simulations via KnowBe4's API or GoPhish, tracking which employees receive each simulated email and their click/report responses
- **StEvaluateResultsWorker** (`st_evaluate_results`): pull training completion status from the LMS and phishing click rates from the simulation platform, flagging employees who failed both
- **StReportComplianceWorker** (`st_report_compliance`): generate a PDF or CSV compliance report with per-department pass rates, upload to your GRC platform (Drata, Vanta, ServiceNow GRC) for auditor access

Integrate with KnowBe4 or GoPhish, and the assign-simulate-evaluate-report campaign orchestration works without any workflow changes.

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
security-training/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/securitytraining/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SecurityTrainingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── StAssignTrainingWorker.java
│       ├── StEvaluateResultsWorker.java
│       ├── StReportComplianceWorker.java
│       └── StSendPhishingSimWorker.java
└── src/test/java/securitytraining/workers/
    └── StAssignTrainingWorkerTest.java        # 8 tests

```
