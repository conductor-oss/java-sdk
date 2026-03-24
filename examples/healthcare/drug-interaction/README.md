# Drug Interaction

Drug Interaction: list medications, check pairs, flag conflicts, recommend alternatives

**Input:** `patientId`, `newMedication` | **Timeout:** 60s

## Pipeline

```
drg_list_medications
    │
drg_check_pairs
    │
drg_flag_conflicts
    │
drg_recommend_alternatives
```

## Workers

**CheckPairsWorker** (`drg_check_pairs`)

Reads `medications`, `newMedication`. Outputs `interactions`, `pairsChecked`.

**FlagConflictsWorker** (`drg_flag_conflicts`)

```java
.filter(i -> "major".equals(i.get("severity")) || "moderate".equals(i.get("severity")))
```

Reads `interactions`. Outputs `conflicts`, `conflictCount`.

**ListMedicationsWorker** (`drg_list_medications`)

Reads `patientId`. Outputs `medications`.

**RecommendAlternativesWorker** (`drg_recommend_alternatives`)

Reads `conflicts`, `newMedication`. Outputs `alternatives`, `reviewed`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
