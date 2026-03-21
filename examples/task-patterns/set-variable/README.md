# Set Variable in Java with Conductor

Demonstrates SET_VARIABLE system task for storing intermediate state accessible via ${workflow.variables.key} ## The Problem

You need to accumulate state across multiple workflow steps. processing a list of items to compute a total amount and category, then applying business rules based on those intermediate results (does this order need approval? what is the risk level?), and finally producing a decision that uses variables from both steps. The rules step needs the total amount and category from the processing step. The finalize step needs the original totals plus the approval and risk results. Intermediate state must be accessible from any downstream task without threading it through every task's input/output mapping.

Without orchestration, you'd store intermediate values in instance variables, a shared map, or a database, threading state manually between method calls. If the process crashes between computing the total and applying rules, the intermediate state is lost. There is no way to inspect what the accumulated state looked like at any point in the execution without adding custom logging at every step.

## The Solution

**You just write the item processing, rules evaluation, and finalization workers. Conductor handles the workflow variable storage and cross-step state sharing.**

This example demonstrates Conductor's SET_VARIABLE system task for storing intermediate state in workflow variables accessible via `${workflow.variables.key}`. ProcessItemsWorker computes totalAmount, itemCount, and category from the input items. A SET_VARIABLE task stores those results as workflow variables. ApplyRulesWorker reads the stored variables (not the task output directly) and applies business rules. orders over $500 need approval, high-value orders are high risk. A second SET_VARIABLE stores the rule results (needsApproval, riskLevel). FinalizeWorker reads all accumulated variables (totals + rules) to produce the final decision. Workflow variables persist across the entire execution and are visible in the Conductor UI, making intermediate state inspectable at any point.

### What You Write: Workers

Three workers build the order processing pipeline: ProcessItemsWorker computes totals and category, ApplyRulesWorker evaluates approval and risk thresholds from stored variables, and FinalizeWorker assembles the final decision using accumulated state from all previous steps.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyRulesWorker** | `sv_apply_rules` | Applies business rules based on intermediate state stored in workflow variables. Rules: - needsApproval: totalAmount ... |
| **FinalizeWorker** | `sv_finalize` | Produces the final decision string from all accumulated workflow variables. |
| **ProcessItemsWorker** | `sv_process_items` | Processes a list of items: computes total amount, count, and category. Category rules: - high-value: total >= 1000 - ... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
sv_process_items
 │
 ▼
store_item_results [SET_VARIABLE]
 │
 ▼
sv_apply_rules
 │
 ▼
store_rule_results [SET_VARIABLE]
 │
 ▼
sv_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
