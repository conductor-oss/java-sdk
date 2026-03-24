# A File-Backed Circuit Breaker State Machine in Java

Your `order-api` starts returning 500s. Within seconds, every upstream service is hammering it with retries, each waiting for a 30-second timeout. The downstream service cannot recover because it never gets breathing room. Meanwhile, your users see cascading latency spikes across unrelated features. This is the thundering herd problem, and it is solved by stopping the calls before they happen.

This example implements a full circuit breaker state machine -- with file-backed persistence, concurrent-safe failure counters, and Conductor SWITCH-based routing -- so that a failing dependency gets automatic protection without any code changes to callers.

## How the State Machine Works

```
                    failureCount >= threshold
  CLOSED ──────────────────────────────────────> OPEN
    ^                                              |
    |  success                     cooldown expires |
    |                                              v
    +──────────── HALF_OPEN <──────────────────────+
                  (test one     failure? back to OPEN
                   request)     success? back to CLOSED
```

The `CircuitBreakerState` class manages transitions with two concurrent data structures:

```java
private static final ConcurrentHashMap<String, String> STATE_CACHE = new ConcurrentHashMap<>();
private static final ConcurrentHashMap<String, AtomicInteger> FAILURE_COUNTS = new ConcurrentHashMap<>();
```

State is persisted to `java.io.tmpdir/circuit-breaker-state/{service}.state` on every write. Each file stores the state name and a timestamp, separated by newline. If the in-memory cache is cold (e.g., after a JVM restart), `getState()` loads from disk:

```java
Path stateFile = STATE_DIR.resolve(sanitize(serviceName) + ".state");
if (Files.exists(stateFile)) {
    String content = Files.readString(stateFile).trim();
    String state = content.split("\n")[0];
    STATE_CACHE.put(serviceName, state);
    return state;
}
```

File persistence is best-effort. The in-memory `ConcurrentHashMap` is the primary store; disk is the recovery mechanism.

## Workflow Orchestration

```
cb_check_circuit  (evaluate state from failure count + threshold)
       |
       v
  SWITCH on state:
       |── OPEN ──> cb_fallback  (return cached data, source: "cache")
       |── default ──> cb_call_service  (call real service, source: "live")
```

The workflow accepts `serviceName`, `failureCount`, `threshold`, and an optional `circuitState` override. The SWITCH task routes entirely based on `check_circuit_ref.output.state`.

## Worker: CheckCircuitWorker (`cb_check_circuit`)

Resolves the circuit state through a priority chain:

1. If `circuitState` input is `"OPEN"` -- force OPEN (manual override)
2. If `circuitState` input is `"HALF_OPEN"` -- force HALF_OPEN (cooldown test)
3. If `failureCount >= threshold` -- transition to OPEN
4. Otherwise -- remain CLOSED

The threshold defaults to 3 and must be positive. Negative failure counts are rejected as terminal errors. Every evaluation persists the resolved state via `CircuitBreakerState.setState()`.

## Worker: CallServiceWorker (`cb_call_service`)

On success, resets the failure counter to zero via `CircuitBreakerState.resetFailureCount()`. On failure (controlled by a `shouldFail` flag), atomically increments the counter:

```java
int newCount = CircuitBreakerState.incrementFailureCount(serviceName);
```

The `incrementAndGet()` on `AtomicInteger` inside `ConcurrentHashMap.computeIfAbsent()` guarantees thread safety even when 10 concurrent threads are hitting the same service name.

## Worker: FallbackWorker (`cb_fallback`)

Returns `{result: "Fallback data for {service}", source: "cache"}`. Called only when the circuit is OPEN, protecting the downstream service from additional load.

## Error Handling

| Condition | Status | Behavior |
|---|---|---|
| Missing `serviceName` on any worker | `FAILED_WITH_TERMINAL_ERROR` | No retry -- caller must fix input |
| `threshold <= 0` | `FAILED_WITH_TERMINAL_ERROR` | Invalid configuration |
| `failureCount < 0` | `FAILED_WITH_TERMINAL_ERROR` | Invalid state data |
| Service call failure | `FAILED_WITH_TERMINAL_ERROR` + counter increment | Failure count persisted for next evaluation |
| File I/O error on state persist | Warning to stderr | In-memory cache continues operating |

## Test Coverage

The test suite contains 4 test classes with 27 tests across three categories:

**State transitions (7 integration tests):** Closed-to-live path, open-to-fallback path, HALF_OPEN recovery on success, HALF_OPEN re-open on failure, threshold tripping after 3 failures, state file persistence, and concurrent access with 10 threads using `CountDownLatch` for synchronized start.

**CheckCircuitWorker (15 unit tests):** Forced OPEN/HALF_OPEN overrides, threshold boundary (at, above, below), default values (threshold=3, failureCount=0), string-to-int coercion for failure count, negative threshold/failureCount rejection, and disk persistence verification.

**CallServiceWorker (7 unit tests):** Successful response format, failure counter increment across 3 calls, counter reset on success, missing serviceName rejection.

**FallbackWorker (4 unit tests):** Cache source tagging, missing serviceName rejection, output field completeness.

## Configuration

| Parameter | Default | Description |
|---|---|---|
| `threshold` | 3 | Failures before circuit opens |
| `failureCount` | 0 | Current accumulated failures |
| `circuitState` | (none) | Manual override: `OPEN` or `HALF_OPEN` |
| State directory | `java.io.tmpdir/circuit-breaker-state/` | File-backed persistence location |

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
