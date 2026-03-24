# Offer Management

Offer management with SWITCH for accept/decline.

**Input:** `candidateName`, `position`, `salary`, `response` | **Timeout:** 60s

## Pipeline

```
ofm_generate
    │
ofm_approve
    │
ofm_send
    │
offer_response [SWITCH]
  ├─ accept: ofm_accept
  └─ decline: ofm_decline
```

## Workers

**AcceptWorker** (`ofm_accept`)

Reads `candidateName`. Outputs `accepted`, `startDate`.

**ApproveWorker** (`ofm_approve`)

Reads `offerId`. Outputs `approved`, `approvers`.

**DeclineWorker** (`ofm_decline`)

Reads `candidateName`. Outputs `declined`, `reason`.

**GenerateWorker** (`ofm_generate`)

Reads `candidateName`, `position`, `salary`. Outputs `offerId`, `generated`.

**SendWorker** (`ofm_send`)

Reads `candidateName`. Outputs `sent`, `expiresIn`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
