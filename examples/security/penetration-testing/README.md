# Implementing Penetration Testing in Java with Conductor :  Reconnaissance, Vulnerability Scanning, Exploit Validation, and Reporting

A Java Conductor workflow example for automated penetration testing .  discovering target endpoints and open ports, scanning for known vulnerabilities, validating which findings are actually exploitable, and generating a remediation report. Uses [Conductor](https://github.

## The Problem

You need to run structured pen tests against external-facing systems. Each engagement follows the same pipeline: reconnaissance to enumerate endpoints and open ports on the target, vulnerability scanning to identify known CVEs and misconfigurations, exploit testing to confirm which vulnerabilities are actually exploitable (not just theoretical), and report generation with prioritized remediation steps.

Without orchestration, you'd script these phases into a single long-running process .  waiting for nmap output before launching a scanner, parsing scan results to decide which exploits to attempt, and hoping the whole thing doesn't crash four hours in. If the exploit phase fails mid-run, you lose all prior scan data and start over. Adding a new scanning tool means rewriting the control flow.

## The Solution

**You just write the recon scripts and vulnerability scanning integrations. Conductor handles phase sequencing so recon feeds the scanner, retries failed exploit tests without re-running the four-hour recon, and a complete record of every finding discovered.**

Each phase of the pen test is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so reconnaissance output feeds the vulnerability scanner, scan results drive exploit selection, and the final report captures everything. If an exploit test times out, Conductor retries it without re-running the four-hour recon phase. Every step's inputs and outputs are recorded, giving you a complete audit trail of what was tested and what was found.

### What You Write: Workers

Three workers execute the pen test pipeline: ReconnaissanceWorker enumerates endpoints and open ports, ScanVulnerabilitiesWorker identifies CVEs and misconfigurations, and GenerateReportWorker compiles findings with prioritized remediation steps.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateReportWorker** | `pen_generate_report` | Generates the pen test report with remediation steps. |
| **ReconnaissanceWorker** | `pen_reconnaissance` | Performs reconnaissance on the target system. |
| **ScanVulnerabilitiesWorker** | `pen_scan_vulnerabilities` | Scans for vulnerabilities in the target. |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
Input -> GenerateReportWorker -> ReconnaissanceWorker -> ScanVulnerabilitiesWorker -> Output

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
java -jar target/penetration-testing-1.0.0.jar

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
java -jar target/penetration-testing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow penetration_testing \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w penetration_testing -s COMPLETED -c 5

```

## How to Extend

Each worker runs one pen test phase .  connect ReconnaissanceWorker to nmap or Shodan, ScanVulnerabilitiesWorker to Nessus or OpenVAS, and the recon-scan-exploit-report workflow stays the same.

- **ReconnaissanceWorker** (`pen_reconnaissance`): integrate with nmap, Shodan API, or Amass to enumerate real endpoints, open ports, and running services on the target
- **ScanVulnerabilitiesWorker** (`pen_scan_vulnerabilities`): invoke Nessus, OpenVAS, or Qualys API to scan discovered endpoints against CVE databases and return severity-ranked findings
- **ExploitTestWorker** (`pen_exploit_test`): use Metasploit RPC API or custom exploit scripts to validate whether discovered vulnerabilities are actually exploitable in the target environment
- **GenerateReportWorker** (`pen_generate_report`): generate a PDF or HTML report with CVSS scores, proof-of-concept details, and prioritized remediation steps (e.g., via Dradis or PlexTrac API)

Connect to nmap, Nessus, or Metasploit, and the recon-scan-report orchestration persists without changes to the workflow.

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
penetration-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/penetrationtesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PenetrationTestingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateReportWorker.java
│       ├── ReconnaissanceWorker.java
│       └── ScanVulnerabilitiesWorker.java
└── src/test/java/penetrationtesting/workers/
    ├── ExploitTestWorkerTest.java        # 8 tests
    ├── GenerateReportWorkerTest.java        # 8 tests
    ├── ReconnaissanceWorkerTest.java        # 8 tests
    └── ScanVulnerabilitiesWorkerTest.java        # 8 tests

```
