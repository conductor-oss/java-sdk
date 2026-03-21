# Service Versioning in Java with Conductor

API version management with version routing. ## The Problem

When an API evolves, clients on different versions must be served correctly. This workflow resolves the requested API version (mapping aliases like 'latest' to 'v2'), routes to the correct versioned handler (v1 or v2), and logs the version usage for deprecation tracking.

Without orchestration, version routing is embedded in API gateway config or application code with if/else branches. Tracking which clients are still using deprecated versions requires parsing access logs, and adding a v3 means modifying the routing logic.

## The Solution

**You just write the version-resolver, versioned API handlers, and usage-logging workers. Conductor handles version-based routing via SWITCH, per-version retries, and usage analytics for deprecation decisions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers manage API versioning: ResolveVersionWorker maps aliases like 'latest' to a concrete version, CallV1Worker and CallV2Worker serve their respective APIs, and LogVersionUsageWorker tracks usage for deprecation planning.

| Worker | Task | What It Does |
|---|---|---|
| **CallV1Worker** | `sv_call_v1` | Handles requests using the v1 (legacy) API handler. |
| **CallV2Worker** | `sv_call_v2` | Handles requests using the v2 (current) API handler. |
| **LogVersionUsageWorker** | `sv_log_version_usage` | Logs which API version was requested and resolved, for deprecation tracking. |
| **ResolveVersionWorker** | `sv_resolve_version` | Resolves the requested API version to a concrete version (e.g., 'latest' -> 'v2') and flags deprecated versions. |

the workflow coordination stays the same.

### The Workflow

```
sv_resolve_version
 │
 ▼
SWITCH (route_ref)
 ├── v2: sv_call_v2
 └── default: sv_call_v1
 │
 ▼
sv_log_version_usage

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
