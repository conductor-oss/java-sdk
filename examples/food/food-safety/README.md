# Food Safety in Java with Conductor

Conducts a food safety inspection: visiting the facility, checking temperatures, verifying hygiene, issuing certification, and recording results. ## The Problem

You need to conduct a food safety inspection at a restaurant. An inspector visits the facility, checks temperature logs for food storage and cooking equipment, verifies hygiene practices (handwashing, cross-contamination prevention, pest control), issues a safety certification if standards are met, and records the inspection results. Certifying without thorough temperature and hygiene checks puts public health at risk.

Without orchestration, you'd manage inspections with paper checklists and spreadsheets. manually tracking which restaurants are due for inspection, recording findings in documents, issuing certificates through a separate system, and maintaining records for health department audits.

## The Solution

**You just write the facility inspection, temperature checks, hygiene verification, and certification issuance logic. Conductor handles inspection scheduling retries, compliance routing, and safety audit trails.**

Each food safety concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (inspect, check temps, verify hygiene, certify, record), tracking every inspection with timestamped evidence, and resuming from the last step if the process crashes. ### What You Write: Workers

Inspection scheduling, temperature logging, compliance checking, and reporting workers each enforce one layer of food safety protocols.

| Worker | Task | What It Does |
|---|---|---|
| **CertifyWorker** | `fsf_certify` | Issues a safety certification with grade, score, and validity date when all checks pass |
| **CheckTempsWorker** | `fsf_check_temps` | Checks fridge, freezer, and hot-holding temperatures at the facility and returns pass/fail for each |
| **InspectWorker** | `fsf_inspect` | Dispatches the inspector to the restaurant and records initial findings for cleanliness, equipment, and violations |
| **RecordWorker** | `fsf_record` | Records inspection results and compliance status in the food safety database |
| **VerifyHygieneWorker** | `fsf_verify_hygiene` | Verifies hygiene practices (handwashing, surface sanitation, cross-contamination prevention) and returns a score |

### The Workflow

```
fsf_inspect
 │
 ▼
fsf_check_temps
 │
 ▼
fsf_verify_hygiene
 │
 ▼
fsf_certify
 │
 ▼
fsf_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
