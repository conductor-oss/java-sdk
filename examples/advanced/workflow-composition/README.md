# Workflow Composition in Java Using Conductor : Compose Sub-Workflows into a Unified Order Pipeline

## Complex Processes Are Made of Simpler Ones

Fulfilling a customer order requires two independent processes: payment processing (validate card, charge amount) and inventory management (check stock, reserve items). Each process has its own steps, failure modes, and retry logic. Building them as a single monolithic workflow means a payment retry can block inventory reservation, and a stock check failure can prevent a valid payment from proceeding.

Workflow composition lets you build each sub-process independently, test it in isolation, and then compose them into a larger workflow. The order pipeline runs payment steps (sub-workflow A: validate then charge) and inventory steps (sub-workflow B: check then reserve), then merges the results to produce the final order status.

## The Solution

**You write each sub-workflow's steps. Conductor handles composition, cross-workflow retries, and result merging.**

`WcpSubAStep1Worker` and `WcpSubAStep2Worker` handle the first sub-workflow (e.g., payment validation and charging). `WcpSubBStep1Worker` and `WcpSubBStep2Worker` handle the second sub-workflow (e.g., inventory check and reservation). `WcpMergeWorker` combines the results from both sub-workflows into a unified order status. Conductor sequences the sub-workflows and their merge, recording the full lineage of both processes and their combined outcome.

### What You Write: Workers

Five workers span two sub-workflows. Order validation and processing in sub-workflow A, customer lookup and enrichment in sub-workflow B, plus a merge step that unifies both outcomes.### The Workflow

```
wcp_sub_a_step1
 │
 ▼
wcp_sub_a_step2
 │
 ▼
wcp_sub_b_step1
 │
 ▼
wcp_sub_b_step2
 │
 ▼
wcp_merge

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
