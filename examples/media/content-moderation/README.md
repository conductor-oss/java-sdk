# Content Moderation Pipeline in Java Using Conductor : Auto-Check, Toxicity Scoring, Human Review, and Policy Enforcement

## Why Content Moderation Needs Orchestration

Moderating user content requires a decision pipeline with multiple possible outcomes. You receive a submission and queue it. You run automated checks. toxicity scoring, policy violation detection, confidence assessment. Based on the auto-check results, you route to three different paths: safe content (high confidence, low toxicity) is approved automatically; flagged content (medium confidence, potential violations) goes to a human moderator for manual review; clearly violating content (high toxicity, obvious violations) is blocked immediately with user notification and appeal options.

This is exactly the kind of conditional routing that becomes unmanageable in a monolithic system. Each moderation decision involves different downstream actions. approval publishes the content, human review assigns a moderator and waits for their verdict, blocking notifies the user and records a block ID. Without orchestration, you'd build a monolithic moderation engine with nested if/else chains handling every combination of toxicity scores and confidence levels, mixing ML inference, queue management, human task assignment, and notification logic in one class.

## How This Workflow Solves It

**You just write the moderation workers. Content submission, auto-check scoring, human review, blocking, and finalization. Conductor handles three-way SWITCH routing, ML service retries, and a tamper-evident audit log for every moderation decision.**

Each moderation concern is an independent worker. submit content, auto-check, approve, human review, block, finalize. Conductor sequences the initial submission and auto-check, then uses a SWITCH task to route to the correct action based on the moderation verdict. Every decision is recorded in the audit log, and adding a new moderation outcome (e.g., "restrict visibility") means adding a new SWITCH case and worker.

### What You Write: Workers

Six workers cover the moderation lifecycle: SubmitContentWorker queues submissions, AutoCheckWorker scores toxicity, ApproveSafeWorker passes clean content, HumanReviewWorker escalates borderline cases, BlockContentWorker rejects violations, and FinalizeWorker records the audit log.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveSafeWorker** | `mod_approve_safe` | Approves the safe |
| **AutoCheckWorker** | `mod_auto_check` | Handles auto check |
| **BlockContentWorker** | `mod_block_content` | Handles block content |
| **FinalizeWorker** | `mod_finalize` | Finalizes the moderation decision by recording the verdict as the final status and generating an audit log entry |
| **HumanReviewWorker** | `mod_human_review` | Handles human review |
| **SubmitContentWorker** | `mod_submit_content` | Handles submit content |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
mod_submit_content
 │
 ▼
mod_auto_check
 │
 ▼
SWITCH (mod_switch_ref)
 ├── safe: mod_approve_safe
 ├── flag: mod_human_review
 ├── block: mod_block_content
 │
 ▼
mod_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
