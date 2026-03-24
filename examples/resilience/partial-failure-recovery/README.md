# Implementing Partial Failure Recovery in Java with Conductor : Resume from Last Successful Step

## The Problem

You have a multi-step pipeline (validate, process, finalize) where an intermediate step fails due to a transient issue. When you retry, you don't want to re-execute steps that already succeeded. step 1's side effects (database writes, API calls) would be duplicated. You need to resume from exactly the point of failure, skipping already-completed steps.

Without orchestration, partial failure recovery requires building custom checkpoint logic. Each step must record its completion state, and the retry mechanism must read those checkpoints to know where to start. This is fragile, error-prone, and means every pipeline needs its own checkpointing implementation.

## The Solution

Each step is an independent worker. When step 2 fails, Conductor records which steps completed and which didn't. Calling Conductor's retry endpoint (`POST /workflow/{id}/retry`) resumes from exactly the failed step. step 1 is skipped because it already succeeded. No checkpointing code needed. Every execution shows exactly which steps ran, which were skipped on retry, and what their results were.

### What You Write: Workers

Step1Worker validates input, Step2Worker performs the main processing (which may fail transiently), and Step3Worker finalizes the result, with Conductor's retry endpoint resuming from the exact point of failure without re-executing completed steps.

| Worker | Task | What It Does |
|---|---|---|
| **Step1Worker** | `pfr_step1` | Worker for pfr_step1. first step in the partial failure recovery pipeline. Takes a "data" input and returns { result.. |
| **Step2Worker** | `pfr_step2` | Worker for pfr_step2. second step that simulates a transient failure. Fails on the first attempt, then succeeds on r.. |
| **Step3Worker** | `pfr_step3` | Worker for pfr_step3. third and final step in the partial failure recovery pipeline. Takes a "prev" input and return.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
pfr_step1
 │
 ▼
pfr_step2
 │
 ▼
pfr_step3

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
