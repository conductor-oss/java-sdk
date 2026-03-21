# Event Audit Trail in Java Using Conductor

Sequential event audit trail workflow: log_received -> validate_event -> log_validated -> process_event -> log_processed -> finalize_audit. ## The Problem

You need to maintain a complete audit trail for every event that passes through your system. Each event must be logged on receipt, validated against business rules, logged again after validation, processed, logged after processing, and have its audit record finalized. Regulatory compliance (SOX, HIPAA, GDPR) often demands proof that every event was received, validated, and processed with timestamps at each stage.

Without orchestration, you'd sprinkle logging calls throughout your event processor, hoping none are skipped by early returns or exceptions. manually correlating log entries across services, ensuring audit records are never lost even when processing fails, and building custom reporting to satisfy auditors.

## The Solution

**You just write the audit-logging, validation, event-processing, and audit-finalization workers. Conductor handles guaranteed step completion for regulatory compliance, crash-safe audit continuity, and a built-in execution history that doubles as the audit trail.**

Each audit concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the six-step audit chain (log received, validate, log validated, process, log processed, finalize), guaranteeing that every step is recorded even if a later step fails, and providing a complete execution history that serves as the audit trail itself. ### What You Write: Workers

Six workers build a compliance-grade audit chain: LogReceivedWorker, ValidateEventWorker, LogValidatedWorker, ProcessEventWorker, LogProcessedWorker, and FinalizeAuditWorker each stamp a verifiable timestamp at every stage of the event lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **FinalizeAuditWorker** | `at_finalize_audit` | Finalizes the audit trail. |
| **LogProcessedWorker** | `at_log_processed` | Logs that an event has been processed. |
| **LogReceivedWorker** | `at_log_received` | Logs that an event has been received. |
| **LogValidatedWorker** | `at_log_validated` | Logs that an event has been validated. |
| **ProcessEventWorker** | `at_process_event` | Processes an event. |
| **ValidateEventWorker** | `at_validate_event` | Validates an incoming event. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
at_log_received
 │
 ▼
at_validate_event
 │
 ▼
at_log_validated
 │
 ▼
at_process_event
 │
 ▼
at_log_processed
 │
 ▼
at_finalize_audit

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
