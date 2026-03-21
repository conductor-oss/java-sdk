# Wire Tap Pattern in Java Using Conductor : Process Messages While Tapping an Audit Copy in Parallel

## Auditing Must Not Slow Down the Main Flow

Every payment transaction must be processed and also logged to the compliance audit trail. If you audit synchronously before processing, you add latency to every transaction. If you audit after processing, a crash between process and audit means a transaction exists without an audit record. The wire tap pattern solves this by running both in parallel. the main flow and the audit tap execute simultaneously, so neither blocks the other.

Building this manually means spawning two threads per message, ensuring the audit log captures the exact same data the main flow received, handling the case where the audit write fails but the main flow succeeds, and joining both results before moving on.

## The Solution

**You write the business logic and audit tap. Conductor handles parallel execution, independent retries, and completion tracking.**

`WtpReceiveWorker` ingests the message. A `FORK_JOIN` immediately splits into two parallel paths: `WtpMainFlowWorker` processes the message through the normal business logic, while `WtpTapAuditWorker` writes a copy to the audit log based on the configured audit level. Both run simultaneously. the audit tap doesn't add latency to the main flow. The `JOIN` waits for both to complete. Conductor ensures both paths see the exact same input, retries either independently if one fails, and records whether both the main flow and audit tap succeeded.

### What You Write: Workers

Three workers split between the main flow and the audit tap: message reception, business logic processing, and parallel audit logging, neither path blocking the other.### The Workflow

```
wtp_receive
 │
 ▼
FORK_JOIN
 ├── wtp_main_flow
 └── wtp_tap_audit
 │
 ▼
JOIN (wait for all branches)

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
