# Visa Processing

Visa processing: collect docs, validate, submit, track, receive.

**Input:** `applicantId`, `country`, `visaType` | **Timeout:** 60s

## Pipeline

```
vsp_collect
    │
vsp_validate
    │
vsp_submit
    │
vsp_track
    │
vsp_receive
```

## Workers

**CollectWorker** (`vsp_collect`)

Reads `applicantId`, `visaType`. Outputs `applicationId`, `documents`.

**ReceiveWorker** (`vsp_receive`)

Outputs `status`, `validFrom`, `validTo`.

**SubmitWorker** (`vsp_submit`)

Reads `country`. Outputs `submitted`, `referenceNumber`.

**TrackWorker** (`vsp_track`)

Reads `applicationId`. Outputs `phase`, `estimatedDate`.

**ValidateWorker** (`vsp_validate`)

Outputs `valid`, `issues`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
