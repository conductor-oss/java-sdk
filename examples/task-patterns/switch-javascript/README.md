# Switch Javascript in Java with Conductor

SWITCH with JavaScript evaluator for complex routing based on amount, customerType, and region.

## The Problem

You need to route order processing based on multiple criteria simultaneously. not just a single field. VIP customers with high-value orders (amount > $1,000) get white-glove concierge service. VIP customers with standard orders get priority processing. Non-VIP orders over $5,000 require manual review for fraud screening. EU region orders need compliance processing (VAT, GDPR). Everything else goes through standard processing. A simple value-param SWITCH can only match on one field, but this routing logic depends on amount AND customerType AND region evaluated together.

Without orchestration, you'd write nested if/else conditions: `if (customerType == "vip" && amount > 1000)` then concierge, `else if (customerType == "vip")` then priority, etc. Adding a new routing rule means modifying deeply nested conditionals and retesting every path. There is no record of which evaluation path was taken for a given order, making it impossible to audit why an order was or was not flagged for review.

## The Solution

**You just write the per-route processing and finalization workers. Conductor handles the JavaScript-based multi-criteria evaluation and branch routing.**

This example demonstrates Conductor's SWITCH task with a JavaScript evaluator. multi-criteria routing logic evaluated server-side. The JavaScript expression takes amount, customerType, and region as inputs and returns a case label: `vip_high` (VIP + amount > $1,000), `vip_standard` (VIP, any amount), `needs_review` (amount > $5,000, non-VIP), `eu_processing` (EU region), or `standard` (everything else). Each case routes to a dedicated worker. VipConciergeWorker handles white-glove VIP orders, ManualReviewWorker flags high-value orders for fraud screening, EuHandlerWorker handles EU compliance, and StandardWorker processes normal orders. After the SWITCH resolves, FinalizeWorker runs regardless of branch. Conductor records the JavaScript evaluation result, so you can see exactly which routing decision was made and why.

### What You Write: Workers

Five workers handle the multi-criteria routing outcomes: VipConciergeWorker for high-value VIP orders, VipPriorityWorker for standard VIP orders, ManualReviewWorker for fraud screening, EuHandlerWorker for EU compliance, StandardWorker for normal processing, and FinalizeWorker for the common completion step.

| Worker | Task | What It Does |
|---|---|---|
| **EuHandlerWorker** | `swjs_eu_handler` | Handles orders from the EU region for compliance processing. |
| **FinalizeWorker** | `swjs_finalize` | Common finalization step that runs after the SWITCH regardless of which branch was taken. |
| **ManualReviewWorker** | `swjs_manual_review` | Handles high-value orders that require manual review (amount > 5000, non-VIP). |
| **StandardWorker** | `swjs_standard` | Default handler for orders that don't match any special criteria. |
| **VipConciergeWorker** | `swjs_vip_concierge` | Handles VIP customers with high-value orders (amount > 1000). |
| **VipStandardWorker** | `swjs_vip_standard` | Handles VIP customers with standard-value orders. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_order_ref)
 ├── vip_high: swjs_vip_concierge
 ├── vip_standard: swjs_vip_standard
 ├── needs_review: swjs_manual_review
 ├── eu_processing: swjs_eu_handler
 └── default: swjs_standard
 │
 ▼
swjs_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
