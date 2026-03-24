# Quality Inspection

Quality inspection: sample, test, switch on pass/fail, and record results.

**Input:** `batchId`, `product`, `sampleSize` | **Timeout:** 60s

## Pipeline

```
qi_sample
    │
qi_test
    │
result_switch [SWITCH]
  ├─ pass: qi_handle_pass
  └─ default: qi_handle_fail
    │
qi_record
```

## Workers

**HandleFailWorker** (`qi_handle_fail`)

Reads `batchId`, `defects`. Outputs `action`, `approved`.

**HandlePassWorker** (`qi_handle_pass`)

Reads `batchId`. Outputs `action`, `approved`.

**RecordWorker** (`qi_record`)

Reads `batchId`, `product`, `result`. Outputs `recorded`.

**SampleWorker** (`qi_sample`)

```java
.mapToObj(i -> Map.<String, Object>of("id", "S-" + i, "unit", i))
```

Reads `batchId`, `sampleSize`. Outputs `samples`, `count`.

**TestWorker** (`qi_test`)

```java
double defectRate = samples.isEmpty() ? 0 : (double) defects / samples.size();
String testResult = defectRate < 0.05 ? "pass" : "fail";
```

Reads `samples`. Outputs `result`, `defects`, `defectRate`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
