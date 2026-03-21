# Implementing Security Incident Response in Java with Conductor : Triage, Containment, Investigation, and Remediation

## The Problem

You need to respond to security incidents. unauthorized access attempts, data exfiltration, compromised credentials, following a structured playbook: triage the alert to determine severity (P1 through P4), contain the threat by isolating the affected system (e.g., an API gateway), investigate to identify root cause and blast radius, and remediate by patching the vulnerability or revoking compromised credentials. Mean time to containment directly impacts breach severity.

Without orchestration, your incident response is a runbook document that engineers follow manually at 2 AM. Steps get skipped under pressure, containment is delayed while waiting for approvals, and there is no reliable record of what was done or when. If the on-call engineer's laptop crashes mid-investigation, the incident state lives in their browser tabs and terminal history.

## The Solution

**You just write the triage rules and containment actions. Conductor handles the ordered response playbook, retries containment if the first attempt fails, and a complete incident timeline for post-mortem review and regulatory reporting.**

Each phase of the incident response playbook is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so triage output drives containment scope, containment completes before investigation begins, and remediation only runs after root cause is confirmed. If containment fails on the first attempt, Conductor retries automatically. Every action, decision, and timing is recorded, giving you a complete incident timeline for post-mortem review and regulatory reporting.

### What You Write: Workers

Four workers execute the incident response playbook: TriageWorker classifies severity and type, ContainWorker isolates the affected system, InvestigateWorker determines root cause and blast radius, and RemediateWorker applies the fix or credential revocation.

| Worker | Task | What It Does |
|---|---|---|
| **ContainWorker** | `si_contain` | Contains a security incident by isolating the affected system. |
| **InvestigateWorker** | `si_investigate` | Investigates a security incident to determine root cause. |
| **RemediateWorker** | `si_remediate` | Remediates a security incident by applying fixes. |
| **TriageWorker** | `si_triage` | Triages a security incident based on type and severity. |

the workflow logic stays the same.

### The Workflow

```
Input -> ContainWorker -> InvestigateWorker -> RemediateWorker -> TriageWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
