# Donor Management in Java with Conductor

## The Problem

A new donor makes their first gift to your nonprofit. The development team needs to acquire the donor by recording their contact information, begin stewardship with engagement touchpoints, send a tax-deductible donation acknowledgment letter, create a retention plan based on their giving level, and evaluate whether they qualify for upgrade to major-donor status. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the donor intake, gift recording, acknowledgment, and stewardship logic. Conductor handles gift processing retries, acknowledgment sequencing, and donor engagement audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Donor intake, gift processing, acknowledgment, and stewardship workers each manage one phase of the donor relationship lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AcknowledgeWorker** | `dnr_acknowledge` | Sends a personalized donation acknowledgment letter to the donor for tax purposes |
| **AcquireWorker** | `dnr_acquire` | Records the new donor's name and email, assigning a unique donor ID |
| **RetainWorker** | `dnr_retain` | Creates a retention plan for the donor based on their giving level, with a recommended next action (e.g., annual report) |
| **StewardWorker** | `dnr_steward` | Manages donor engagement touchpoints, tracking contact history and sentiment for the donor's first gift |
| **UpgradeWorker** | `dnr_upgrade` | Evaluates whether the donor qualifies for upgrade from their current level to major-donor status |

### The Workflow

```
dnr_acquire
 │
 ▼
dnr_steward
 │
 ▼
dnr_acknowledge
 │
 ▼
dnr_retain
 │
 ▼
dnr_upgrade

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
