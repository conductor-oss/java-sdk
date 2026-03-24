# Benefits Enrollment in Java with Conductor : Plan Presentation, Selection, Validation, Enrollment, and Confirmation

## The Problem

You need to manage open enrollment for employee benefits. During the enrollment period, each employee must see their eligible plan options. medical (PPO, HMO, HDHP), dental (basic, premium), and vision (standard). The employee makes selections for themselves and their dependents. Those selections must be validated, checking that chosen plans are available in the employee's region, dependents meet age and relationship eligibility rules, and HSA elections are only paired with HDHP medical plans. Validated selections are enrolled with each insurance carrier, generating a monthly premium total and an effective date. Finally, the employee receives confirmation with their benefit summary, ID card information, and payroll deduction details. If enrollment happens out of order, selecting before seeing options, or enrolling without validating, employees end up with ineligible plan combinations or missed coverage.

Without orchestration, you'd build a monolithic enrollment portal that queries plan catalogs, captures selections in a form, runs validation rules, calls each carrier's enrollment API, and emails a confirmation. If a carrier API is down during open enrollment crunch time, the enrollment fails and the employee may miss the deadline. If the system crashes after enrolling with the medical carrier but before dental, the employee has partial coverage. HR needs a complete audit trail of every enrollment for ERISA compliance and benefits reconciliation.

## The Solution

**You just write the plan presentation, selection capture, eligibility validation, carrier enrollment, and confirmation logic. Conductor handles enrollment retries, plan selection routing, and benefits audit trails.**

Each stage of the enrollment process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of presenting options before selection, validating before enrollment, enrolling with carriers only after validation passes, confirming only after all carriers acknowledge, retrying if a carrier API is temporarily unavailable during peak enrollment periods, and maintaining a complete audit trail for ERISA compliance.

### What You Write: Workers

Eligibility check, plan comparison, enrollment processing, and confirmation workers guide employees through benefits selection as independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **PresentWorker** | `ben_present` | Retrieves the employee's eligible benefit options. medical plans (PPO, HMO, HDHP), dental tiers, and vision coverage, based on their employment class and location |
| **SelectWorker** | `ben_select` | Captures the employee's plan selections for medical, dental, and vision, including dependent coverage elections |
| **ValidateWorker** | `ben_validate` | Validates selections against plan rules. HDHP/HSA pairing, dependent age limits, regional availability, and ACA affordability requirements |
| **EnrollWorker** | `ben_enroll` | Submits validated selections to insurance carriers, calculates the total monthly premium, and sets the coverage effective date |
| **ConfirmWorker** | `ben_confirm` | Sends the employee a benefits confirmation with plan summaries, ID card details, payroll deduction amounts, and carrier contact information |

### The Workflow

```
ben_present
 │
 ▼
ben_select
 │
 ▼
ben_validate
 │
 ▼
ben_enroll
 │
 ▼
ben_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
