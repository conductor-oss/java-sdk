# Medical Records Review in Java Using Conductor : HIPAA Compliance Validation, Physician Review via WAIT, and Audit-Trailed Storage

## Medical Records Must Pass HIPAA Compliance Before Physician Review

Before a physician can review medical records, the system must validate HIPAA compliance. Checking PHI encryption, audit logging, access controls, and data retention policies. The workflow runs automated HIPAA checks, pauses at a WAIT task for physician review, then stores the result with an audit trail. If storing the result fails, you need to retry it without asking the physician to re-review.

## The Solution

**You just write the HIPAA validation and audit-trailed storage workers. Conductor handles the durable pause for physician review and the compliance pipeline.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

MrValidateHipaaWorker checks PHI encryption and access controls, and MrStoreResultWorker records the physician's assessment with an audit trail, the clinical review pause between them is healthcare-pattern and durable.

| Worker | Task | What It Does |
|---|---|---|
| **MrValidateHipaaWorker** | `mr_validate_hipaa` | Runs automated HIPAA compliance checks. verifies PHI encryption, audit logging configuration, access controls, and data retention policies |
| *WAIT task* | `mr_physician_review` | Pauses until a physician reviews the records and submits their clinical assessment via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **MrStoreResultWorker** | `mr_store_result` | Stores the physician review result with a complete healthcare-pattern audit trail including reviewer identity, timestamp, and access log |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
mr_validate_hipaa
 │
 ▼
physician_review [WAIT]
 │
 ▼
mr_store_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
