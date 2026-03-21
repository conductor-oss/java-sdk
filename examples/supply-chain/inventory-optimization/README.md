# Inventory Optimization in Java with Conductor : Stock Analysis, Reorder Point Calculation, Multi-SKU Optimization, and Replenishment Execution

## The Problem

You need to keep the right amount of inventory across multiple SKUs. Too much stock ties up working capital and warehouse space; too little causes stockouts and lost sales. For each SKU in WH-Central (widgets, gadgets, sensors, cables), you must analyze current on-hand quantities against consumption rates, calculate the reorder point where a new order must be placed to arrive before stock runs out, optimize order quantities across all SKUs to minimize total cost (ordering costs + holding costs + stockout penalties), and trigger purchase orders for items below their reorder threshold.

Without orchestration, inventory planners run the analysis in a spreadsheet, manually check each SKU against reorder thresholds, and create POs one at a time. The optimization step (which considers all SKUs together for volume discounts and warehouse capacity) is skipped because it requires data from the analysis step that isn't easily passed between tools. If the replenishment order fails to submit, the SKU sits below reorder point until someone notices days later.

## The Solution

**You just write the inventory workers. Stock analysis, reorder calculation, multi-SKU optimization, and replenishment execution. Conductor handles pipeline sequencing, ERP retry logic, and recorded optimization decisions for supply chain analytics.**

Each stage of the optimization pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so stock analysis feeds reorder calculation, reorder points feed the multi-SKU optimizer, and optimization results drive replenishment execution. If the ERP integration fails when placing a purchase order, Conductor retries without re-running the analysis. Every stock snapshot, reorder calculation, optimization decision, and order execution is recorded for supply chain analytics and audit.

### What You Write: Workers

Four workers optimize inventory across SKUs: AnalyzeStockWorker reads current levels, CalculateReorderWorker sets reorder points, OptimizeWorker minimizes total cost across all items, and ExecuteWorker triggers replenishment purchase orders.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeStockWorker** | `io_analyze_stock` | Analyzes current on-hand quantities and consumption rates across all SKUs. |
| **CalculateReorderWorker** | `io_calculate_reorder` | Calculates the reorder point for each SKU based on demand velocity and lead time. |
| **ExecuteWorker** | `io_execute` | Triggers replenishment purchase orders for items below their reorder threshold. |
| **OptimizeWorker** | `io_optimize` | Optimizes order quantities across all SKUs to minimize total cost (ordering + holding + stockout). |

### The Workflow

```
io_analyze_stock
 │
 ▼
io_calculate_reorder
 │
 ▼
io_optimize
 │
 ▼
io_execute

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
