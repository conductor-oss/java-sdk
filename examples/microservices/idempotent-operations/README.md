# Idempotent Operations in Java with Conductor

Idempotent operations with duplicate detection. ## The Problem

In a distributed system, duplicate requests are inevitable (network retries, user double-clicks). Each operation must be idempotent. Executing it twice must produce the same result. This workflow generates a deterministic idempotency key, checks whether the operation was already executed, and either skips execution (duplicate) or executes and records the completion.

Without orchestration, idempotency checks are sprinkled throughout business logic with ad-hoc Redis or database lookups. Missing a check on one endpoint leads to duplicate charges, double inventory deductions, or repeated notifications.

## The Solution

**You just write the key-generation, duplicate-check, execution, and completion-recording workers. Conductor handles conditional skip-or-execute routing, guaranteed completion recording, and full visibility into duplicate detection.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers enforce exactly-once semantics: GenerateKeyWorker computes a deterministic idempotency key, CheckDuplicateWorker queries the dedup store, ExecuteWorker performs the operation if not a duplicate, and RecordCompletionWorker persists the result.

| Worker | Task | What It Does |
|---|---|---|
| **CheckDuplicateWorker** | `io_check_duplicate` | Checks the idempotency store to determine whether this key was already processed. |
| **ExecuteWorker** | `io_execute` | Executes the operation (only if not a duplicate) and returns the result. |
| **GenerateKeyWorker** | `io_generate_key` | Generates a deterministic idempotency key from the operation ID and action name. |
| **RecordCompletionWorker** | `io_record_completion` | Records the idempotency key and result in the store to prevent future duplicates. |

the workflow coordination stays the same.

### The Workflow

```
io_generate_key
 │
 ▼
io_check_duplicate
 │
 ▼
SWITCH (decision_ref)
 ├── true: 
 └── default: io_execute -> io_record_completion

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
