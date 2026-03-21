# Circuit Breaker Microservice in Java with Conductor

Circuit breaker pattern for resilient service calls. ## The Problem

When a downstream service is failing, continuing to send requests wastes resources and can cascade failures to the caller. The circuit breaker pattern tracks failure counts and, when a threshold is exceeded, short-circuits requests to a fallback response until the downstream recovers. This workflow checks circuit state, routes to either a live call or a fallback, and records the result to update the circuit's failure counter.

Without orchestration, circuit breaker logic is embedded in each calling service using libraries like Resilience4j or Hystrix. There is no centralized view of which circuits are open, no audit trail of when circuits tripped, and changing thresholds requires redeploying each caller.

## The Solution

**You just write the circuit-check, service-call, fallback, and result-recording workers. Conductor handles conditional routing between call and fallback paths, failure tracking, and automatic retries.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers implement the circuit breaker: CheckCircuitWorker reads the circuit state, CallServiceWorker makes the downstream call when the circuit is closed, FallbackWorker returns a degraded response when it is open, and RecordResultWorker updates the failure counter.

| Worker | Task | What It Does |
|---|---|---|
| **CallServiceWorker** | `cb_call_service` | Makes the actual call to the downstream service when the circuit is closed. |
| **CheckCircuitWorker** | `cb_check_circuit` | Checks the circuit state (closed/open/half-open) for the target service and returns failure count and threshold. |
| **FallbackWorker** | `cb_fallback` | Returns a cached or degraded response when the circuit is open. |
| **RecordResultWorker** | `cb_record_result` | Records the call outcome (success/failure) to update the circuit's failure counter. |

the workflow coordination stays the same.

### The Workflow

```
cb_check_circuit
 │
 ▼
SWITCH (switch_ref)
 ├── open: cb_fallback
 └── default: cb_call_service -> cb_record_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
