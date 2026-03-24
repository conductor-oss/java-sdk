# Property Tax Assessment in Java with Conductor : Valuation, Calculation, and Owner Notification

## The Problem

You need to assess property taxes for a municipality. Each assessment involves pulling property records (square footage, lot size, bedroom count), appraising the property's market value, applying the local mill rate to compute the tax bill, mailing the owner a notice, and opening a statutory appeal window. These steps must run in strict sequence. you cannot calculate tax without a valuation, and you cannot notify an owner without a final amount.

Without orchestration, you'd wire all of this into a single monolithic class. querying the property database, calling the appraisal service, computing the tax, sending the notification, and recording the appeal deadline. If the appraisal service times out, you'd need retry logic. If the notification fails after the tax is already calculated, you'd need to track partial progress. Every step needs error handling, and auditors need a complete record of every assessment for compliance.

## The Solution

**You just write the property data collection, valuation, tax calculation, owner notification, and appeal processing logic. Conductor handles valuation retries, notice generation, and assessment audit trails.**

Each stage of the tax assessment is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in the right order, retrying on failure, tracking every assessment from data collection through appeal window, and resuming if the process crashes mid-assessment.

### What You Write: Workers

Property data collection, valuation, assessment calculation, and notice generation workers each handle one phase of the tax assessment cycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `txa_collect_data` | Pulls property records (sqft, bedrooms, lot size) for a given property ID and tax year |
| **AssessPropertyWorker** | `txa_assess_property` | Appraises fair market value and determines the applicable tax rate based on property data |
| **CalculateWorker** | `txa_calculate` | Multiplies the assessed value by the mill rate to produce the final tax bill |
| **NotifyWorker** | `txa_notify` | Sends the tax bill notice to the property owner |
| **AppealWorker** | `txa_appeal` | Opens the statutory appeal window and records the deadline for the property owner to contest |

### The Workflow

```
txa_collect_data
 │
 ▼
txa_assess_property
 │
 ▼
txa_calculate
 │
 ▼
txa_notify
 │
 ▼
txa_appeal

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
