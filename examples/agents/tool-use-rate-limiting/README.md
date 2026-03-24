# Tool Use Rate Limiting: Quota Check, Queue, and Delayed Execution

An application calls a translation API that enforces a quota of 100 requests per time window. When the quota is nearly exhausted (98 of 100 used), sending another request risks a hard rejection. The system needs to check the remaining quota, decide whether to execute immediately or queue the request, and retry after the window resets.

This workflow checks the API rate limit, then uses a SWITCH task to either execute immediately (if allowed) or queue the request and execute after a delay.

## Pipeline Architecture

```
toolName, toolArgs
       |
       v
rl_check_rate_limit      (quotaUsed=98, quotaLimit=100, decision)
       |
       v
SWITCH on decision
    |              |
 allowed       throttled (default)
    |              |
    v              v
rl_execute      rl_queue_request
_tool              |
                   v
                rl_delayed_execute
```

## Worker: CheckRateLimit (`rl_check_rate_limit`)

Reads the tool name and API key, then evaluates the quota. In this demonstration, `quotaUsed=98` and `quotaLimit=100`, leaving `quotaRemaining=2`. The decision rule is: if `quotaRemaining <= 2`, set `decision: "throttled"`; otherwise `"allowed"`. Returns `retryAfterMs: 5000`, `queuePosition: 3`, and `windowResetAt: "2026-03-08T10:01:00Z"`.

## Worker: ExecuteTool (`rl_execute_tool`)

Activated when quota allows. Returns a translation result: `"Bonjour, comment allez-vous aujourd'hui?"` with `sourceLanguage: "en"`, `targetLanguage: "fr"`, `confidence: 0.98`. Sets `executedImmediately: true` and `executionTimeMs: 180`.

## Worker: QueueRequest (`rl_queue_request`)

Queues the throttled request. Returns `queueId: "q-fixed-001"`, the `queuePosition` from the rate limit check, `estimatedWaitMs` (set to `retryAfterMs`), `queuedAt` timestamp, and `status: "queued"`.

## Worker: DelayedExecute (`rl_delayed_execute`)

Executes the previously queued request after the rate limit window resets. Returns the same translation result as the immediate path but with `executedImmediately: false`, `delayedByMs: 5000`, and the `queueId` for correlation. Reports `executionTimeMs: 195`.

## Tests

4 tests cover rate limit checking, immediate execution, request queuing, and delayed execution.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
