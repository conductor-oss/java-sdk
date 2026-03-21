# Lease Management in Java with Conductor : Create, Sign, Activate, Renew, or Terminate

## The Problem

You need to manage residential or commercial leases from creation through completion. A new lease must be drafted with terms (rent amount, duration, deposit), signed by both tenant and landlord, and then activated to begin the tenancy. At the end of the lease term, the workflow needs to handle either renewal (new terms, rent escalation) or termination (security deposit return, move-out inspection). The path taken depends on whether the tenant or landlord requests renewal or termination, and every action must be recorded for legal compliance.

Without orchestration, lease management is a tangle of manual steps. Property managers track lease statuses in spreadsheets, miss renewal deadlines, activate leases before signatures are collected, or forget to process terminations. A monolithic script that tries to handle both renewal and termination paths becomes a mess of if/else branches that nobody wants to touch when lease terms change.

## The Solution

**You just write the lease creation, signature collection, activation, and renewal or termination logic. Conductor handles notification retries, payment scheduling, and lease lifecycle audit trails.**

Each lease lifecycle step is a simple, independent worker. one creates the lease, one handles signing, one activates it, and then a SWITCH task routes to either renewal or termination based on the requested action. Conductor takes care of executing them in order, ensuring no lease is activated without a signature, routing to the correct end-of-lease path, and maintaining a complete history of every lease action. ### What You Write: Workers

Lease drafting, tenant notification, payment scheduling, and renewal tracking workers each handle one aspect of the lease lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CreateLeaseWorker** | `lse_create` | Generates a lease agreement with terms (rent, duration, deposit) for the tenant/property pair |
| **SignLeaseWorker** | `lse_sign` | Records lease signatures from tenant and landlord, producing a fully executed agreement |
| **ActivateLeaseWorker** | `lse_activate` | Activates the lease. starts the tenancy, enables rent collection, sets key dates |
| **RenewLeaseWorker** | `lse_renew` | Processes lease renewal with updated terms and rent escalation (renew path) |
| **TerminateLeaseWorker** | `lse_terminate` | Handles lease termination. schedules move-out inspection, initiates deposit return (terminate path) |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs. ### The Workflow

```
lse_create
 │
 ▼
lse_sign
 │
 ▼
lse_activate
 │
 ▼
SWITCH (lse_switch_ref)
 ├── renew: lse_renew
 ├── terminate: lse_terminate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
