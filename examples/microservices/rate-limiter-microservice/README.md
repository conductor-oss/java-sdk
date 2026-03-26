# Rate Limiting with SWITCH-Based Allow/Reject Routing

Without rate limiting, a single misbehaving client can exhaust your API capacity. This
workflow checks the client's quota (42 of 100 requests used in a 1-minute window), then
routes via SWITCH: allowed requests are processed and the counter incremented, while
over-limit requests are rejected with a `retryAfter: 30` seconds header.

## Workflow

```
clientId, endpoint, request
            |
            v
+--------------------+
| rl_check_quota     |   42/100 used, remaining: 58, window: 1m
+--------------------+
         |
         v
    SWITCH on allowed
    +--true (under limit)-----+--false (over limit)-------+
    | rl_process_request      | rl_reject_request         |
    | response: {status: ok}  | rejected: true            |
    |         |               | retryAfter: 30            |
    |         v               +----------------------------+
    | rl_update_counter       |
    | updated: true           |
    +-------------------------+
```

## Workers

**CheckQuotaWorker** -- Checks `clientId` quota on `endpoint`: 42/100 used,
`remaining: 58`, `limit: 100`, `window: "1m"`.

**ProcessRequestWorker** -- Processes the allowed request. Returns
`response: {status: "ok"}`.

**RejectRequestWorker** -- Rejects the over-limit request. Returns `rejected: true`,
`retryAfter: 30`.

**UpdateCounterWorker** -- Increments the client's request counter. Returns
`updated: true`.

## Tests

30 unit tests cover quota checking, request processing, rejection, and counter updates.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
