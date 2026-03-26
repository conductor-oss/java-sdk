# Lab Results

Lab results: collect, process, analyze, report, notify

**Input:** `orderId`, `patientId`, `testType` | **Timeout:** 60s

## Pipeline

```
lab_collect_sample
    │
lab_process
    │
lab_analyze
    │
lab_report
    │
lab_notify
```

## Workers

**AnalyzeSampleWorker** (`lab_analyze`)

**CollectSampleWorker** (`lab_collect_sample`)

**LabNotifyWorker** (`lab_notify`)

```java
String channel = critical ? "phone+portal" : "portal";
```

**LabReportWorker** (`lab_report`)

**ProcessSampleWorker** (`lab_process`)

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
