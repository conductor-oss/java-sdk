# Prescription Workflow

Prescription processing: verify, check interactions, fill, dispense, track adherence

**Input:** `prescriptionId`, `patientId`, `medication`, `dosage` | **Timeout:** 1800s

## Pipeline

```
prx_verify
    │
prx_check_interactions
    │
prx_fill
    │
prx_dispense
    │
prx_track
```

## Workers

**CheckInteractionsWorker** (`prx_check_interactions`): Checks for drug interactions between new medication and current medications.

Reads `currentMedications`, `medication`. Outputs `cleared`, `interactions`, `severity`.

**DispenseWorker** (`prx_dispense`): Dispenses the filled prescription to the patient.

Reads `fillId`, `patientId`. Outputs `dispensed`, `dispensedAt`, `pharmacist`.

**FillWorker** (`prx_fill`): Fills the prescription order.

Reads `dosage`, `medication`. Outputs `fillId`, `quantity`, `daysSupply`.

**TrackWorker** (`prx_track`): Tracks the prescription for refills and adherence monitoring.

Reads `prescriptionId`. Outputs `trackingId`, `refillDate`, `adherenceMonitoring`.

**VerifyWorker** (`prx_verify`): Verifies a prescription and retrieves current medications.

Reads `dosage`, `medication`, `prescriptionId`. Outputs `verified`, `prescriber`, `currentMedications`.

## Tests

**21 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
