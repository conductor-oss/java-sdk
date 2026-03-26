# Helpdesk Routing

Orchestrates helpdesk routing through a multi-stage Conductor workflow.

**Input:** `issueDescription`, `customerId` | **Timeout:** 60s

## Pipeline

```
hdr_classify
    │
hdr_route_switch [SWITCH]
  ├─ tier1: hdr_tier1
  ├─ tier2: hdr_tier2
  ├─ tier3: hdr_tier3
  └─ default: hdr_tier1
```

## Workers

**ClassifyWorker** (`hdr_classify`)

```java
if (desc.contains("outage") || desc.contains("security")) tier = "tier3";
```

Reads `description`. Outputs `tier`, `confidence`, `keywords`.

**Tier1Worker** (`hdr_tier1`)

Reads `customerId`. Outputs `handler`, `resolution`, `avgResponseMin`.

**Tier2Worker** (`hdr_tier2`)

Reads `customerId`. Outputs `handler`, `resolution`, `avgResponseMin`.

**Tier3Worker** (`hdr_tier3`)

Reads `customerId`. Outputs `handler`, `resolution`, `avgResponseMin`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
