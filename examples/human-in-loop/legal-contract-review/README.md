# Legal Contract Review in Java Using Conductor: AI Term Extraction, Human Legal Review via WAIT, and Finalization

## Contracts Need AI-Assisted Extraction Followed by Human Legal Review

Legal contracts contain key terms (parties, payment terms, liability caps, termination clauses) and risk flags (unlimited liability, auto-renewal, broad IP assignment) that AI can extract, but a human lawyer must verify before the contract is signed. The workflow extracts terms via AI, pauses for legal review via a WAIT task, then finalizes the review. If finalization fails after the lawyer approves, you need to retry it without asking the lawyer to re-review.

## The Solution

**You just write the term-extraction and review-finalization workers. Conductor handles the durable pause for lawyer review and the retry logic.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

LcrExtractTermsWorker pulls key clauses and risk flags from contracts, and LcrFinalizeWorker records the lawyer's redline notes, the WAIT task between them holds state for days if needed.

| Worker | Task | What It Does |
|---|---|---|
| **LcrExtractTermsWorker** | `lcr_extract_terms` | Uses AI to extract key contract terms (parties, payment terms, liability caps, IP clauses, termination conditions) and flag risk areas (unlimited liability, auto-renewal) |
| *WAIT task* | `lcr_legal_review` | Pauses with the extracted terms and risk flags until a lawyer reviews, verifies accuracy, and submits their assessment via `POST /tasks/{taskId}` | Built-in Conductor WAIT.; no worker needed |
| **LcrFinalizeWorker** | `lcr_finalize` | Finalizes the contract review. records the lawyer's approval and any redline notes, updates the contract status in the CLM system |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
lcr_extract_terms
 │
 ▼
legal_review [WAIT]
 │
 ▼
lcr_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
