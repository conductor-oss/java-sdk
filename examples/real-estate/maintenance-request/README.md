# Property Maintenance Request in Java with Conductor : Submit, Classify, Assign, Complete, and Invoice

## The Problem

You need to handle maintenance requests from tenants across your property portfolio. A tenant reports "kitchen faucet leaking". the request must be logged, classified by category (plumbing, electrical, HVAC, general) and priority (emergency, urgent, routine), assigned to an available technician with the right skills, tracked through completion with labor hours recorded, and invoiced for billing. Each step depends on the previous one: you can't assign a technician without knowing the category, and you can't invoice without knowing the labor hours.

Without orchestration, maintenance requests pile up in email inboxes. Property managers manually classify and assign them, lose track of which requests are pending, and forget to invoice completed work. Building this as a monolithic script means a failure in the assignment step silently prevents invoicing, and tenants never get updates on their request status.

## The Solution

**You just write the request intake, classification, technician assignment, completion tracking, and invoicing logic. Conductor handles dispatch retries, priority routing, and maintenance audit trails.**

Each maintenance step is a simple, independent worker. one logs the request, one classifies category and priority, one assigns the right technician, one records completion details, one generates the invoice. Conductor takes care of executing them in order, retrying if the technician scheduling system is temporarily down, and tracking every request from submission through invoicing so nothing falls through the cracks. ### What You Write: Workers

Request intake, priority assessment, vendor dispatch, and completion verification workers each manage one step of resolving property maintenance issues.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `mtr_submit` | Logs the maintenance request from the tenant with a unique request ID and description |
| **ClassifyWorker** | `mtr_classify` | Determines the request category (plumbing, electrical, HVAC) and priority (emergency, urgent, routine) |
| **AssignWorker** | `mtr_assign` | Selects and assigns an available technician with the right skills based on category and priority |
| **CompleteWorker** | `mtr_complete` | Records work completion. labor hours, parts used, and resolution notes from the technician |
| **InvoiceWorker** | `mtr_invoice` | Generates an invoice for labor and materials, calculating total cost from completion data |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs. ### The Workflow

```
mtr_submit
 │
 ▼
mtr_classify
 │
 ▼
mtr_assign
 │
 ▼
mtr_complete
 │
 ▼
mtr_invoice

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
