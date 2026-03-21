# Helpdesk Routing in Java with Conductor : Tier-Based Ticket Routing via SWITCH

## Getting Tickets to the Right Team on the First Try

Customers expect their support tickets to reach someone who can actually help. Routing a complex technical issue to Tier 1 wastes the customer's time with a handoff. Routing a simple password reset to Tier 3 wastes engineer time. The classification needs to happen automatically and the routing needs to be deterministic. the right tier every time, based on issue complexity.

This workflow classifies the ticket first, then routes it to the correct tier using a Conductor SWITCH task. The classifier analyzes the issue description and customer context to determine which tier should handle it. The SWITCH task reads the tier assignment and routes to `hdr_tier1` (general support for common questions), `hdr_tier2` (technical support for product issues), or `hdr_tier3` (engineering escalation for critical problems). If the classification does not match any defined tier, the default case routes to Tier 1.

## The Solution

**You just write the ticket-classification and tier-specific handler workers. Conductor handles the SWITCH-based routing to the correct support tier.**

Four workers handle the routing. one classifier and three tier-specific handlers. The classifier determines the appropriate tier based on issue complexity and customer context. The SWITCH task makes routing declarative: Conductor reads the tier from the classifier's output and sends the ticket to the matching handler. Each tier handler processes tickets differently. Tier 1 uses knowledge base lookups, Tier 2 investigates technical details, Tier 3 engages engineering directly.

### What You Write: Workers

ClassifyWorker determines the support tier, then the SWITCH routes to Tier1Worker for common questions, Tier2Worker for technical issues, or Tier3Worker for critical escalations, each handler focuses on its specialty.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `hdr_classify` | Analyzes the issue description and customer context to determine which support tier should handle it. |
| **Tier1Worker** | `hdr_tier1` | Handles basic support: common questions, password resets, and knowledge base lookups. |
| **Tier2Worker** | `hdr_tier2` | Handles technical support: product issues, configuration problems, and debugging. |
| **Tier3Worker** | `hdr_tier3` | Handles engineering escalations: critical problems requiring senior engineer investigation. |

### The Workflow

```
hdr_classify
 │
 ▼
SWITCH (hdr_switch_ref)
 ├── tier1: hdr_tier1
 ├── tier2: hdr_tier2
 ├── tier3: hdr_tier3
 └── default: hdr_tier1

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
