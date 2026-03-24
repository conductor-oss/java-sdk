# Patent Filing

Orchestrates patent filing through a multi-stage Conductor workflow.

**Input:** `inventionTitle`, `inventors`, `description` | **Timeout:** 60s

## Pipeline

```
ptf_draft
    │
ptf_review
    │
ptf_prior_art
    │
ptf_file
    │
ptf_track
```

## Workers

**DraftWorker** (`ptf_draft`)

Reads `inventionTitle`. Outputs `draftId`, `claims`.

**FileWorker** (`ptf_file`)

Reads `draftId`. Outputs `applicationNumber`, `filingDate`.

**PriorArtWorker** (`ptf_prior_art`)

Reads `inventionTitle`. Outputs `priorArtClear`, `matchesFound`, `conflictRisk`.

**ReviewWorker** (`ptf_review`)

Reads `draftId`. Outputs `reviewStatus`, `comments`.

**TrackWorker** (`ptf_track`)

Reads `applicationNumber`. Outputs `trackingId`, `status`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
