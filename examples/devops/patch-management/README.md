# Patch Management in Java with Conductor :  Scan Vulnerabilities, Deploy Patch, Test, Verify

Automates security patch management using [Conductor](https://github.com/conductor-oss/conductor). This workflow scans systems for known vulnerabilities, deploys patches to affected hosts in a rolling fashion, and verifies all hosts are patched and healthy afterward. You write the patching logic, Conductor handles retries, failure routing, durability, and observability for free.

## Patching Before the Exploit Lands

A critical CVE was published yesterday affecting OpenSSL on your fleet of 200 servers. You need to know which hosts are vulnerable, patch them without taking down the entire fleet at once (rolling deployment), and verify every host is running the patched version and still healthy. Missing a host means leaving a known vulnerability exposed. Patching too aggressively means a service outage if the patch has a regression.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the vulnerability scanning and patching logic. Conductor handles rolling deployment sequencing, verification gates, and compliance reporting.**

`ScanVulnerabilitiesWorker` identifies systems affected by known vulnerabilities, prioritizing by CVSS severity score and exposure level. `DeployPatchWorker` applies the security patch to the targeted systems .  updating packages, applying hotfixes, or deploying updated container images. `TestPatchWorker` runs functional tests on the patched systems to confirm no regressions ,  application health checks, integration tests, and load tests. `VerifyPatchWorker` re-scans the patched systems to confirm the vulnerability is remediated and no new vulnerabilities were introduced. Conductor sequences these steps and records the patch lifecycle for compliance reporting.

### What You Write: Workers

Three workers manage the patching cycle. Scanning for vulnerabilities, deploying patches in rolling fashion, and verifying remediation.

| Worker | Task | What It Does |
|---|---|---|
| **DeployPatch** | `pm_deploy_patch` | Deploys a patch to affected hosts in rolling fashion. |
| **ScanVulnerabilities** | `pm_scan_vulnerabilities` | Scans systems for vulnerabilities matching a given patch. |
| **VerifyPatch** | `pm_verify_patch` | Verifies that all hosts are patched and healthy. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> DeployPatch -> ScanVulnerabilities -> VerifyPatch -> Output
```

## Example Output

```
=== Patch Management Demo: Automated Security Patch Deployment ===

Step 1: Registering task definitions...
  Registered: pm_deploy_patch, pm_scan_vulnerabilities, pm_test_patch, pm_verify_patch

Step 2: Registering workflow 'patch_management_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [pm_deploy_patch] Patch deployed to hosts in rolling fashion (staging passed:
  [pm_scan_vulnerabilities] Patch
  [pm_test_patch] Patch
  [pm_verify_patch] All

  Status: COMPLETED
  Output: {deployed=..., hostsPatched=..., deploymentStrategy=..., processed=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/patch-management-1.0.0.jar
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
java -jar target/patch-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow patch_management \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w patch_management -s COMPLETED -c 5
```

## How to Extend

Each worker handles one patch lifecycle step .  replace the simulated calls with Qualys or Nessus for vulnerability scanning and Ansible for OS-level patching, and the patch management workflow runs unchanged.

- **ScanVulnerabilitiesWorker**: integrate with Qualys, Nessus, Trivy (for containers), or AWS Inspector for real vulnerability scanning with CVE correlation
- **DeployPatchWorker**: use Ansible for OS-level patching, yum/apt for package updates, or update container base images and redeploy via CI/CD
- **VerifyPatchWorker**: re-scan with the same vulnerability scanner to confirm remediation, check NVD for new advisories, and generate compliance reports for auditors

Integrate with your vulnerability scanner and package manager; the patching pipeline maintains the same scan-deploy-verify contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
patch-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/patchmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PatchManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeployPatch.java
│       ├── ScanVulnerabilities.java
│       └── VerifyPatch.java
└── src/test/java/patchmanagement/workers/
    ├── DeployPatchTest.java        # 8 tests
    ├── ScanVulnerabilitiesTest.java        # 9 tests
    ├── TestPatchTest.java        # 8 tests
    └── VerifyPatchTest.java        # 8 tests
```
