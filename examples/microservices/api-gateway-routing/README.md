# API Gateway Routing in Java with Conductor

API gateway routing workflow that authenticates requests, checks rate limits, routes to backend services, and transforms responses. ## The Problem

An API gateway must authenticate every inbound request, enforce rate limits per client, route the call to the right backend service, and transform the response before returning it. Each of these concerns lives in a different service, and they must run in strict sequence. Routing depends on auth, and the response transform depends on the routing result.

Without orchestration, you end up hard-coding the call chain inside a single gateway class, manually threading client IDs between steps, and writing retry/timeout logic around each HTTP call. Any change to the pipeline (adding a logging step, rearranging rate-check order) forces a redeploy of the whole gateway.

## The Solution

**You just write the auth, rate-check, routing, and response-transform workers. Conductor handles ordered execution, automatic retries on transient failures, and per-request observability.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers divide the routing pipeline: AuthenticateWorker validates tokens, RateCheckWorker enforces per-client limits, RouteRequestWorker forwards to backends, and TransformResponseWorker shapes the reply for the caller.

| Worker | Task | What It Does |
|---|---|---|
| **AuthenticateWorker** | `gw_authenticate` | Validates authentication tokens for incoming API requests. |
| **RateCheckWorker** | `gw_rate_check` | Checks rate limits for a client. |
| **RouteRequestWorker** | `gw_route_request` | Routes a request to the appropriate backend service. |
| **TransformResponseWorker** | `gw_transform_response` | Transforms a service response for the client version. |

the workflow coordination stays the same.

### The Workflow

```
gw_authenticate
 │
 ▼
gw_rate_check
 │
 ▼
gw_route_request
 │
 ▼
gw_transform_response

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
