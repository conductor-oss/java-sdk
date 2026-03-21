# Prior Authorization in Java Using Conductor : Request Submission, Clinical Criteria Review, Three-Way Decision Routing, and Provider Notification

## The Problem

You need to process prior authorization requests for medical procedures, imaging studies, or specialty medications. A provider submits a request with the patient ID, procedure code, and clinical justification. The request must be reviewed against the payer's medical necessity criteria. InterQual, MCG, or custom clinical guidelines. Based on the criteria review, the request is routed to one of three paths: auto-approve if all criteria are met, auto-deny if the procedure clearly does not meet medical necessity, or escalate to a medical director for manual peer review if the case is ambiguous. Regardless of the determination, the provider must be notified with the decision, authorization number (if approved), or denial reason with appeal instructions. State prompt-decision laws require turnaround within specific timeframes.

Without orchestration, you'd build a monolithic prior auth engine that receives the 278 request, runs the criteria check, branches with if/else into approve/deny/review paths, and sends the notification. If the criteria engine is slow, the entire determination is delayed. If the system crashes after the criteria review but before the decision is recorded, the provider has no determination and the clock is ticking on regulatory timeframes. Payers must maintain a complete audit trail of every authorization decision for regulatory and accreditation reviews.

## The Solution

**You just write the prior auth workers. Request submission, criteria review, approve/deny/review routing, and provider notification. Conductor handles conditional SWITCH routing between approve, deny, and manual review paths, automatic retries, and timestamped records for regulatory compliance.**

Each stage of the prior authorization process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of submitting before reviewing criteria, routing to the correct decision path (approve, deny, or manual review) via SWITCH, always notifying the provider regardless of which path was taken, and maintaining a complete audit trail with regulatory timeframes. ### What You Write: Workers

Six workers cover the prior auth process: SubmitRequestWorker intakes the request, ReviewCriteriaWorker evaluates medical necessity, ApproveWorker and DenyWorker handle clear-cut decisions, ManualReviewWorker escalates ambiguous cases, and NotifyWorker sends the determination to the provider.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitRequestWorker** | `pa_submit_request` | Receives the authorization request with patient, procedure, and clinical justification |
| **ReviewCriteriaWorker** | `pa_review_criteria` | Evaluates the request against medical necessity criteria (InterQual, MCG) and returns a decision: approve, deny, or review |
| **ApproveWorker** | `pa_approve` | Records the approval, generates an authorization number, and sets the validity period |
| **DenyWorker** | `pa_deny` | Records the denial with the specific clinical reason and appeal instructions |
| **ManualReviewWorker** | `pa_manual_review` | Escalates ambiguous cases to a medical director for peer-to-peer clinical review |
| **NotifyWorker** | `pa_notify` | Sends the determination (approval with auth number, or denial with reason) to the requesting provider |

the workflow and compliance logic stay the same.

### The Workflow

```
pa_submit_request
 │
 ▼
pa_review_criteria
 │
 ▼
SWITCH (pa_switch_ref)
 ├── approve: pa_approve
 ├── deny: pa_deny
 ├── review: pa_manual_review
 └── default: pa_manual_review
 │
 ▼
pa_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
