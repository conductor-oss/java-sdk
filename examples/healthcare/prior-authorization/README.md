# Prior Authorization

Prior Authorization: submit, review criteria, SWITCH decision, notify

**Input:** `authId`, `patientId`, `procedure`, `clinicalReason` | **Timeout:** 60s

## Pipeline

```
pa_submit_request
    │
pa_review_criteria
    │
pa_route_decision [SWITCH]
  ├─ approve: pa_approve
  ├─ deny: pa_deny
  ├─ review: pa_manual_review
  └─ default: pa_manual_review
    │
pa_notify
```

## Workers

**ApproveWorker** (`pa_approve`)

Reads `authId`, `validDays`. Outputs `approved`, `authNumber`, `validDays`.

**DenyWorker** (`pa_deny`)

Reads `authId`, `denyReason`. Outputs `denied`, `appealDeadline`.

**ManualReviewWorker** (`pa_manual_review`)

Reads `authId`, `reviewNotes`. Outputs `pendingReview`, `assignedTo`.

**NotifyWorker** (`pa_notify`)

Reads `authId`, `decision`. Outputs `notified`, `channels`, `notifiedAt`.

**ReviewCriteriaWorker** (`pa_review_criteria`)

Reads `clinicalReason`, `procedure`. Outputs `decision`, `validDays`, `denyReason`, `reviewNotes`.

**SubmitRequestWorker** (`pa_submit_request`)

Reads `authId`, `procedure`. Outputs `submitted`, `submittedAt`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
