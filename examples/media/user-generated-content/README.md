# User Generated Content

Orchestrates user generated content through a multi-stage Conductor workflow.

**Input:** `submissionId`, `userId`, `contentType`, `contentBody`, `title` | **Timeout:** 60s

## Pipeline

```
ugc_submit
    │
ugc_moderate
    │
ugc_approve
    │
ugc_enrich
    │
ugc_publish
```

## Workers

**ApproveWorker** (`ugc_approve`)

Reads `approvalMethod`. Outputs `approvalMethod`, `approvedAt`.

**EnrichWorker** (`ugc_enrich`)

Reads `metadata`. Outputs `metadata`, `autoTags`, `sentiment`, `readability`, `language`.

**ModerateWorker** (`ugc_moderate`)

Reads `moderationScore`. Outputs `moderationScore`, `flagged`, `categories`, `spam`, `adult`.

**PublishWorker** (`ugc_publish`)

Reads `published`. Outputs `published`, `publishUrl`, `publishedAt`.

**SubmitWorker** (`ugc_submit`)

Reads `receivedAt`. Outputs `receivedAt`, `queuePosition`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
