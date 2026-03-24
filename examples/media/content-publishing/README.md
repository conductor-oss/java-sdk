# Content Publishing

Publishing pipeline: draft content, review, approve, format for distribution, publish, and distribute to channels.

**Input:** `contentId`, `authorId`, `contentType`, `title` | **Timeout:** 60s

## Pipeline

```
pub_draft_content
    │
pub_review_content
    │
pub_approve_content
    │
pub_format_content
    │
pub_publish_content
    │
pub_distribute_content
```

## Workers

**ApproveContentWorker** (`pub_approve_content`): Approves content based on review score.

Reads `reviewScore`. Outputs `approved`, `approver`, `approvedAt`.

**DistributeContentWorker** (`pub_distribute_content`): Distributes published content to channels.

Reads `title`. Outputs `channels`, `scheduledPosts`, `distributedAt`.

**DraftContentWorker** (`pub_draft_content`): Creates a draft of the content.

Reads `authorId`, `title`. Outputs `draftVersion`, `wordCount`, `readTime`, `slug`.

**FormatContentWorker** (`pub_format_content`): Formats content for distribution.

Reads `contentType`. Outputs `formattedUrl`, `seoMetadata`, `formats`.

**PublishContentWorker** (`pub_publish_content`): Publishes content to production.

Outputs `publishUrl`, `publishedAt`, `cacheInvalidated`.

**ReviewContentWorker** (`pub_review_content`): Reviews draft content and assigns a score.

Reads `draftVersion`, `wordCount`. Outputs `reviewScore`, `reviewer`, `reviewNotes`.

## Tests

**48 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
