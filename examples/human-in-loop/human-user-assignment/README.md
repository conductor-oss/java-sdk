# Human User Assignment

HUMAN Task with User Assignment — assigns a review task to a specific user

**Input:** `documentId`, `assignedTo`

**Output:** `prepared`, `reviewResult`, `finalized`

## Pipeline

```
hua_prepare
    │
assigned_review [WAIT]
    │
hua_post_review
```

## Workers

**HuaPostReviewWorker** (`hua_post_review`): Worker for hua_post_review — finalizes the document after human review.

Outputs `finalized`.

**HuaPrepareWorker** (`hua_prepare`): Worker for hua_prepare — prepares a document for human review.

Outputs `prepared`.

## Workflow Output

- `prepared`: `${prepare.output.prepared}`
- `reviewResult`: `${assigned_review_ref.output.result}`
- `finalized`: `${post_review.output.finalized}`

## Data Flow

**hua_prepare**: `documentId` = `${workflow.input.documentId}`
**assigned_review** [WAIT]: `assignedTo` = `${workflow.input.assignedTo}`, `documentId` = `${workflow.input.documentId}`
**hua_post_review**: `reviewResult` = `${assigned_review_ref.output.result}`, `documentId` = `${workflow.input.documentId}`

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
