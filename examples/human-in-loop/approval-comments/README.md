# Approval with Comments and Attachments in Java Using Conductor : Document Preparation, Human Review via WAIT, and Feedback Application

Human-in-the-loop approval with rich feedback. prepares a document for review, pauses the workflow with a WAIT task until a human reviewer submits their decision along with comments, attachments, ratings, and tags, then applies that feedback to the document.

## The Problem

You need approvals that go beyond a simple approve/reject button. A reviewer needs to provide structured feedback. a decision (approve, reject, revise), free-text comments explaining their reasoning, file attachments (annotated screenshots, supporting documents, redlined versions), a quality rating, and classification tags. The workflow must pause and wait for this human input, then pass every piece of that feedback to downstream processing. Without a WAIT mechanism, you'd poll a database or queue for the reviewer's response, building your own timeout and notification logic.

Without orchestration, you'd build a custom review portal backed by a polling loop. the system prepares the document, writes a review request to a database, and then polls every few seconds for a response. When the reviewer finally submits their decision, comments, and attachments, you'd manually update the workflow state. If the process crashes while waiting for the review, the request is lost. If the reviewer attaches files, you'd need to pass attachment metadata through your own state management. There is no built-in timeout, no audit trail of when the review was requested versus completed, and no visibility into how long reviews are taking.

## The Solution

**You just write the document-preparation and feedback-application workers. Conductor handles the durable pause for rich reviewer input.**

The WAIT task is the key pattern here. After preparing the document, the workflow pauses at the WAIT task. Conductor holds the workflow state durably until an external signal (REST API call or SDK call) completes the task with the reviewer's decision, comments, attachments, rating, and tags. The apply-feedback worker then receives all of that structured input. Conductor takes care of holding the workflow state for hours or days while awaiting review, accepting the reviewer's rich feedback (decision, comments, attachments, rating, tags) via API, passing every field from the WAIT output into the feedback worker, and providing a complete timeline showing when the document was prepared, when the review was completed, and how long it took.

### What You Write: Workers

PrepareDocWorker formats the document for review, and ApplyFeedbackWorker processes the reviewer's comments, attachments, and ratings. Neither manages the durable pause between them.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareDocWorker** | `ac_prepare_doc` | Prepares the document for review. formats it, generates a preview, and signals readiness so the WAIT task can accept reviewer input |
| *WAIT task* | `review_with_comments` | Pauses the workflow until a reviewer submits their decision, comments, attachments, rating, and tags via the Conductor REST API | Built-in Conductor WAIT. no worker needed |
| **ApplyFeedbackWorker** | `ac_apply_feedback` | Applies the reviewer's structured feedback. updates the document status based on the decision, stores comments, processes attachments, and records the rating and tags |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
ac_prepare_doc
 │
 ▼
review_with_comments [WAIT]
 │
 ▼
ac_apply_feedback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
