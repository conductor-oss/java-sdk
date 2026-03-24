# Bid Management

Bid management: create, distribute, collect, evaluate, and award.

**Input:** `projectName`, `budget`, `vendors` | **Timeout:** 60s

## Pipeline

```
bid_create
    │
bid_distribute
    │
bid_collect
    │
bid_evaluate
    │
bid_award
```

## Workers

**AwardWorker** (`bid_award`)

Reads `bidId`, `winner`. Outputs `awardedTo`, `notified`.

**CollectWorker** (`bid_collect`)

Outputs `responses`, `responseCount`.

**CreateWorker** (`bid_create`)

Reads `budget`, `projectName`. Outputs `bidId`.

**DistributeWorker** (`bid_distribute`)

```java
int count = vendors != null ? vendors.size() : 0;
```

Reads `bidId`, `vendors`. Outputs `invitedCount`.

**EvaluateWorker** (`bid_evaluate`)

Reads `responses`. Outputs `winner`, `winningBid`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
