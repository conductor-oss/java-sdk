# Implementing Immutable Audit Logging in Java with Conductor : Event Capture, Context Enrichment, Immutable Storage, and Integrity Verification

## The Problem

You need a tamper-proof audit trail for compliance (SOC2, HIPAA, PCI). Every security-relevant action (login, permission change, data access) must be captured with full context (actor, action, resource, timestamp, source IP), stored in an immutable log that can't be modified after the fact, and periodically verified for integrity (no entries deleted or modified).

Without orchestration, audit logs are application-level log.info() calls that can be modified or deleted. Context enrichment is inconsistent. some logs include the actor, some don't. Immutability is an aspiration rather than a guarantee, and nobody verifies that the log hasn't been tampered with.

## The Solution

**You just write the event capture and immutable storage logic. Conductor handles ordered execution, durable delivery to the immutable store, and a meta-audit trail of every logging operation itself.**

Each audit concern is an independent worker. event capture, context enrichment, immutable storage, and integrity verification. Conductor runs them in sequence: capture the event, enrich with full context, store immutably, then verify integrity. Every audit operation is itself tracked by Conductor, creating a meta-audit trail. ### What You Write: Workers

Four workers form the audit chain: CaptureEventWorker records security-relevant actions, EnrichContextWorker adds who/what/where context, StoreImmutableWorker writes to a tamper-proof log, and VerifyIntegrityWorker validates the hash chain.

| Worker | Task | What It Does |
|---|---|---|
| **CaptureEventWorker** | `al_capture_event` | Captures a security-relevant event with actor, action, and resource details |
| **EnrichContextWorker** | `al_enrich_context` | Enriches the event with IP address, device info, session data, and role context |
| **StoreImmutableWorker** | `al_store_immutable` | Writes the enriched event to an immutable, append-only audit log |
| **VerifyIntegrityWorker** | `al_verify_integrity` | Verifies log integrity by validating the hash chain. confirms no entries were modified or deleted |

the workflow logic stays the same.

### The Workflow

```
al_capture_event
 │
 ▼
al_enrich_context
 │
 ▼
al_store_immutable
 │
 ▼
al_verify_integrity

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
