# Exclusive Join in Java with Conductor

EXCLUSIVE_JOIN demo. query three vendors in parallel, wait for all responses, then select the best offer by lowest price and fastest response time.

## The Problem

You need to get price quotes from multiple vendors simultaneously and pick the best offer. A product query goes out to Vendor A, Vendor B, and Vendor C at the same time. Each vendor responds with a price and response time. After all three respond, you need to compare their offers and select the winner. lowest price wins, with fastest response time as the tiebreaker. The entire comparison must happen only after every vendor has replied, not as responses trickle in.

Without orchestration, you'd spawn three threads or async HTTP calls, manage a countdown latch or barrier to wait for all responses, handle the case where one vendor times out while the others succeed, and write comparison logic that runs only after the barrier releases. If the process crashes after two vendors respond, you lose those responses and have to re-query all three. There is no record of what each vendor quoted or why a particular vendor was selected.

## The Solution

**You just write the vendor query and best-offer selection workers. Conductor handles the parallel execution, join, and per-vendor retry.**

This example demonstrates Conductor's FORK_JOIN pattern for parallel vendor quoting. Three vendor workers (VendorA, VendorB, VendorC) run concurrently, each querying their respective pricing API with the same product query. A JOIN task waits until all three vendors have responded, then the SelectBestWorker compares all offers side by side. selecting the vendor with the lowest price, using response time as a tiebreaker when prices match. Conductor tracks each vendor's quote independently, so if Vendor B's API times out, Conductor retries just that branch while the other quotes remain safely stored.

### What You Write: Workers

Four workers implement the competitive quoting pattern: three vendor workers (A, B, C) query prices in parallel, and SelectBestWorker compares all offers to pick the lowest price with response time as a tiebreaker.

| Worker | Task | What It Does |
|---|---|---|
| **SelectBestWorker** | `ej_select_best` | Selects the best vendor from the three parallel vendor responses. Picks the vendor with the lowest price. If prices a... |
| **VendorAWorker** | `ej_vendor_a` | Simulates Vendor A responding to a product query. Returns deterministic price and response time data. |
| **VendorBWorker** | `ej_vendor_b` | Simulates Vendor B responding to a product query. Returns deterministic price and response time data. Vendor B has th... |
| **VendorCWorker** | `ej_vendor_c` | Simulates Vendor C responding to a product query. Returns deterministic price and response time data. Vendor C has th... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
FORK_JOIN
 ├── ej_vendor_a
 ├── ej_vendor_b
 └── ej_vendor_c
 │
 ▼
JOIN (wait for all branches)
ej_select_best

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
