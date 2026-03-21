# Returns Processing in Java Using Conductor : Receive, Inspect, Route Decision via SWITCH

## Return Outcomes Depend on Product Condition and Policy

A customer returns a pair of shoes they bought 10 days ago. The outcome depends on several factors: Is it within the 30-day return window? What's the product condition. unworn with tags (full refund eligible), worn but undamaged (store credit only), or damaged (rejection)? Is the return reason covered by policy (wrong size vs, changed mind vs, defective)?

The inspection step determines the condition, and the routing step maps condition + reason + policy to the right resolution. A defective item within warranty gets a full refund regardless of condition. A changed-mind return with a worn product gets store credit at best. Damaged items outside warranty get rejected. Each resolution path has different downstream steps (refund to original payment method, generate store credit code, or send rejection notification).

## The Solution

**You just write the return intake, inspection, and resolution (refund/exchange/reject) logic. Conductor handles refund retries, inspection routing, and return lifecycle audit trails.**

`ReceiveWorker` logs the return request with order ID, return reason, items, and customer ID, and generates a return merchandise authorization (RMA) number. `InspectWorker` evaluates the product condition. checking for damage, wear, missing components, and original packaging, and produces a condition grade. Conductor's `SWITCH` routes based on the inspection result and return policy: full refund (unworn, within window), exchange (wrong size/color), store credit (worn but acceptable), or rejection (damaged, outside window). Each resolution path processes the appropriate outcome. Conductor records the inspection results and routing decision for return analytics.

### What You Write: Workers

Return authorization, inspection, refund processing, and restocking workers handle each phase of a return without depending on the others' internals.

| Worker | Task | What It Does |
|---|---|---|
| **ReceiveReturnWorker** | `ret_receive` | Logs receipt of returned items at the warehouse, assigns a return ID |
| **InspectReturnWorker** | `ret_inspect` | Evaluates item condition and return reason, decides refund/exchange/reject |
| **RefundWorker** | `ret_refund` | Issues a refund to the customer's original payment method |
| **ExchangeWorker** | `ret_exchange` | Creates a replacement order and ships the exchange item |
| **RejectWorker** | `ret_reject` | Rejects the return and notifies the customer with the reason |

### The Workflow

```
ret_receive
 │
 ▼
ret_inspect
 │
 ▼
SWITCH (switch_ref)
 ├── refund: ret_refund
 ├── exchange: ret_exchange
 ├── reject: ret_reject

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
