# Exception Handling

Auto-Process or Human Review Based on Risk

**Input:** `risk`

## Pipeline

```
eh_analyze
    │
route_switch [SWITCH]
  ├─ human_review: human_review_wait
  └─ default: eh_auto_process
    │
eh_finalize
```

## Workers

**AnalyzeWorker** (`eh_analyze`): Worker for eh_analyze — evaluates risk score and routes accordingly.

```java
String route = risk > 7 ? "human_review" : "auto_process";
```

Reads `risk`. Outputs `route`, `risk`.

**AutoProcessWorker** (`eh_auto_process`): Worker for eh_auto_process — handles low-risk items automatically.

Outputs `processed`.

**FinalizeWorker** (`eh_finalize`): Worker for eh_finalize — finalizes the workflow after processing.

Outputs `finalized`.

## Tests

**17 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
