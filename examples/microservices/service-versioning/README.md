# API Version Routing with Deprecation Tracking

Clients send an `apiVersion` header, but you need to route v1 requests to the legacy handler
and v2 requests to the new handler -- while logging which versions are still in use so you
know when to sunset v1. This workflow resolves the version, routes via SWITCH, and logs
usage.

## Workflow

```
apiVersion, request
         |
         v
+------------------------+
| sv_resolve_version     |   resolvedVersion, deprecated: true if v1
+------------------------+
         |
         v
    SWITCH on resolvedVersion
    +--"v1"--------------+--"v2"-------------+
    | sv_call_v1         | sv_call_v2        |
    | response:          | response:         |
    |   "v1-response"    |   "v2-response"   |
    +--------------------+-------------------+
         |
         v
+------------------------+
| sv_log_version_usage   |   logged: true
+------------------------+
```

## Workers

**ResolveVersionWorker** -- Resolves `apiVersion` to a `resolvedVersion`. Marks
`deprecated: true` if the resolved version is `"v1"`.

**CallV1Worker** -- Processes with the legacy API. Returns `response: "v1-response"`.

**CallV2Worker** -- Processes with the new API. Returns `response: "v2-response"`.

**LogVersionUsageWorker** -- Logs the resolved version usage. Returns `logged: true`.

## Tests

8 unit tests cover version resolution, v1/v2 routing, and usage logging.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
