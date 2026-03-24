# Circuit Breaker with Fallback to Cached Data

When a downstream service is flaky, every call attempt adds latency and wastes resources.
This workflow checks the circuit breaker state first. If CLOSED (healthy), it calls the
service and records the result. If OPEN, it skips the call entirely and returns cached data
via a SWITCH-routed fallback path.

## Workflow

```
serviceName, request
         |
         v
+---------------------+
| cb_check_circuit    |   circuitState: "closed", failureCount: 0, threshold: 5
+---------------------+
         |
         v
    SWITCH on circuitState
    +--"closed"------------------+--"open"-------------------+
    | cb_call_service            | cb_fallback               |
    | success: true              | response: cached_response |
    | latencyMs: 45              | fromCache: true           |
    |         |                  +----------------------------+
    |         v                  |
    | cb_record_result           |
    | recorded: true             |
    +----------------------------+
```

## Workers

**CheckCircuitWorker** -- Checks the circuit state for `serviceName`. Returns
`circuitState: "closed"`, `failureCount: 0`, `threshold: 5`.

**CallServiceWorker** -- Calls the service. Returns `success: true`,
`response: {data: "service_response"}`, `latencyMs: 45`.

**FallbackWorker** -- Returns cached data when the circuit is open. Returns
`response: {data: "cached_response"}`, `fromCache: true`.

**RecordResultWorker** -- Records the call result for circuit state tracking. Returns
`recorded: true`.

## Tests

8 unit tests cover circuit checking, service calls, fallback, and result recording.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
