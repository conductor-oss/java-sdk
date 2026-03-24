# Supplier Evaluation

Supplier evaluation: collect data, score, rank, and report.

**Input:** `category`, `period` | **Timeout:** 60s

## Pipeline

```
spe_collect_data
    │
spe_score
    │
spe_rank
    │
spe_report
```

## Workers

**CollectDataWorker** (`spe_collect_data`)

Reads `category`. Outputs `suppliers`, `supplierCount`.

**RankWorker** (`spe_rank`)

```java
String top = rankings.isEmpty() ? "none" : (String) rankings.get(0).get("name");
```

Reads `scores`. Outputs `rankings`, `topSupplier`.

**ReportWorker** (`spe_report`)

```java
int count = rankings != null ? rankings.size() : 0;
```

Reads `rankings`. Outputs `report`.

**ScoreWorker** (`spe_score`)

Reads `suppliers`. Outputs `scores`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
