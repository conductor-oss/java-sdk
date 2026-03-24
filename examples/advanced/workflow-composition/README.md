# Workflow Composition

A complex business process is built from simpler, reusable sub-workflows. The order-processing workflow calls the payment sub-workflow, the inventory sub-workflow, and the shipping sub-workflow. Each sub-workflow is independently testable, and the parent handles coordination and error propagation.

## Pipeline

```
[wcp_sub_a_step1]
     |
     v
[wcp_sub_a_step2]
     |
     v
[wcp_sub_b_step1]
     |
     v
[wcp_sub_b_step2]
     |
     v
[wcp_merge]
```

**Workflow inputs:** `orderId`, `customerId`

## Workers

**WcpMergeWorker** (task: `wcp_merge`)

- Writes `composedResult`, `orderProcessed`, `customerEnriched`

**WcpSubAStep1Worker** (task: `wcp_sub_a_step1`)

- Writes `validated`, `orderId`

**WcpSubAStep2Worker** (task: `wcp_sub_a_step2`)

- Sets `result` = `"order_processed"`
- Writes `result`, `total`

**WcpSubBStep1Worker** (task: `wcp_sub_b_step1`)

- Writes `profile`

**WcpSubBStep2Worker** (task: `wcp_sub_b_step2`)

- Sets `result` = `"customer_enriched"`
- Writes `result`, `discount`

---

**20 tests** | Workflow: `workflow_composition_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
