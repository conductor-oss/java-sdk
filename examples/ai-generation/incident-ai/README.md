# Incident AI in Java with Conductor : Detect, Diagnose, Fix, and Verify Production Incidents

## Responding to Incidents Faster Than Humans Can

When a production alert fires at 3 AM, the response time depends on how fast an engineer can wake up, read the alert, diagnose the issue, apply a fix, and verify recovery. Automating this pipeline reduces mean time to resolution (MTTR) from hours to minutes. Each step in the response chain builds on the previous one. diagnosis requires the detection context, the suggested fix depends on the diagnosis, execution depends on the fix plan, and verification confirms the fix actually worked.

This workflow models the incident response lifecycle. The detector analyzes the incoming alert. The diagnoser identifies the root cause from the detection output. The fix suggester proposes a remediation based on the diagnosis. The executor applies the fix. The verifier checks that the service has recovered. The five-step chain ensures every incident is handled systematically, with full traceability from alert to resolution.

## The Solution

**You just write the detection, diagnosis, fix-suggestion, execution, and verification workers. Conductor handles the five-step incident response chain.**

Five workers handle the incident lifecycle. detection, diagnosis, fix suggestion, fix execution, and verification. The detector correlates alert data. The diagnoser identifies root causes. The fix suggester proposes remediations. The executor applies the chosen fix. The verifier confirms service recovery. Conductor sequences the five steps and passes alert context, diagnoses, fix plans, and execution results between them automatically.

### What You Write: Workers

DetectWorker correlates alert data, DiagnoseWorker identifies root causes, SuggestFixWorker proposes remediation, ExecuteFixWorker applies it, and VerifyWorker confirms service recovery. Five steps from alert to resolution.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `iai_detect` | Correlates the incoming alert with service health data and determines the anomaly type. |
| **DiagnoseWorker** | `iai_diagnose` | Identifies the root cause of the detected anomaly (e.g., memory leak, traffic spike, dependency failure). |
| **ExecuteFixWorker** | `iai_execute_fix` | Applies the suggested remediation (restart, scale up, roll back, etc.). |
| **SuggestFixWorker** | `iai_suggest_fix` | Proposes a remediation action based on the root cause diagnosis. |
| **VerifyWorker** | `iai_verify` | Checks that the service has recovered after the fix by running health checks. |

### The Workflow

```
iai_detect
 │
 ▼
iai_diagnose
 │
 ▼
iai_suggest_fix
 │
 ▼
iai_execute_fix
 │
 ▼
iai_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
