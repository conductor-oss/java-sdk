# Performance Review in Java with Conductor : Self-Evaluation, Manager Evaluation, Calibration, and Finalization

## The Problem

You need to run the annual performance review cycle. Each employee writes a self-evaluation covering goal progress, accomplishments, and development areas. Their manager completes an evaluation with competency ratings, goal achievement scores, and narrative feedback. The calibration step normalizes ratings across teams to ensure consistent standards. preventing rating inflation in lenient teams or deflation in strict ones. Finally, the review is finalized with a composite rating that feeds into compensation, promotion, and development decisions. Each step must complete before the next, you cannot calibrate without both evaluations, and you cannot finalize without calibration.

Without orchestration, you'd manage this through email reminders, spreadsheet trackers, and manual follow-ups. HR sends reminder emails, managers submit evaluations at different times, calibration happens on whiteboards, and final ratings are entered one by one. If a manager misses their deadline, the entire team's calibration is delayed. HR has no real-time visibility into which of hundreds of reviews are stuck at which stage.

## The Solution

**You just write the self-evaluation, manager evaluation, calibration, and review finalization logic. Conductor handles review routing, calibration sequencing, and evaluation cycle audit trails.**

Each stage of the review cycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting the self-eval before the manager eval, calibrating only after both evaluations are in, finalizing after calibration, and giving HR complete real-time visibility into every review's progress across the organization.

### What You Write: Workers

Goal retrieval, self-assessment collection, manager review, and calibration workers each manage one stage of the performance evaluation cycle.

| Worker | Task | What It Does |
|---|---|---|
| **SelfEvalWorker** | `pfr_self_eval` | Collects the employee's self-evaluation with goal progress, accomplishments, and development areas |
| **ManagerEvalWorker** | `pfr_manager_eval` | Gathers the manager's evaluation with competency ratings, goal achievement scores, and narrative feedback |
| **CalibrateWorker** | `pfr_calibrate` | Normalizes ratings across teams against organizational standards and distribution guidelines |
| **FinalizeWorker** | `pfr_finalize` | Finalizes the review with composite rating, development plan, and compensation recommendation |

### The Workflow

```
pfr_self_eval
 │
 ▼
pfr_manager_eval
 │
 ▼
pfr_calibrate
 │
 ▼
pfr_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
