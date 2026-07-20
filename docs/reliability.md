# Reliability: timeouts, retries, idempotency, and domains

**Audience:** production workflow and worker owners.  
**Works with:** OSS and Orkes.

Conductor delivers tasks at least once. A worker must tolerate retries, timeouts, and process replacement without duplicating business effects.

## Set the three task timeouts

Set `pollTimeoutSeconds` to detect a task no worker claims, `responseTimeoutSeconds` to recover a task after a worker disappears, and `timeoutSeconds` to bound total execution. Set a workflow timeout as a separate guardrail. Choose values from observed latency plus realistic failure recovery, not defaults.

## Retry only safe work

Configure retry count, backoff, and delay on task definitions. Use `FAILED_WITH_TERMINAL_ERROR` for invalid input or an irreversible business rejection; allow transient network or provider failures to use the retry policy. Design each external action with an idempotency key or a durable application record before enabling retries.

## Isolate workers with domains

Use task domains for dedicated worker pools, regions, tenants, or high-risk integrations. A domain is a routing boundary, not an authorization boundary; still use scoped credentials and network controls.

## Verify

Exercise success, transient failure, terminal failure, duplicate delivery, and timeout paths in an integration test. The maintained [workflow test harness](workflow-testing.md) runs against a real local server.

Next: [deployment and scaling](deployment-scaling.md), [security](security.md), and [debugging](debugging.md).
