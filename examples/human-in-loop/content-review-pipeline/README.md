# Content Review Pipeline

Content Review Pipeline -- AI generates draft, human reviews/edits, then publish.

**Input:** `topic`, `audience` | **Timeout:** 300s

**Output:** `content`, `wordCount`, `model`, `published`, `url`

## Pipeline

```
crp_ai_draft
    │
crp_human_review [WAIT]
    │
crp_publish
```

## Workers

**CrpAiDraftWorker** (`crp_ai_draft`): Worker for crp_ai_draft task -- generates an AI content draft.

- topic:    the content topic
- audience: the target audience
- content:   generated draft content string incorporating topic and audience
- wordCount: 42 (deterministic.word count)

Reads `audience`, `topic`. Outputs `content`, `wordCount`, `model`.

**CrpPublishWorker** (`crp_publish`): Worker for crp_publish task -- publishes content if approved.

- approved: boolean indicating whether the content was approved
- content:  the content to publish
- published: true if approved and published, false otherwise
- url:       the published URL (only present when published=true)

```java
boolean approved = Boolean.TRUE.equals(approvedObj);
```

Reads `approved`. Outputs `published`, `url`.

## Workflow Output

- `content`: `${crp_ai_draft_ref.output.content}`
- `wordCount`: `${crp_ai_draft_ref.output.wordCount}`
- `model`: `${crp_ai_draft_ref.output.model}`
- `published`: `${crp_publish_ref.output.published}`
- `url`: `${crp_publish_ref.output.url}`

## Data Flow

**crp_ai_draft**: `topic` = `${workflow.input.topic}`, `audience` = `${workflow.input.audience}`
**crp_human_review** [WAIT]: `content` = `${crp_ai_draft_ref.output.content}`, `wordCount` = `${crp_ai_draft_ref.output.wordCount}`, `model` = `${crp_ai_draft_ref.output.model}`
**crp_publish**: `approved` = `${crp_human_review_ref.output.approved}`, `content` = `${crp_human_review_ref.output.editedContent}`

## Tests

**16 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
