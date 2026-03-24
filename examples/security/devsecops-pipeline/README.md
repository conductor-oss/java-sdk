# Implementing DevSecOps Pipeline in Java with Conductor : SAST, SCA, Container Scan, and Security Gate

## The Problem

You need to integrate security into your CI/CD pipeline. Every commit must be scanned for code vulnerabilities (SAST), dependency CVEs (SCA), and container image vulnerabilities before deployment. The scans run in parallel for speed, and a security gate evaluates the combined results. blocking deployment if critical or high-severity findings exist, warning for medium, and passing for low.

Without orchestration, security scans are bolted onto CI/CD as optional stages that developers skip when they're in a hurry. Results from different scanners aren't consolidated, there's no unified security gate, and critical vulnerabilities make it to production because the SCA scan ran but nobody checked the results.

## The Solution

**You just write the scanner integrations and gate policy. Conductor handles parallel scan execution, consolidated finding aggregation for the security gate, and a full record of every scan result and gate decision per commit.**

Each security scan is an independent worker. SAST, SCA, and container scanning. Conductor runs them in parallel (they're independent) and feeds all results to a security gate worker that makes the go/no-go decision. Every pipeline run is tracked with findings per scanner, gate decisions, and exception approvals.

### What You Write: Workers

Three security scanners and a gate worker form the pipeline: SastScanWorker finds code vulnerabilities, ScaScanWorker detects dependency CVEs, ContainerScanWorker checks container images, and SecurityGateWorker evaluates combined results to pass or block the deployment.

| Worker | Task | What It Does |
|---|---|---|
| **ContainerScanWorker** | `dso_container_scan` | Scans the container image for OS-level and package vulnerabilities |
| **SastScanWorker** | `dso_sast_scan` | Runs static application security testing to find code-level vulnerabilities |
| **ScaScanWorker** | `dso_sca_scan` | Runs software composition analysis to detect vulnerable dependencies |
| **SecurityGateWorker** | `dso_security_gate` | Evaluates all scan results against policy thresholds and passes or blocks the deployment |

the workflow logic stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
