# Implementing Graceful Degradation in Java with Conductor: Core Processing with Optional Enrichment and Analytics

The recommendation engine goes down at 9 AM on Black Friday. Your product page has a "You might also like" section that calls the recommendation API synchronously. Now every product page takes 30 seconds to load, then shows a blank section where the recommendations should be. Customers leave. Revenue drops. The fix was obvious in retrospect: the product page should have shown bestsellers as a fallback and loaded in 200ms. But your code doesn't distinguish between "must succeed" and "nice to have," so one optional service took down the whole experience. This example builds a graceful degradation pipeline with Conductor: the core order process always completes, while optional enrichment and analytics run in parallel via `FORK_JOIN`. If either fails, the result is flagged as "degraded" but the customer still gets their confirmation.

## The Problem

Your order processing pipeline has a core function (create the order: this must succeed) plus optional enhancements, data enrichment from a third-party API (add customer demographics, credit score) and analytics tracking (send event to Segment/Mixpanel). When the enrichment API is down or the analytics service is slow, you do not want the core pipeline to fail or stall. The order should still be created, but with a "degraded" flag indicating which optional services were unavailable.

### What Goes Wrong Without Graceful Degradation

Consider an e-commerce order pipeline without graceful degradation:

1. Core: Create order ORD-123. **success**
2. Enrichment: Look up customer credit score. **TIMEOUT** (third-party API is slow)
3. Analytics: Track purchase event. **waiting...**

Without graceful degradation, the enrichment timeout blocks the entire pipeline. The customer sees a spinning wheel for 30 seconds, then gets an error page. The order was created but never confirmed to the customer. The analytics event is never sent. Support gets a ticket.

With graceful degradation, the core order is created immediately. Enrichment and analytics run in parallel. If either fails, the result is flagged as `degraded: true` but the order still completes. The customer gets their confirmation page in 200ms instead of a 30-second timeout.

## The Solution

**You just write the core processing and optional enrichment logic. Conductor handles FORK/JOIN parallel execution of optional services, continuing the pipeline when optional tasks fail, and clear tracking of which services were available versus degraded for every execution.**

The core process worker runs first and always succeeds. Then Conductor's FORK/JOIN runs the enrichment and analytics workers in parallel. Both are optional, so their failure does not fail the workflow. The finalize worker checks which optional services succeeded, sets a degraded flag if any failed, and produces the final result. Every execution shows which services were available and which were degraded. ### What You Write: Workers

CoreProcessWorker handles the required order creation that must always succeed, EnrichWorker and AnalyticsWorker run in parallel via FORK/JOIN as optional enhancements, and FinalizeWorker checks which services responded and sets a degradation flag.

| Worker | Task | What It Does |
|---|---|---|
| **CoreProcessWorker** | `gd_core_process` | The required core processing step. Accepts `{data: "order-123"}`, returns `{result: "processed-order-123"}`. Always succeeds. Uses "default" when no data input is provided. |
| **EnrichWorker** | `gd_enrich` | Optional enrichment step. When `available=true` (or not specified), returns `{enriched: true}`. When `available=false`, returns `{enriched: false}` (simulating the enrichment service being down). |
| **AnalyticsWorker** | `gd_analytics` | Optional analytics tracking step. When `available=true` (or not specified), returns `{tracked: true}`. When `available=false`, returns `{tracked: false}` (simulating the analytics service being down). |
| **FinalizeWorker** | `gd_finalize` | Checks enrichment and analytics results. Sets `degraded=true` if either `enriched` or `analytics` is false. Returns `{enriched, analytics, degraded}`. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
gd_core_process (always runs, always succeeds)
 |
 v
FORK_JOIN
 |-- branch 1: gd_enrich (optional. Can fail without failing the workflow)
 |-- branch 2: gd_analytics (optional. Can fail without failing the workflow)
 |
 v
JOIN (wait for both branches)
 |
 v
gd_finalize (checks which services responded, sets degraded flag)

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
