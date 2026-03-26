# ITIL Change Management with Risk-Based Approval

A database migration needs to go to production. Someone files the change request, but then
it sits in a shared inbox. Nobody assesses the risk, the CAB never sees it, and the change
gets applied without approval. This workflow enforces the full ITIL lifecycle: submit, assess
risk, route through CAB approval, and implement -- with risk scores driving the approval
path.

## Workflow

```
changeType, description, system
              |
              v
  +-------------+     +-----------------+     +-------------+     +----------------+
  | cm_submit   | --> | cm_assess_risk  | --> | cm_approve  | --> | cm_implement   |
  +-------------+     +-----------------+     +-------------+     +----------------+
    SUBMIT-1351         riskLevel by type     CAB decision        implemented=true
    success=true        riskScore computed     note varies         rollbackPlan set
```

## Workers

**SubmitChange** -- Takes `changeType` (default `"standard"`), `description`, and `system`.
Returns `submitId: "SUBMIT-1351"` with the inputs echoed back.

**AssessRisk** -- Reads `changeType` from the submit output. `"emergency"` yields `riskLevel:
"high"`, `riskScore: 85`, `requiresApproval: true`. `"normal"` yields `"medium"` / 50 / true.
Default `"standard"` yields `"low"` / 25 / false.

**ApproveChange** -- Reads `riskLevel`. High-risk changes are `"Approved with additional
monitoring required"`. Medium-risk get `"Approved by CAB with standard review"`. Low-risk
are `"Auto-approved for low-risk change"`. Approver is always `"cab-board"`.

**ImplementChange** -- Checks the `approved` flag from the approval output. Stamps
`completedAt: "2026-03-14T10:00:00Z"` and provides `rollbackPlan: "Restore previous RDS
snapshot"`.

## Tests

33 unit tests cover submission, risk scoring for all change types, approval routing, and
implementation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
