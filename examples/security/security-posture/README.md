# Implementing Security Posture Assessment in Java with Conductor : Infrastructure, Application, and Compliance Scoring

## The Problem

You need a unified view of your organization's security posture. not just infrastructure (firewalls, encryption, patching) or application security (code vulnerabilities, dependency risks) or compliance (SOC2, HIPAA controls) in isolation, but all three combined into a single score that executives can understand and track over time.

Without orchestration, security posture is measured in silos. the infrastructure team has their metrics, the application security team has theirs, and compliance has a separate assessment. Nobody can answer "how secure are we overall?" because the data isn't consolidated.

## The Solution

**You just write the domain-specific security assessments. Conductor handles parallel assessment execution, score aggregation across domains, and trend tracking of posture scores over time.**

Conductor's FORK/JOIN evaluates infrastructure, application, and compliance security in parallel. A scoring worker combines all three assessments into a unified security posture score with breakdown by domain. Every assessment is tracked with detailed findings per domain and trend data over time. ### What You Write: Workers

Four domain-specific assessors run in parallel: AssessInfrastructureWorker evaluates firewalls and patching, AssessApplicationWorker scores code vulnerabilities, AssessComplianceWorker checks framework adherence, and CalculateScoreWorker merges them into a unified posture grade.

| Worker | Task | What It Does |
|---|---|---|
| **AssessApplicationWorker** | `sp_assess_application` | Scores application security. counts critical vulnerabilities in production code and dependencies |
| **AssessComplianceWorker** | `sp_assess_compliance` | Scores compliance status against frameworks (SOC2, HIPAA) and flags overdue reviews |
| **AssessInfrastructureWorker** | `sp_assess_infrastructure` | Scores infrastructure security. evaluates firewall rules, encryption, and patching gaps |
| **CalculateScoreWorker** | `sp_calculate_score` | Computes a unified security posture score (letter grade and numeric) from all domain assessments |

the workflow logic stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
