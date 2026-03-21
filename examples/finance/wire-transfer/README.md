# Wire Transfer in Java with Conductor

Wire transfer workflow: validate, verify sender, compliance check, execute, and confirm. ## The Problem

You need to execute a wire transfer between bank accounts. The workflow validates the transfer details (amount, currency, account numbers), verifies the sender's identity and account balance, runs compliance checks (sanctions screening, transaction monitoring), executes the wire through the payment network, and confirms completion. Executing a wire without compliance checks exposes the bank to regulatory penalties; insufficient balance checks result in overdrafts.

Without orchestration, you'd build a single wire service that validates, authenticates, screens, transmits via SWIFT/Fedwire, and confirms. manually handling the two-phase nature of wire execution (reserve funds, then release), retrying failed network transmissions, and maintaining an audit trail for BSA/AML compliance.

## The Solution

**You just write the wire transfer workers. Detail validation, sender verification, sanctions screening, network execution, and confirmation. Conductor handles sequential execution, automatic retries when the payment network is temporarily unavailable, and a complete audit trail for BSA/AML compliance.**

Each wire transfer concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (validate, verify sender, compliance check, execute, confirm), retrying if the payment network is temporarily unavailable, tracking every wire with complete audit trail for regulatory compliance, and resuming from the last step if the process crashes. ### What You Write: Workers

Five workers handle the wire lifecycle: ValidateWorker checks transfer details and routing numbers, VerifySenderWorker authenticates the sender and confirms funds, ComplianceCheckWorker screens against sanctions lists, ExecuteWorker transmits via the payment network, and ConfirmWorker records the settlement.

| Worker | Task | What It Does |
|---|---|---|
| **ComplianceCheckWorker** | `wir_compliance_check` | Compliance Check. Computes and returns cleared, ctr required, sanctions cleared, risk score |
| **ConfirmWorker** | `wir_confirm` | Confirms the operation and computes confirmed, confirmation number, notified parties |
| **ExecuteWorker** | `wir_execute` | Execute. Computes and returns transaction ref, network, settled at, fee |
| **ValidateWorker** | `wir_validate` | Validates the input data and computes valid, routing valid, recipient bank verified, swift code |
| **VerifySenderWorker** | `wir_verify_sender` | Verifying account |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
wir_validate
 │
 ▼
wir_verify_sender
 │
 ▼
wir_compliance_check
 │
 ▼
wir_execute
 │
 ▼
wir_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
