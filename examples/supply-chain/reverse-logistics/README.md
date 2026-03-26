# Reverse Logistics

Reverse logistics: receive, inspect, switch on condition, and process.

**Input:** `returnId`, `product`, `reason` | **Timeout:** 60s

## Pipeline

```
rvl_receive_return
    │
rvl_inspect
    │
condition_switch [SWITCH]
  ├─ refurbish: rvl_refurbish
  ├─ recycle: rvl_recycle
  └─ default: rvl_dispose
    │
rvl_process
```

## Workers

**DisposeWorker** (`rvl_dispose`)

Reads `returnId`. Outputs `action`, `method`.

**InspectWorker** (`rvl_inspect`)

Reads `returnId`. Outputs `condition`, `damageLevel`.

**ProcessWorker** (`rvl_process`)

Reads `condition`, `returnId`. Outputs `action`, `completed`.

**ReceiveReturnWorker** (`rvl_receive_return`)

Reads `product`, `reason`, `returnId`. Outputs `received`.

**RecycleWorker** (`rvl_recycle`)

Reads `returnId`. Outputs `action`, `materialsRecovered`.

**RefurbishWorker** (`rvl_refurbish`)

Reads `returnId`. Outputs `action`, `resaleValue`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
