# Multi-Tenant Request Processing with Usage Metering

Each tenant needs isolated processing, but all requests flow through the same API. Without
tenant resolution, you cannot enforce tier-based isolation or meter usage for billing. This
workflow resolves the tenant (enterprise tier, us-east-1, isolated), processes the request
at $0.05 per call, and logs the usage for billing.

## Workflow

```
tenantId, action, data
         |
         v
+----------------------+     +----------------------+     +-----------------+
| mt_resolve_tenant    | --> | mt_process_request   | --> | mt_log_usage    |
+----------------------+     +----------------------+     +-----------------+
  tier: "enterprise"          result: "processed"          logged: true
  region: "us-east-1"         cost: $0.05
  isolated: true
```

## Workers

**ResolveTenantWorker** -- Resolves `tenantId` to `tier: "enterprise"`,
`region: "us-east-1"`, `isolated: true`.

**ProcessRequestWorker** -- Processes the `action` for the tenant at their tier. Returns
`result: "processed"`, `cost: 0.05`.

**LogUsageWorker** -- Logs `tenantId` usage with the cost for downstream billing systems.
Returns `logged: true`.

## Tests

6 unit tests cover tenant resolution, request processing, and usage logging.
Enterprise tenants get isolated processing in their own region.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
