# Travel Policy in Java with Conductor

Travel policy with SWITCH for compliant/exception. ## The Problem

You need to enforce travel policy on booking requests. Checking whether a booking (flight class, hotel rate, rental vehicle class) complies with company policy, routing compliant bookings for automatic approval, routing non-compliant bookings through an exception approval process, and then processing the booking once approved. The routing decision depends on the policy check result.

Without orchestration, you'd hardcode policy rules in the booking handler, mixing compliance checks with approval logic and booking processing. Updating the policy (e.g., allowing business class for flights over 6 hours) means editing the same code that handles booking creation. A SWITCH task cleanly separates the compliance check from the approval path, so you can update policy rules without touching the approval or booking logic.

## The Solution

**You just write the policy compliance check, auto-approval, exception routing, and booking processing logic. Conductor handles rule evaluation retries, exception routing, and policy compliance audit trails.**

CheckWorker evaluates the booking against travel policy rules. Maximum hotel rate by city tier, flight class by trip duration, rental vehicle class by trip purpose, and returns a compliance result with a reason. A SWITCH task routes based on the result: CompliantWorker auto-approves policy-compliant bookings, while ExceptionWorker routes non-compliant bookings to a manager or finance team for exception approval, recording who approved and why. After either approval path, ProcessWorker finalizes the booking with the approved parameters. Each worker is a standalone Java class. Conductor handles the routing, retries, and execution tracking.

### What You Write: Workers

Policy retrieval, rule evaluation, exception handling, and compliance reporting workers each enforce one layer of corporate travel governance.

| Worker | Task | What It Does |
|---|---|---|
| **CheckWorker** | `tpl_check` | Checks the input and computes compliance result, reason |
| **CompliantWorker** | `tpl_compliant` | Within policy. auto-approved |
| **ExceptionWorker** | `tpl_exception` | Processes a travel policy exception request. Approves the exception and records the VP-level approver |
| **ProcessWorker** | `tpl_process` | Processes the booking request against the company travel policy |

### The Workflow

```
tpl_check
 │
 ▼
SWITCH (tpl_switch_ref)
 ├── compliant: tpl_compliant
 ├── exception: tpl_exception
 │
 ▼
tpl_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
