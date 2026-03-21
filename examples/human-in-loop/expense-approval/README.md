# Expense Approval in Java Using Conductor: Policy Validation, SWITCH for Auto-Approve vs. Manager Approval via WAIT, and Processing

## Expenses Above Policy Thresholds Need Human Approval

An employee submits an expense report, but not all expenses can be auto-approved. The workflow validates the expense against policy rules. If the amount exceeds $100 or the category is "travel", a human manager must approve it via a WAIT task. Otherwise, the expense is processed automatically. The SWITCH task routes between the approval path and the direct-processing path based on the policy check. If processing fails after approval, you need to retry it without asking the manager to re-approve.

## The Solution

**You just write the policy validation and expense processing workers. Conductor handles the routing, approval holds, and retries.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

ValidatePolicyWorker checks expense amounts and categories, while ProcessWorker handles reimbursement. Neither knows about the SWITCH or WAIT tasks that connect them.

| Worker | Task | What It Does |
|---|---|---|
| **ValidatePolicyWorker** | `exp_validate_policy` | Checks the expense amount and category against policy rules. amounts over $100 or "travel" category require manager approval; returns `approvalRequired: "true"` or `"false"` as a string for the SWITCH evaluator |
| *SWITCH task* | `approval_switch` | Routes based on `approvalRequired`. `"true"` sends to a WAIT task for manager approval, default skips straight to processing | Built-in Conductor SWITCH.; no worker needed |
| *WAIT task* | `wait_for_approval` | Pauses the workflow until a manager approves or rejects the expense via `POST /tasks/{taskId}`. See [Completing the WAIT Task](#completing-the-wait-task-human-approval) | Built-in Conductor WAIT.; no worker needed |
| **ProcessWorker** | `exp_process` | Finalizes the approved expense: posts the reimbursement to payroll, updates the general ledger, and sends the employee a confirmation email; returns `processed: true` |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
exp_validate_policy
 │
 ▼
SWITCH (approval_switch)
 ├── "true": wait_for_approval (WAIT)
 │
 ▼
exp_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
