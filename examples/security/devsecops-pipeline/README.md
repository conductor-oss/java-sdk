# Implementing DevSecOps Pipeline in Java with Conductor :  SAST, SCA, Container Scan, and Security Gate

A Java Conductor workflow example for a DevSecOps pipeline .  running SAST (static analysis), SCA (dependency scanning), and container security scans against a commit, then evaluating a security gate to approve or block the deployment.

## The Problem

You need to integrate security into your CI/CD pipeline. Every commit must be scanned for code vulnerabilities (SAST), dependency CVEs (SCA), and container image vulnerabilities before deployment. The scans run in parallel for speed, and a security gate evaluates the combined results .  blocking deployment if critical or high-severity findings exist, warning for medium, and passing for low.

Without orchestration, security scans are bolted onto CI/CD as optional stages that developers skip when they're in a hurry. Results from different scanners aren't consolidated, there's no unified security gate, and critical vulnerabilities make it to production because the SCA scan ran but nobody checked the results.

## The Solution

**You just write the scanner integrations and gate policy. Conductor handles parallel scan execution, consolidated finding aggregation for the security gate, and a full record of every scan result and gate decision per commit.**

Each security scan is an independent worker. SAST, SCA, and container scanning. Conductor runs them in parallel (they're independent) and feeds all results to a security gate worker that makes the go/no-go decision. Every pipeline run is tracked with findings per scanner, gate decisions, and exception approvals. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three security scanners and a gate worker form the pipeline: SastScanWorker finds code vulnerabilities, ScaScanWorker detects dependency CVEs, ContainerScanWorker checks container images, and SecurityGateWorker evaluates combined results to pass or block the deployment.

| Worker | Task | What It Does |
|---|---|---|
| **ContainerScanWorker** | `dso_container_scan` | Scans the container image for OS-level and package vulnerabilities |
| **SastScanWorker** | `dso_sast_scan` | Runs static application security testing to find code-level vulnerabilities |
| **ScaScanWorker** | `dso_sca_scan` | Runs software composition analysis to detect vulnerable dependencies |
| **SecurityGateWorker** | `dso_security_gate` | Evaluates all scan results against policy thresholds and passes or blocks the deployment |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
dso_sast_scan
    │
    ▼
dso_sca_scan
    │
    ▼
dso_container_scan
    │
    ▼
dso_security_gate

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
java -jar target/devsecops-pipeline-1.0.0.jar

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
java -jar target/devsecops-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow devsecops_pipeline_workflow \
  --version 1 \
  --input '{"repository": "sample-repository", "commitSha": "sample-commitSha"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w devsecops_pipeline_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker wraps one security scanner .  connect SastScanWorker to Semgrep or CodeQL, ScaScanWorker to Snyk, ContainerScanWorker to Trivy, and the parallel scan-then-gate workflow stays the same.

- **ContainerScanWorker** (`dso_container_scan`): scan container images using Trivy, Grype, or Prisma Cloud before they're pushed to the registry
- **SastScanWorker** (`dso_sast_scan`): run real SAST scans using Semgrep, CodeQL, SonarQube, or Checkmarx against the committed code
- **ScaScanWorker** (`dso_sca_scan`): run real SCA scans using Snyk, Dependabot, or OWASP Dependency-Check against dependency manifests

Swap in real scanner CLIs (Semgrep, Snyk, Trivy), and the parallel-scan-then-gate pipeline runs as before.

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
devsecops-pipeline-devsecops-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/devsecopspipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ContainerScanWorker.java
│       ├── SastScanWorker.java
│       ├── ScaScanWorker.java
│       └── SecurityGateWorker.java
└── src/test/java/devsecopspipeline/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
