# Compliance Monitoring in Java Using Conductor : Resource Scanning, Policy Evaluation, Remediation, and Logging

## The Problem

You need to continuously verify that your infrastructure complies with security and regulatory policies. Resources must be scanned for their current configuration, evaluated against compliance rules (encryption enabled, public access blocked, logging configured), violations must be remediated (close an open port, enable encryption), and compliant resources must be logged as evidence for auditors.

Without orchestration, compliance is a periodic manual audit. Scans run weekly, violations are tracked in spreadsheets, remediation is manual and delayed, and compliance evidence is scattered across tools. By the time a violation is found and fixed, weeks have passed and auditors are unhappy.

## The Solution

**You just write the policy evaluation rules and remediation actions. Conductor handles the scan-evaluate-remediate cycle with conditional routing, retries when cloud config APIs are unavailable, and timestamped proof of every compliance check and remediation action.**

Each compliance concern is an independent worker. resource scanning, policy evaluation, remediation, and compliance logging. Conductor runs them in sequence with conditional routing: violations route to remediation, clean resources route to logging. Every compliance check is tracked, you can prove exactly when resources were evaluated, which policies were applied, and what remediation was performed.

### What You Write: Workers

Four workers run the compliance loop: ScanResourcesWorker inventories infrastructure, EvaluatePoliciesWorker checks against CIS/SOC2/HIPAA rules, RemediateWorker auto-fixes violations where possible, and LogCompliantWorker records passing resources as audit evidence.

| Worker | Task | What It Does |
|---|---|---|
| **EvaluatePoliciesWorker** | `cpm_evaluate_policies` | Evaluates scanned resources against compliance policies, returning an overall status, compliance score, and list of violations |
| **LogCompliantWorker** | `cpm_log_compliant` | Logs successful compliance checks as audit evidence and issues a compliance certificate |
| **RemediateWorker** | `cpm_remediate` | Initiates remediation for violations. Auto-fixes where possible, creates tickets for manual remediation |
| **ScanResourcesWorker** | `cpm_scan_resources` | Scans infrastructure resources for a given compliance framework, returning resource count and findings |

the schedule triggers, retry behavior, and monitoring stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
