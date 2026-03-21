# Government Permit in Java with Conductor

Processes a government permit application: receiving the application, validating documents, routing to a zoning board review, and issuing or denying the permit via a SWITCH task. ## The Problem

You need to process a government permit application. A citizen submits an application for a permit (building, business, event), the application is validated for completeness and jurisdiction, a reviewer assesses it against regulations and zoning rules, and the permit is either issued or denied with explanation. Issuing a permit without proper review creates legal liability for the government; denying without explanation violates due process.

Without orchestration, you'd manage permits through a legacy system with paper forms, manual reviews, and status tracking in spreadsheets. losing applications in the queue, missing review deadlines mandated by statute, and struggling to produce audit trails when a permit decision is challenged.

## The Solution

**You just write the application intake, document validation, zoning review, and permit issuance or denial logic. Conductor handles review retries, approval routing, and permit application audit trails.**

Each permit concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the application flow (apply, validate, review, issue/deny), routing via a SWITCH task to the correct outcome, tracking every application with timestamps and reviewer notes, and resuming from the last step if the process crashes. ### What You Write: Workers

Application intake, document validation, zoning review, and permit issuance workers each handle one stage of the government permitting process.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `gvp_apply` | Receives and validates the permit application with applicant details and permit type |
| **DenyWorker** | `gvp_deny` | Denies the permit application with the specified reason and records the denial |
| **IssueWorker** | `gvp_issue` | Issues the approved permit to the applicant with a unique permit number |
| **ReviewWorker** | `gvp_review` | Conducts a zoning board review of the application and returns an approve/deny decision |
| **ValidateWorker** | `gvp_validate` | Validates the application for completeness and verifies all required documents |

### The Workflow

```
gvp_apply
 │
 ▼
gvp_validate
 │
 ▼
gvp_review
 │
 ▼
SWITCH (gvp_switch_ref)
 ├── approve: gvp_issue
 ├── deny: gvp_deny

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
