# Service Decomposition in Java with Conductor

Strangler fig pattern: routes requests to monolith or microservice based on feature flags, with optional shadow comparison. ## The Problem

Migrating from a monolith to microservices cannot happen all at once. The strangler fig pattern uses feature flags to gradually route requests from the monolith to new microservices. This workflow checks a feature flag to decide the routing target (monolith, microservice, or shadow mode), and in shadow mode runs both in parallel to compare results before committing to the new service.

Without orchestration, the routing proxy is a bespoke middleware that hard-codes if/else branches for each migrated feature. Shadow-mode comparison requires custom diff logic, and there is no visibility into what percentage of traffic is hitting the monolith vs the microservice.

## The Solution

**You just write the feature-flag check, monolith-call, microservice-call, and result-comparison workers. Conductor handles flag-based conditional routing, parallel shadow execution, and full visibility into migration traffic splits.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers implement the strangler fig: CheckFeatureFlagWorker determines the routing target, CallMonolithWorker handles legacy requests, CallMicroserviceWorker handles migrated ones, and CompareResultsWorker diffs outputs in shadow mode.

| Worker | Task | What It Does |
|---|---|---|
| **CallMicroserviceWorker** | `sd_call_microservice` | Calls the new microservice to process the request. |
| **CallMonolithWorker** | `sd_call_monolith` | Calls the legacy monolith to process the request. |
| **CheckFeatureFlagWorker** | `sd_check_feature_flag` | Checks feature flag to determine routing target. |
| **CompareResultsWorker** | `sd_compare_results` | Compares results from monolith and microservice in shadow mode. |

the workflow coordination stays the same.

### The Workflow

```
sd_check_feature_flag
 │
 ▼
SWITCH (route_ref)
 ├── microservice: sd_call_microservice
 ├── shadow: parallel_compare -> join_compare -> sd_compare_results
 └── default: sd_call_monolith

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
