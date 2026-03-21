# Compliance Scanning in Java with Conductor : Resource Discovery, Policy Scanning, Report Generation, and Auto-Remediation

Orchestrates infrastructure compliance scanning using [Conductor](https://github.com/conductor-oss/conductor). This workflow discovers all resources in a target environment, scans them against a compliance framework's policies (SOC 2, HIPAA, PCI-DSS, CIS benchmarks), generates an audit-ready compliance report with pass/fail results per policy, and auto-remediates fixable violations like open security groups or unencrypted storage.

## Staying Compliant at Scale

Your auditor asks for evidence that every S3 bucket has encryption enabled, every security group restricts SSH access, and every database has automated backups configured. You have 200 resources across three AWS accounts. Manually checking each one takes days, and by the time you finish, someone has already created a new unencrypted bucket. You need automated discovery of all resources in the environment, policy scanning against the specified compliance framework, a report that the auditor can read, and automatic remediation of the violations you can fix without human intervention.

Without orchestration, you'd write a single compliance script that inventories resources, checks policies, generates a PDF, and fixes violations in one pass. If remediation fails on one resource, there's no record of which policies passed and which didn't. There's no visibility into how many resources were scanned, how many violations were found, or whether auto-remediation actually fixed them. Re-running the script after fixing one issue means re-scanning everything from scratch.

## The Solution

**You write the compliance checks and remediation logic. Conductor handles scan-to-report sequencing, remediation gating, and audit trail generation.**

Each stage of the compliance pipeline is a simple, independent worker. The resource discoverer inventories all infrastructure resources in the target environment. EC2 instances, S3 buckets, RDS databases, security groups, IAM roles. The policy scanner checks each discovered resource against the specified compliance framework's rules and flags violations. The report generator produces an audit-ready compliance report with pass/fail results, violation details, and remediation recommendations. The remediator auto-fixes violations that have safe automated remediation paths (enabling encryption, restricting overly-permissive security groups, enabling logging). Conductor executes them in strict sequence, ensures remediation only runs after the report is generated, retries if the cloud API is rate-limited, and tracks resource counts and violation counts at every stage. ### What You Write: Workers

Four workers execute the compliance pipeline. Discovering cloud resources, scanning against policy frameworks, generating audit reports, and auto-remediating violations.

| Worker | Task | What It Does |
|---|---|---|
| **DiscoverResourcesWorker** | `cs_discover_resources` | Inventories all cloud resources (EC2, S3, RDS, etc.) in the target environment |
| **GenerateReportWorker** | `cs_generate_report` | Produces a compliance audit report summarizing pass/fail status per policy |
| **RemediateWorker** | `cs_remediate` | Auto-remediates critical compliance findings (e.g., enabling encryption, restricting public access) |
| **ScanPoliciesWorker** | `cs_scan_policies` | Evaluates discovered resources against the specified compliance framework (e.g., CIS-AWS) |

the workflow and rollback logic stay the same.

### The Workflow

```
cs_discover_resources
 │
 ▼
cs_scan_policies
 │
 ▼
cs_generate_report
 │
 ▼
cs_remediate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
