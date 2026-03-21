# Implementing Compensation Workflows in Java with Conductor: Forward Steps with Automatic Undo on Failure

## The Problem

You have a multi-step provisioning process where each step makes a side effect that needs to be reversed if a later step fails. Step A creates a cloud resource, Step B inserts a database record, Step C sends a notification. If Step C fails (external service unavailable), the database record from Step B must be deleted and the cloud resource from Step A must be cleaned up. In reverse order. Without this rollback, you end up with orphaned resources, dangling database records, and a system in an inconsistent state.

### What Goes Wrong Without Compensation

Consider this provisioning flow without compensation:

1. Step A creates an EC2 instance. **success** (resource exists)
2. Step B inserts a billing record. **success** (row exists in database)
3. Step C sends a confirmation email. **FAILS** (SMTP server down)

The workflow fails, but the EC2 instance is still running (costing money) and the billing record still exists (customer gets charged). Nobody knows these orphans exist until the monthly bill arrives or a customer complains.

The compensation pattern solves this with a separate compensation workflow that runs undo operations in reverse order: `Undo B` (delete billing record) then `Undo A` (terminate EC2 instance). Each undo worker receives the output of its corresponding forward step, so it knows exactly what to clean up.

## The Solution

**You just write the forward steps and their matching undo operations. Conductor handles forward execution sequencing, reverse-order compensation on failure, retries on each undo step, and a full audit trail of every forward and compensation action with their inputs and outputs.**

Each forward step and its corresponding undo are simple, independent workers. Step A creates a resource, UndoA deletes it. Step B inserts a record, UndoB removes it. When Step C fails, the main workflow ends with FAILED status. You then start the compensation workflow, which runs the undo workers in reverse order automatically. Every compensation action is tracked, so you can see exactly which steps were undone and whether the rollback completed successfully. ### What You Write: Workers

Three forward workers. CompStepAWorker, CompStepBWorker, and CompStepCWorker. Execute provisioning actions, while CompUndoBWorker and CompUndoAWorker reverse completed steps in reverse order when any forward step fails.

| Worker | Task | What It Does |
|---|---|---|
| **CompStepAWorker** | `comp_step_a` | Creates a resource. Always succeeds, returning `{result: "resource-A-created"}`. |
| **CompStepBWorker** | `comp_step_b` | Inserts a database record. Always succeeds, returning `{result: "record-B-inserted"}`. Receives Step A's result as `prevResult`. |
| **CompStepCWorker** | `comp_step_c` | Sends a notification. Fails if `failAtStep="C"` (simulating an external service outage), otherwise returns `{result: "notification-C-sent"}`. |
| **CompUndoAWorker** | `comp_undo_a` | Compensation: reverses Step A by deleting the created resource. Receives the `original` value from Step A's output. Returns `{undone: true}`. |
| **CompUndoBWorker** | `comp_undo_b` | Compensation: reverses Step B by removing the inserted record. Receives the `original` value from Step B's output. Returns `{undone: true}`. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflows

**Main workflow** (`compensatable_workflow`):

```
comp_step_a --> comp_step_b --> comp_step_c
 (can fail)

```

**Compensation workflow** (`compensation_workflow`): runs when main fails:

```
comp_undo_b --> comp_undo_a
(reverse order: undo B first, then A. Skip C since it never completed)

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
