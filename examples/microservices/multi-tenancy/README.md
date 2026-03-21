# Multi Tenancy in Java with Conductor

Tenant-isolated workflows with per-tenant routing. ## The Problem

In a multi-tenant system, each request must be routed to the correct tenant context, processed according to the tenant's tier (which determines rate limits, features, and resource allocation), and logged for per-tenant billing. The tenant's tier affects which resources and SLAs apply.

Without orchestration, tenant isolation logic is scattered across middleware and service code. Changing a tenant's tier or adding a new billing rule requires modifying multiple services, and there is no centralized audit trail of per-tenant usage.

## The Solution

**You just write the tenant-resolution, request-processing, and usage-logging workers. Conductor handles per-tenant routing, usage tracking across every request, and durable execution state per tenant.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Three workers isolate tenant concerns: ResolveTenantWorker maps a tenant ID to its configuration and tier, ProcessRequestWorker applies tier-appropriate resources, and LogUsageWorker records consumption for billing.

| Worker | Task | What It Does |
|---|---|---|
| **LogUsageWorker** | `mt_log_usage` | Logs the tenant's usage (action and cost) for billing and audit purposes. |
| **ProcessRequestWorker** | `mt_process_request` | Processes the tenant's request using tier-appropriate resources and returns the result with cost. |
| **ResolveTenantWorker** | `mt_resolve_tenant` | Resolves a tenant ID to its configuration: tier (enterprise/standard), region, and isolation level. |

the workflow coordination stays the same.

### The Workflow

```
mt_resolve_tenant
 │
 ▼
mt_process_request
 │
 ▼
mt_log_usage

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
