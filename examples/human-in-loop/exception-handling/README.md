# Exception-Based Routing in Java Using Conductor : Risk Analysis, SWITCH for Auto-Process vs. Human Review, and Finalization

## The Problem

Not every item needs human attention. most can be auto-processed, but high-risk exceptions must be escalated to a person. The workflow analyzes the item's risk score (1-10) and routes accordingly: items scoring 7 or below go to automatic processing, while items above 7 pause at a WAIT task for human review. The human reviewer examines the flagged item and makes a decision. After either path completes (auto-processed or human-reviewed), a finalization step wraps up. Without this routing, you either manually review everything (expensive and slow) or auto-process everything (dangerous for high-risk items).

## The Solution

**You just write the risk-analysis, auto-processing, and finalization workers. Conductor handles the score-based routing and the human review hold for exceptions.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

AnalyzeWorker scores risk on a 1-10 scale, AutoProcessWorker handles low-risk items, and FinalizeWorker wraps up, the SWITCH routing between auto-processing and human review is declarative.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `eh_analyze` | Evaluates the item's risk score. scores above 7 return route="human_review" for the SWITCH, scores 7 or below return route="auto_process" for automatic handling |
| *SWITCH task* | `route_switch` | Routes based on the analyze worker's output. "human_review" sends to a WAIT task for human examination, default sends to auto-processing | Built-in Conductor SWITCH, no worker needed |
| **AutoProcessWorker** | `eh_auto_process` | Automatically processes low-risk items. executes the standard business logic without human intervention |
| *WAIT task* | `human_review_wait` | Pauses for a human reviewer to examine the high-risk item and submit their decision via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **FinalizeWorker** | `eh_finalize` | Finalizes the workflow after either auto-processing or human review, recording the outcome and the path taken |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
eh_analyze
 │
 ▼
SWITCH (route_switch)
 ├── human_review: human_review_wait
 └── default: eh_auto_process
 │
 ▼
eh_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
