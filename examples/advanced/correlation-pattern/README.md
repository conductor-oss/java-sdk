# Correlation Pattern

A distributed system sends a request to an external service and receives the response asynchronously on a different channel. The correlation pattern matches the response to the original request using a correlation ID, handles timeouts when no response arrives, and ensures that late responses do not corrupt the workflow state.

## Pipeline

```
[crp_receive_messages]
     |
     v
[crp_match_by_id]
     |
     v
[crp_aggregate]
     |
     v
[crp_process]
```

**Workflow inputs:** `messages`, `correlationField`

## Workers

**CrpAggregateWorker** (task: `crp_aggregate`)

- Writes `aggregatedResults`

**CrpMatchByIdWorker** (task: `crp_match_by_id`)

- Writes `correlatedGroups`, `groupCount`

**CrpProcessWorker** (task: `crp_process`)

- Writes `processedCount`, `outcomes`

**CrpReceiveMessagesWorker** (task: `crp_receive_messages`)

- Writes `messages`, `totalMessages`

---

**16 tests** | Workflow: `crp_correlation_pattern` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
