# Public Records in Java with Conductor

Fulfills a public records request (FOIA): receiving the request, searching government databases, verifying document authenticity, redacting sensitive information, and releasing records to the requester.

## The Problem

You need to fulfill a public records request (FOIA, state open records law). A requester submits a request for specific records, the records are searched across government databases and archives, found records are verified for authenticity, sensitive information is redacted (SSNs, law enforcement details, attorney-client privilege), and the redacted records are released to the requester. Releasing un-redacted records exposes private information and violates privacy laws; denying without proper search violates open records laws.

Without orchestration, you'd manage records requests through email and shared drives. manually searching across multiple databases, tracking which documents need redaction, coordinating with legal counsel on exemptions, and meeting statutory response deadlines that vary by jurisdiction.

## The Solution

**You just write the request intake, database search, document verification, redaction, and record release logic. Conductor handles search retries, redaction sequencing, and records request audit trails.**

Each records request concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (request, search, verify, redact, release), tracking every request with timestamps for statutory deadline compliance, and resuming from the last step if the process crashes.

### What You Write: Workers

Request validation, records search, redaction, and delivery workers handle FOIA and public records requests through auditable stages.

| Worker | Task | What It Does |
|---|---|---|
| **RedactWorker** | `pbr_redact` | Redacts PII and sensitive information from the found documents before release |
| **ReleaseWorker** | `pbr_release` | Releases the redacted public records documents to the requester |
| **RequestWorker** | `pbr_request` | Receives the FOIA request from the requester and assigns a tracking ID |
| **SearchWorker** | `pbr_search` | Searches government databases and archives for documents matching the request query |
| **VerifyWorker** | `pbr_verify` | Verifies document authenticity and checks for applicable exemptions before release |

### The Workflow

```
pbr_request
 │
 ▼
pbr_search
 │
 ▼
pbr_verify
 │
 ▼
pbr_redact
 │
 ▼
pbr_release

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
