# Triage and Route Customer Messages to Specialist Agents

A customer message arrives and must be classified, then handed off to the right specialist. The triage worker scans for keywords (refund/overcharged/charged-twice -> billing; rate/limit/throttl -> technical) with a confidence score based on matched keyword count. A SWITCH routes to billing, tech, or general agents. Each specialist generates ticket numbers via `Math.abs(customerId.hashCode() % 9000) + 1000`.

## Workflow

```
customerId, message
       │
       ▼
┌──────────────┐
│ ah_triage    │  Keyword-based classification + confidence
└──────┬───────┘
       ▼
  SWITCH (route_to_specialist)
  ├── "billing": ah_billing
  ├── "technical": ah_tech
  └── default: ah_general
```

## Workers

**TriageWorker** (`ah_triage`) -- Scans for keyword matches; confidence is deterministic from match count.

**BillingWorker** (`ah_billing`) -- Handles refund/overcharge/double-charge messages. Generates ticket numbers.

**TechWorker** (`ah_tech`) -- Handles rate-limit/throttling messages.

**GeneralWorker** (`ah_general`) -- Handles plan/upgrade/pricing and everything else.

## Tests

59 tests extensively cover triage classification, all three specialist routes, and ticket generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
