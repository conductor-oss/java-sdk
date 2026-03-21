# User-Assigned Human Task in Java Using Conductor : Document Preparation, WAIT Assigned to Specific Reviewer, and Post-Review Finalization

## Review Tasks Need to Be Assigned to a Specific Person

Unlike group assignments, some tasks must go to a specific user, the document's author, a designated reviewer, or a subject-matter expert. The workflow prepares the document, pauses at a WAIT task assigned to the specific user, and after they complete their review, a post-review step finalizes the document. If finalization fails, you need to retry it without re-assigning the review.

## The Solution

**You just write the document-preparation and post-review finalization workers. Conductor handles the user-specific assignment and the durable wait for that reviewer.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

HuaPrepareWorker identifies the designated reviewer from document metadata, and HuaPostReviewWorker applies their feedback. Neither handles the user-specific assignment or the wait for that person's response.

| Worker | Task | What It Does |
|---|---|---|
| **HuaPrepareWorker** | `hua_prepare` | Prepares the document for review. formats it, identifies the assigned reviewer from the document metadata, and signals readiness |
| *WAIT task* | `hua_user_review` | Pauses until the assigned user completes their review via `POST /tasks/{taskId}` with their feedback and decision | Built-in Conductor WAIT. no worker needed |
| **HuaPostReviewWorker** | `hua_post_review` | Finalizes the document after review. applies the reviewer's feedback, updates the document status, and notifies the author |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
hua_prepare
 │
 ▼
assigned_review [WAIT]
 │
 ▼
hua_post_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
