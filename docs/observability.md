# Metrics and logging

**Audience:** operators diagnosing throughput, latency, errors, and cost.  
**Works with:** OSS and Orkes.

Use the Conductor UI/API for execution-level state and the SDK metrics module for client-side telemetry. Link logs, traces, and business records with workflow ID, task ID, correlation ID, and a redacted business identifier.

## What to measure

- Workflow starts, completions, failures, and end-to-end latency.
- Task queue age, poll rate, execution latency, timeout count, and retry count.
- Worker thread saturation and downstream dependency error rate.
- Agent model tokens, tool calls, time spent waiting for human input, and terminal status.

The [`conductor-client-metrics`](../conductor-client-metrics/README.md) module provides the SDK-side metrics integration. Do not label metrics with unbounded IDs, prompts, user email addresses, or secrets.

## Logs

Log state transitions and failure reasons at the worker boundary. Preserve the workflow and task IDs so an operator can open the exact execution. Redact request bodies by default and log only the fields necessary to reproduce a problem.

Next: [debugging incidents](debugging.md) and [deployment and scaling](deployment-scaling.md).
