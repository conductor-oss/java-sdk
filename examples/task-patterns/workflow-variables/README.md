# Workflow Variables in Java with Conductor

Shows how variables and expressions work across tasks. ## The Problem

You need to accumulate state across multiple tasks in an order pricing pipeline. Calculate the subtotal from items, apply a tier-based discount, compute shipping costs, and build a final summary. Each task depends on results from earlier tasks and the original workflow input. Workflow variables and expressions let you reference any task's output from any subsequent task using `${task_ref.output.field}` syntax.

Without workflow variables, you'd pass all accumulated state through every worker's input/output, or store intermediate results in an external database. Workflow variables keep intermediate state inside the workflow execution itself, visible and inspectable at every step.

## The Solution

**You just write the pricing, shipping, and summary workers. Conductor handles wiring each task's output into subsequent tasks via variable expressions.**

This example builds an order pricing pipeline where each task's output feeds into subsequent tasks via workflow variable expressions. CalcPriceWorker sums the items' `price * qty` to compute a subtotal. An INLINE system task (`apply_tier_discount`) applies tier-based discounts using JavaScript: gold tier gets 20% off, silver 10%, bronze 5%, reading the subtotal from `${calc_price_ref.output.subtotal}` and the tier from `${workflow.input.customerTier}`. CalcShippingWorker computes shipping cost based on the discounted subtotal and item count (free shipping for gold tier or orders over $100). Finally, BuildSummaryWorker assembles a complete order summary by pulling fields from all three preceding tasks and the original workflow input, demonstrating multi-source data composition across the full pipeline.

### What You Write: Workers

Three workers form an order pricing pipeline connected by variable expressions: CalcPriceWorker computes the subtotal from line items, CalcShippingWorker determines shipping cost based on tier and subtotal, and BuildSummaryWorker assembles the final order summary from all preceding task outputs.

| Worker | Task | What It Does |
|---|---|---|
| **BuildSummaryWorker** | `wv_build_summary` | Builds a final order summary by pulling data from workflow input and outputs of all preceding tasks. Demonstrates how... |
| **CalcPriceWorker** | `wv_calc_price` | Calculates the base price (subtotal) from a list of order items. Each item has a name, price, and qty. Returns the su... |
| **CalcShippingWorker** | `wv_calc_shipping` | Calculates shipping cost based on the discounted subtotal, item count, and customer tier. Gold tier or orders over $1... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
wv_calc_price
 │
 ▼
apply_tier_discount [INLINE]
 │
 ▼
wv_calc_shipping
 │
 ▼
wv_build_summary

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
