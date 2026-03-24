# Content Moderation

Orchestrates content moderation through a multi-stage Conductor workflow.

**Input:** `contentId`, `contentType`, `userId`, `contentText` | **Timeout:** 60s

## Pipeline

```
mod_submit_content
    │
mod_auto_check
    │
mod_decision [SWITCH]
  ├─ safe: mod_approve_safe
  ├─ flag: mod_human_review
  └─ block: mod_block_content
    │
mod_finalize
```

## Workers

**ApproveSafeWorker** (`mod_approve_safe`)

Reads `approved`. Outputs `approved`, `approvedAt`.

**AutoCheckWorker** (`mod_auto_check`)

Reads `confidence`. Outputs `confidence`, `flagReasons`, `toxicityScore`.

**BlockContentWorker** (`mod_block_content`)

Reads `blocked`. Outputs `blocked`, `userNotified`, `appealAvailable`, `blockId`.

**FinalizeWorker** (`mod_finalize`)

Reads `finalStatus`, `verdict`. Outputs `finalStatus`, `auditLogId`.

**HumanReviewWorker** (`mod_human_review`)

Reads `reviewerDecision`. Outputs `reviewerDecision`, `reviewerId`, `reviewedAt`, `notes`.

**SubmitContentWorker** (`mod_submit_content`)

Reads `receivedAt`. Outputs `receivedAt`, `queuePosition`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
