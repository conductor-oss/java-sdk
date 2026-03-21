# Patient Intake in Java Using Conductor: Registration, Insurance Verification, Triage, and Provider Assignment

The patient filled out her name, date of birth, and insurance information on a clipboard in the waiting room. Then she gave the same information verbally to the nurse at the triage station. Then a registration clerk typed it into the EMR. except they entered the birth year as 1987 instead of 1978, which pulled the wrong insurance policy, which flagged her as uninsured, which delayed her triage assessment by 40 minutes while someone sorted it out. She came in with chest pain. Three handoffs, three chances for transcription error, and the data entry mistake followed her all the way to the provider assignment. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate patient intake end-to-end, registration, insurance verification, triage, and provider assignment, as a single automated pipeline where data flows once and each step depends on verified output from the last.

## The Problem

You need to process patient intake at a healthcare facility. When a patient arrives, they must be registered in the system with demographics, contact information, and initial vitals (blood pressure, heart rate, temperature, weight). Their insurance coverage must be verified to confirm active benefits and determine copay/coinsurance. A triage assessment must be performed based on the chief complaint to determine acuity level (ESI 1-5 in emergency settings, or urgency classification in clinic settings). Based on the triage outcome, the patient is assigned to the appropriate provider: a physician, nurse practitioner, or specialist. Each step must complete before the next can proceed, you cannot triage without registration data, and you cannot assign a provider without a triage level.

Without orchestration, you'd build a monolithic intake application that writes to the registration database, calls the insurance eligibility API, runs the triage assessment, and queries provider availability. If the insurance verification service is down, the entire intake process stalls. If the system crashes after registration but before triage, the patient is registered but sitting in the waiting room with no acuity level and no provider. EMTALA requires documentation of triage timing for emergency departments.

## The Solution

**You just write the intake workers. Patient registration, insurance verification, clinical triage, and provider assignment. Conductor handles step dependencies, automatic retries when the insurance eligibility API is down, and a complete intake timeline for EMTALA compliance.**

Each step of the intake process is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of registering before verifying insurance, triaging after registration, assigning providers based on triage acuity, retrying if the insurance eligibility system is temporarily unavailable, and maintaining a complete intake timeline for EMTALA and regulatory compliance. ### What You Write: Workers

Four workers handle the intake process: RegisterWorker captures demographics and vitals, VerifyInsuranceWorker confirms coverage, TriageWorker assigns acuity level, and AssignWorker routes the patient to the right provider.

| Worker | Task | What It Does |
|---|---|---|
| **RegisterWorker** | `pit_register` | Registers the patient with demographics, contact info, and initial vitals (BP, HR, temp, weight) |
| **VerifyInsuranceWorker** | `pit_verify_insurance` | Verifies active insurance coverage, benefit details, and patient financial responsibility (copay, deductible) |
| **TriageWorker** | `pit_triage` | Performs triage assessment based on chief complaint and vitals, assigns acuity level (ESI 1-5) |
| **AssignWorker** | `pit_assign` | Assigns the patient to an available provider based on triage acuity and provider panel capacity |

### The Workflow

```
Input -> AssignWorker -> RegisterWorker -> TriageWorker -> VerifyInsuranceWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
