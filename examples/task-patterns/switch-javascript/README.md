# Switch with JavaScript Evaluator

A SWITCH task uses JavaScript expressions to evaluate complex routing conditions based on `amount`, `customerType`, and `region`. VIP customers with high-value orders go to a concierge, EU orders get compliance processing, and everything else follows the standard path.

## Workflow

```
SWITCH(javascript expression)
  ├── "vip_concierge" ──> swjs_vip_concierge
  ├── "vip_standard" ──> swjs_vip_standard
  ├── "eu_handler" ──> swjs_eu_handler
  ├── "manual_review" ──> swjs_manual_review
  └── default ──> swjs_standard
                     │
               swjs_finalize
```

Workflow `switch_js_demo` accepts `amount`, `customerType`, and `region`. Times out after `60` seconds.

## Workers

**VipConciergeWorker** (`swjs_vip_concierge`) -- handles VIP high-value orders.

**VipStandardWorker** (`swjs_vip_standard`) -- handles VIP standard orders.

**EuHandlerWorker** (`swjs_eu_handler`) -- applies EU compliance processing.

**ManualReviewWorker** (`swjs_manual_review`) -- flags high-value orders for review.

**StandardWorker** (`swjs_standard`) -- standard order processing.

**FinalizeWorker** (`swjs_finalize`) -- finalizes all orders after routing.

## Tests

20 tests verify JavaScript expression evaluation for each routing combination and the finalize step.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
