# Content Review Pipeline in Java Using Conductor : AI Draft Generation, Human Review via WAIT, and Publishing

## The Problem

You need a content pipeline where AI generates first drafts and humans review them before publishing. An AI model produces content for a given topic and audience. a blog post, marketing copy, product description, or documentation page. The draft includes the generated text, word count, and model used. A human editor must review the AI output for accuracy, tone, brand voice, and factual correctness. The editor may approve it as-is, edit it and approve the revised version, or reject it entirely. Only approved content should be published. Without a review step, AI hallucinations, off-brand tone, or factual errors reach your audience.

Without orchestration, you'd call the AI API, store the draft in a database, email the editor a link to review it, poll for their response, and then call the CMS publish API. If the AI API times out, you'd need retry logic. If the system crashes after the editor approves but before publishing, the reviewed content never goes live. There is no single view showing which drafts are awaiting review, how long reviews take, or the approval rate of AI-generated content.

## The Solution

**You just write the AI draft-generation and publishing workers. Conductor handles the durable pause for editorial review and the content lifecycle tracking.**

The WAIT task is the key pattern here. After the AI generates the draft, the workflow pauses at the WAIT task. Conductor holds the draft content, word count, and model metadata until a human editor completes the review with an approved/rejected decision and optionally edited content. The publish worker only fires after the review is complete. Conductor takes care of holding the draft durably while the editor reviews (minutes, hours, or days), passing the editor's approved flag and edited content to the publish worker, tracking the complete content lifecycle from AI generation through review to publication, and providing metrics on review turnaround time and approval rates. ### What You Write: Workers

CrpAiDraftWorker generates content for a topic and audience, and CrpPublishWorker posts approved copy to the CMS, the editorial review pause between them is handled entirely by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **CrpAiDraftWorker** | `crp_ai_draft` | Generates an AI content draft for the given topic and target audience, returning the draft text, word count, and model used |
| *WAIT task* | `crp_human_review` | Pauses the workflow with the AI draft content until a human editor submits their review. approved/rejected flag and optionally edited content, via `POST /tasks/{taskId}` | Built-in Conductor WAIT, no worker needed |
| **CrpPublishWorker** | `crp_publish` | Publishes the approved content to the CMS, returning the published URL; skips publishing if the review was rejected |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
crp_ai_draft
 │
 ▼
crp_human_review [WAIT]
 │
 ▼
crp_publish

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
