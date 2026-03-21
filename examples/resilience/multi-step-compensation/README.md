# Implementing Multi-Step Compensation in Java with Conductor : Account Provisioning with Reverse-Order Undo

## The Problem

You need to provision a new customer account. create the account record, set up billing, and provision cloud resources. If resource provisioning fails (quota exceeded, region unavailable), the billing setup must be undone and the account must be deleted, in reverse order. Each undo operation must receive the output from the original forward step (account ID, billing ID) to know what to clean up.

Without orchestration, compensation logic is tangled with forward logic. Each step must know about every other step's undo operation. Partial failures leave orphaned billing records attached to deleted accounts, or provisioned resources with no associated billing. Testing the compensation path requires simulating failures at each step, which is nearly impossible in a monolithic script.

## The Solution

**You just write the provisioning steps and their matching undo operations. Conductor handles forward sequencing, automatic compensation in reverse order via the failure workflow, retries on each undo step, and a complete record of both the forward and compensation paths.**

The forward workflow runs three workers in sequence. create account, setup billing, provision resources. A separate compensation workflow runs the undo workers in reverse order (undo provision, undo billing, undo account) using the outputs from the forward steps. Conductor's failure workflow feature links them: when the forward workflow fails, compensation runs automatically. Every step in both directions is tracked. ### What You Write: Workers

Three forward workers. CreateAccountWorker, SetupBillingWorker, and ProvisionResourcesWorker. Run in sequence, with matching undo workers (UndoProvisionWorker, UndoBillingWorker, UndoAccountWorker) that execute in reverse order when any step fails.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAccountWorker** | `msc_create_account` | Worker for msc_create_account. creates an account and returns a deterministic accountId. |
| **ProvisionResourcesWorker** | `msc_provision_resources` | Worker for msc_provision_resources. provisions resources. If failAt input equals "provision", the task returns FAILE.. |
| **SetupBillingWorker** | `msc_setup_billing` | Worker for msc_setup_billing. sets up billing and returns a deterministic billingId. |
| **UndoAccountWorker** | `msc_undo_account` | Worker for msc_undo_account. undoes account creation. |
| **UndoBillingWorker** | `msc_undo_billing` | Worker for msc_undo_billing. undoes billing setup. |
| **UndoProvisionWorker** | `msc_undo_provision` | Worker for msc_undo_provision. undoes resource provisioning. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
Input -> CreateAccountWorker -> ProvisionResourcesWorker -> SetupBillingWorker -> UndoAccountWorker -> UndoBillingWorker -> UndoProvisionWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
