# Approval Comments

Approval with Comments and Attachments -- prepares a document, waits for human review with comments/attachments, then applies feedback.

**Input:** `documentId` | **Timeout:** 3600s

**Output:** `decision`, `comments`, `attachments`, `attachmentCount`, `rating`, `tags`

## Pipeline

```
ac_prepare_doc
    │
review_with_comments [WAIT]
    │
ac_apply_feedback
```

## Workers

**ApplyFeedbackWorker** (`ac_apply_feedback`): Worker for ac_apply_feedback -- applies the reviewer's feedback.

Reads `attachments`, `comments`, `decision`, `rating`, `tags`. Outputs `applied`.

**PrepareDocWorker** (`ac_prepare_doc`): Worker for ac_prepare_doc -- prepares a document for review.

Outputs `ready`.

## Workflow Output

- `decision`: `${review_with_comments_ref.output.decision}`
- `comments`: `${review_with_comments_ref.output.comments}`
- `attachments`: `${review_with_comments_ref.output.attachments}`
- `attachmentCount`: `${review_with_comments_ref.output.attachmentCount}`
- `rating`: `${review_with_comments_ref.output.rating}`
- `tags`: `${review_with_comments_ref.output.tags}`

## Data Flow

**ac_prepare_doc**: `documentId` = `${workflow.input.documentId}`
**review_with_comments** [WAIT]: `documentId` = `${workflow.input.documentId}`, `ready` = `${prepare_doc_ref.output.ready}`
**ac_apply_feedback**: `decision` = `${review_with_comments_ref.output.decision}`, `comments` = `${review_with_comments_ref.output.comments}`, `attachments` = `${review_with_comments_ref.output.attachments}`, `attachmentCount` = `${review_with_comments_ref.output.attachmentCount}`, `rating` = `${review_with_comments_ref.output.rating}`, `tags` = `${review_with_comments_ref.output.tags}`

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
