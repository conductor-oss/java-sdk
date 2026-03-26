# Ordered Processing

A financial ledger requires that transactions for the same account are processed in strict sequence order. Events may arrive out of order due to network jitter. The pipeline must detect sequence gaps, buffer out-of-order events, process them in correct sequence, and flag gaps that exceed a timeout.

## Pipeline

```
[opr_receive]
     |
     v
[opr_sort_by_sequence]
     |
     v
[opr_process_in_order]
     |
     v
[opr_verify_order]
```

**Workflow inputs:** `messages`, `partitionKey`

## Workers

**OprProcessInOrderWorker** (task: `opr_process_in_order`)

- Writes `processedOrder`, `processedCount`

**OprReceiveWorker** (task: `opr_receive`)

- Writes `receivedMessages`, `count`

**OprSortBySequenceWorker** (task: `opr_sort_by_sequence`)

- Writes `sortedMessages`, `expectedOrder`

**OprVerifyOrderWorker** (task: `opr_verify_order`)

- Writes `orderCorrect`, `processedSequence`

---

**16 tests** | Workflow: `opr_ordered_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
