# Multi-Tenant Approval in Java Using Conductor : Tenant Config Loading, Amount-Based SWITCH to Manager or Manager+Executive WAIT Chains, and Tenant-Scoped Finalization

## Different Tenants Have Different Approval Rules and Thresholds

In a multi-tenant SaaS application, each tenant has its own approval configuration. Different amount thresholds, different required approval levels, different approver roles. The workflow loads the tenant's configuration, determines the required approval level based on the amount, routes to the appropriate approval chain, then finalizes. If the config loading step uses stale data, you can re-run just that step.

## The Solution

**You just write the tenant-config-loading and tenant-scoped finalization workers. Conductor handles the per-tenant routing to the correct approval chain.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

MtaLoadConfigWorker loads each tenant's approval thresholds and required levels, and MtaFinalizeWorker applies tenant-scoped post-approval logic, the per-tenant SWITCH routing to the correct approval chain is declarative.

| Worker | Task | What It Does |
|---|---|---|
| **MtaLoadConfigWorker** | `mta_load_config` | Loads the tenant's approval configuration. looks up the auto-approve limit and approval levels for the tenantId, compares against the request amount, and outputs the approvalLevel (none, manager, or executive) that determines the SWITCH path |
| *SWITCH* | `approval_switch` | Routes based on the tenant's approvalLevel: "manager" goes to a single manager WAIT, "executive" goes to sequential manager + executive WAITs, "none" skips directly to finalization | Built-in Conductor SWITCH. no worker needed |
| *WAIT task(s)* | Manager / Executive | Pauses for each required approval level. manager WAIT pauses until a manager approves via `POST /tasks/{taskId}`, executive WAIT (if required) pauses for a second executive approval | Built-in Conductor WAIT, no worker needed |
| **MtaFinalizeWorker** | `mta_finalize` | Finalizes the approved request with tenant context. records which approval chain was used, the tenant and amount, and triggers tenant-specific post-approval logic |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
mta_load_config
 │
 ▼
SWITCH (approval_switch_ref)
 ├── manager: manager_approval_wait
 ├── executive: manager_approval_wait -> executive_approval_wait
 │
 ▼
mta_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
