# Nested Switch in Java with Conductor

Multi-level decision tree using nested SWITCH tasks with value-param. ## The Problem

You need to route a request through a multi-level decision tree based on region and subscription tier. A US premium customer gets different processing than an EU standard customer or a customer from an unlisted region. The first level routes by region (US, EU, or other), and within each region, a second level routes by tier (premium or standard/default). Each combination. US/premium, US/standard, EU/premium, EU/standard, other/any. runs completely different processing logic. After the region-and-tier-specific processing completes, a final completion step runs regardless of which branch was taken.

Without orchestration, you'd write deeply nested if/else or switch statements, with each branch calling different functions. Adding a new region or tier means modifying the routing code, retesting every branch, and hoping you didn't break an existing path. There is no record of which branch was taken for a given request, and debugging why a customer got the wrong processing requires tracing through nested conditionals.

## The Solution

**You just write the region-specific and tier-specific processing workers. Conductor handles the nested routing, branch tracking, and completion.**

This example demonstrates nested SWITCH tasks. a multi-level decision tree declared in the workflow definition. The outer SWITCH routes on `region` (US, EU, or default). Within the US branch, a nested SWITCH routes on `tier` (premium or default/standard). Within the EU branch, another nested SWITCH does the same tier routing. Each leaf node is a dedicated worker. NsUsPremiumWorker handles US premium requests, NsEuStandardWorker handles EU standard requests, and NsOtherRegionWorker catches everything else. After the nested switches resolve, NsCompleteWorker runs the final completion step. Conductor records exactly which branch was taken, so you can see that a request with `region=EU, tier=premium` was routed to `ns_eu_premium`.

### What You Write: Workers

Six workers handle the multi-level decision tree: region-and-tier-specific workers (US Premium, US Standard, EU Premium, EU Standard, Other Region) each process their branch, and NsCompleteWorker runs the final completion step regardless of which path was taken.

| Worker | Task | What It Does |
|---|---|---|
| **NsCompleteWorker** | `ns_complete` | Final completion step after all nested switch branches. |
| **NsEuPremiumWorker** | `ns_eu_premium` | Handles EU region, premium tier requests. |
| **NsEuStandardWorker** | `ns_eu_standard` | Handles EU region, standard (default) tier requests. |
| **NsOtherRegionWorker** | `ns_other_region` | Handles requests from regions other than US or EU (default case). |
| **NsUsPremiumWorker** | `ns_us_premium` | Handles US region, premium tier requests. |
| **NsUsStandardWorker** | `ns_us_standard` | Handles US region, standard (default) tier requests. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (region_switch_ref)
 ├── US: route_us_tier
 ├── EU: route_eu_tier
 └── default: ns_other_region
 │
 ▼
ns_complete

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
