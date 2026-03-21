# Patch Management in Java with Conductor : Scan Vulnerabilities, Deploy Patch, Test, Verify

Automates security patch management using [Conductor](https://github.com/conductor-oss/conductor). This workflow scans systems for known vulnerabilities, deploys patches to affected hosts in a rolling fashion, and verifies all hosts are patched and healthy afterward.

## Patching Before the Exploit Lands

A critical CVE was published yesterday affecting OpenSSL on your fleet of 200 servers. You need to know which hosts are vulnerable, patch them without taking down the entire fleet at once (rolling deployment), and verify every host is running the patched version and still healthy. Missing a host means leaving a known vulnerability exposed. Patching too aggressively means a service outage if the patch has a regression.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the vulnerability scanning and patching logic. Conductor handles rolling deployment sequencing, verification gates, and compliance reporting.**

`ScanVulnerabilitiesWorker` identifies systems affected by known vulnerabilities, prioritizing by CVSS severity score and exposure level. `DeployPatchWorker` applies the security patch to the targeted systems. updating packages, applying hotfixes, or deploying updated container images. `TestPatchWorker` runs functional tests on the patched systems to confirm no regressions, application health checks, integration tests, and load tests. `VerifyPatchWorker` re-scans the patched systems to confirm the vulnerability is remediated and no new vulnerabilities were introduced. Conductor sequences these steps and records the patch lifecycle for compliance reporting.

### What You Write: Workers

Three workers manage the patching cycle. Scanning for vulnerabilities, deploying patches in rolling fashion, and verifying remediation.

| Worker | Task | What It Does |
|---|---|---|
| **DeployPatch** | `pm_deploy_patch` | Deploys a patch to affected hosts in rolling fashion. |
| **ScanVulnerabilities** | `pm_scan_vulnerabilities` | Scans systems for vulnerabilities matching a given patch. |
| **VerifyPatch** | `pm_verify_patch` | Verifies that all hosts are patched and healthy. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> DeployPatch -> ScanVulnerabilities -> VerifyPatch -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
