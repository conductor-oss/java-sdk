# Gateway Routing with Rate Limiting and Version-Aware Transforms

Your API gateway needs to authenticate tokens, enforce rate limits per client, route
requests to the right service, and format responses differently for different client
versions. This workflow chains auth, rate checking (45/100 used, 55 remaining),
routing to order-service, and version-aware response transformation.

## Workflow

```
path, method, headers, body
           |
           v
+--------------------+     +------------------+     +--------------------+     +---------------------------+
| gw_authenticate    | --> | gw_rate_check    | --> | gw_route_request   | --> | gw_transform_response     |
+--------------------+     +------------------+     +--------------------+     +---------------------------+
  clientId: client-42       45/100 used              POST /orders ->           formatted for
  clientVersion: v2         55 remaining              order-service             clientVersion v2
  requestId: REQ-fixed-001  allowed: true             orderId: ORD-123
```

## Workers

**AuthenticateWorker** -- Validates the token for the requested `path`. Returns
`clientId: "client-42"`, `clientVersion: "v2"`, `requestId: "REQ-fixed-001"`.

**RateCheckWorker** -- Checks the client's rate limit: 45 of 100 requests used, `remaining:
55`, `allowed: true`.

**RouteRequestWorker** -- Routes `method` + `path` to order-service. Returns `statusCode: 200`
with `response: {orderId: "ORD-123", status: "confirmed"}`.

**TransformResponseWorker** -- Formats the response for the `clientVersion`.

## Tests

31 unit tests cover authentication, rate limiting, routing, and response transformation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
