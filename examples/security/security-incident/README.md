# Implementing Security Incident Response in Java with Conductor :  Triage, Containment, Investigation, and Remediation

A Java Conductor workflow example for security incident response. triaging alerts by type and severity, containing the threat by isolating affected systems, investigating root cause through log and forensic analysis, and applying remediation fixes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to respond to security incidents. unauthorized access attempts, data exfiltration, compromised credentials,  following a structured playbook: triage the alert to determine severity (P1 through P4), contain the threat by isolating the affected system (e.g., an API gateway), investigate to identify root cause and blast radius, and remediate by patching the vulnerability or revoking compromised credentials. Mean time to containment directly impacts breach severity.

Without orchestration, your incident response is a runbook document that engineers follow manually at 2 AM. Steps get skipped under pressure, containment is delayed while waiting for approvals, and there is no reliable record of what was done or when. If the on-call engineer's laptop crashes mid-investigation, the incident state lives in their browser tabs and terminal history.

## The Solution

**You just write the triage rules and containment actions. Conductor handles the ordered response playbook, retries containment if the first attempt fails, and a complete incident timeline for post-mortem review and regulatory reporting.**

Each phase of the incident response playbook is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so triage output drives containment scope, containment completes before investigation begins, and remediation only runs after root cause is confirmed. If containment fails on the first attempt, Conductor retries automatically. Every action, decision, and timing is recorded,  giving you a complete incident timeline for post-mortem review and regulatory reporting.

### What You Write: Workers

Four workers execute the incident response playbook: TriageWorker classifies severity and type, ContainWorker isolates the affected system, InvestigateWorker determines root cause and blast radius, and RemediateWorker applies the fix or credential revocation.

| Worker | Task | What It Does |
|---|---|---|
| **ContainWorker** | `si_contain` | Contains a security incident by isolating the affected system. |
| **InvestigateWorker** | `si_investigate` | Investigates a security incident to determine root cause. |
| **RemediateWorker** | `si_remediate` | Remediates a security incident by applying fixes. |
| **TriageWorker** | `si_triage` | Triages a security incident based on type and severity. |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
Input -> ContainWorker -> InvestigateWorker -> RemediateWorker -> TriageWorker -> Output

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
java -jar target/security-incident-1.0.0.jar

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
java -jar target/security-incident-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow security_incident \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w security_incident -s COMPLETED -c 5

```

## How to Extend

Each worker handles one IR phase. connect TriageWorker to your SIEM (Splunk, Elastic Security), ContainWorker to CrowdStrike's network containment API, and the triage-contain-investigate-remediate workflow stays the same.

- **TriageWorker** (`si_triage`): pull alert details from your SIEM (Splunk, Sentinel, Elastic Security) and classify severity based on IOC matching and affected asset criticality
- **ContainWorker** (`si_contain`): isolate the compromised host via AWS Security Groups, CrowdStrike network containment API, or firewall rule injection to stop lateral movement
- **InvestigateWorker** (`si_investigate`): query CloudTrail/VPC Flow Logs, pull forensic images, or invoke an EDR timeline API to determine root cause and blast radius
- **RemediateWorker** (`si_remediate`): rotate compromised credentials via Vault, deploy patches via your CI/CD pipeline, or update WAF rules to block the attack vector

Connect to your SIEM and EDR APIs, and the triage-contain-investigate-remediate playbook runs in production with no orchestration changes.

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
security-incident/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/securityincident/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SecurityIncidentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ContainWorker.java
│       ├── InvestigateWorker.java
│       ├── RemediateWorker.java
│       └── TriageWorker.java
└── src/test/java/securityincident/workers/
    ├── ContainWorkerTest.java        # 8 tests
    ├── InvestigateWorkerTest.java        # 8 tests
    ├── RemediateWorkerTest.java        # 8 tests
    └── TriageWorkerTest.java        # 8 tests

```
