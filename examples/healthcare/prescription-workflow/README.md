# Prescription Workflow in Java Using Conductor : Verification, Interaction Checking, Filling, Dispensing, and Adherence Tracking

## The Problem

You need to process prescriptions from the point a provider writes the order through dispensing and ongoing adherence monitoring. The prescription must first be verified. confirming the prescriber's DEA number, the patient's identity, and the medication's formulary status. The patient's current medication list must be pulled and the new drug checked for interactions, contraindications, and duplicate therapy. Once cleared, the prescription is filled, the correct medication, strength, and quantity are prepared. The filled prescription is dispensed to the patient with counseling instructions. Finally, the prescription must be tracked for refill timing and adherence (medication possession ratio). A missed interaction check or dispensing error can cause serious patient harm.

Without orchestration, you'd build a monolithic pharmacy system that validates the Rx, queries the interaction database, updates the fill record, prints the label, and schedules refill reminders. If the interaction database is temporarily unavailable, the pharmacist cannot verify safety. If the system crashes after filling but before dispensing, the medication is prepared but the patient record is not updated. State pharmacy boards and DEA require complete dispensing records for every controlled substance.

## The Solution

**You just write the prescription workers. Rx verification, interaction checking, filling, dispensing, and adherence tracking. Conductor handles strict safety sequencing, automatic retries when the interaction database is unavailable, and complete dispensing records for pharmacy board and DEA compliance.**

Each stage of the prescription lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of verifying before checking interactions, filling only after safety checks pass, dispensing only after filling is complete, activating adherence tracking as the final step, and maintaining a complete dispensing record for pharmacy board and DEA compliance.

### What You Write: Workers

Five workers manage the prescription lifecycle: VerifyWorker validates prescriber credentials and formulary status, CheckInteractionsWorker screens for drug-drug conflicts, FillWorker prepares the medication, DispenseWorker records the dispensing event, and TrackWorker monitors refill adherence.

| Worker | Task | What It Does |
|---|---|---|
| **VerifyWorker** | `prx_verify` | Validates the prescription (prescriber credentials, patient identity, formulary status) and retrieves current medications |
| **CheckInteractionsWorker** | `prx_check_interactions` | Checks the new medication against the patient's current medications for drug-drug interactions and contraindications |
| **FillWorker** | `prx_fill` | Fills the prescription. selects the correct NDC, verifies strength and quantity, generates the label |
| **DispenseWorker** | `prx_dispense` | Records the dispensing event, updates the patient's medication profile, and documents counseling |
| **TrackWorker** | `prx_track` | Monitors refill timing, calculates medication possession ratio (MPR), and schedules refill reminders |

the workflow and compliance logic stay the same.

### The Workflow

```
Input -> CheckInteractionsWorker -> DispenseWorker -> FillWorker -> TrackWorker -> VerifyWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
