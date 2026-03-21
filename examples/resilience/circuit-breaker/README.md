# Implementing Circuit Breaker Pattern in Java with Conductor: Prevent Cascading Failures with State-Based Routing

## The Problem

You have a service that depends on an unreliable downstream dependency, a payment API, a third-party geocoding service, an internal inventory microservice. When that dependency starts failing, every request continues to hit it, making things worse (thundering herd), and each request waits for a timeout before failing (degrading your own response times). Meanwhile, the downstream service is overwhelmed and cannot recover.

### The Circuit Breaker State Machine

The circuit breaker pattern tracks failure counts and transitions between three states:

```
 success
 +------------------+
 | |
 v failureCount | failureCount
 CLOSED ------------> OPEN <-- stays OPEN until cooldown
 ^ >= threshold |
 | | cooldown expires
 | v
 +--- HALF_OPEN ----+
 (test one failure -> back to OPEN
 request) success -> back to CLOSED

```

- **CLOSED**: Normal operation. Requests go through to the real service. Failures are counted.
- **OPEN**: Too many failures. Requests are immediately routed to fallback (cached data, default values) without touching the failing service. This gives the downstream service time to recover.
- **HALF_OPEN**: After a cooldown period, one test request is sent to the real service. If it succeeds, the circuit closes. If it fails, the circuit opens again.

Without orchestration, implementing circuit breakers means embedding state management, threshold logic, and fallback routing into every service that calls a dependency. Each implementation is slightly different, the failure thresholds are hardcoded, and nobody can see which circuits are open across the system.

## The Solution

**You just write the service call and fallback logic. Conductor handles SWITCH-based state routing between OPEN and CLOSED paths, retries on the service call, and visibility into every circuit evaluation showing which state was active and whether the live or fallback path was taken.**

Each circuit breaker concern is a simple, independent worker. One evaluates the circuit state from failure count and threshold, one makes the actual service call, one returns fallback data. Conductor's SWITCH task handles the routing: when the circuit is OPEN, it skips the service call entirely and routes to the fallback path. Every circuit evaluation is tracked with inputs, outputs, and timing, giving you full visibility into which circuits tripped and when. ### What You Write: Workers

CheckCircuitWorker evaluates the circuit state from failure counts and thresholds, CallServiceWorker makes the live service call when the circuit is CLOSED, and FallbackWorker returns cached data when the circuit is OPEN to protect the failing dependency.

| Worker | Task | What It Does |
|---|---|---|
| **CheckCircuitWorker** | `cb_check_circuit` | Evaluates the circuit breaker state. If `circuitState` is "OPEN" or "HALF_OPEN", returns that state directly (manual override). Otherwise, compares `failureCount` against `threshold`. Returns "OPEN" if failures >= threshold, "CLOSED" otherwise. Defaults: failureCount=0, threshold=3. |
| **CallServiceWorker** | `cb_call_service` | Makes the real service call. Called when the circuit is CLOSED or HALF_OPEN. Returns `{result: "Service payment-api responded successfully", source: "live"}`. |
| **FallbackWorker** | `cb_fallback` | Returns cached/fallback data. Called when the circuit is OPEN. Returns `{result: "Fallback data for payment-api", source: "cache"}`. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
cb_check_circuit
 |
 v
SWITCH (circuit_switch_ref) on state:
 |-- "OPEN": cb_fallback --> returns cached data (source: "cache")
 |-- default: cb_call_service --> returns live data (source: "live")

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
