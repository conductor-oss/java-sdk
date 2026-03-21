# Drug Interaction Checking in Java Using Conductor : Medication List, Pairwise Checks, Conflict Flagging, and Alternative Recommendations

## The Problem

You need to check for drug-drug interactions when a new medication is prescribed. The patient's complete active medication list must be retrieved from the EHR. Every pair of the new medication against existing medications must be checked against an interaction database for contraindications, dose adjustments, and monitoring requirements. Clinically significant conflicts must be flagged with severity levels (minor, moderate, major, contraindicated). For flagged interactions, the system must recommend alternative medications in the same therapeutic class that do not conflict. A missed major interaction. like prescribing warfarin with a CYP2C9 inhibitor, can cause life-threatening adverse events.

Without orchestration, you'd build a monolithic prescribing safety service that queries the medication list, runs pairwise lookups against the interaction database, formats the conflict alerts, and generates alternatives. all in a single request. If the drug interaction database is temporarily unavailable, the prescriber gets no safety check at all. If the system crashes after finding conflicts but before presenting alternatives, the clinician sees a warning with no actionable guidance. Every interaction check must be logged for medication safety audits.

## The Solution

**You just write the drug safety workers. Medication list retrieval, pairwise interaction checks, conflict flagging, and alternative recommendation. Conductor handles sequential evaluation, automatic retries when the interaction database is unavailable, and logged records of every safety check for medication audits.**

Each stage of the interaction check is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of listing medications before checking pairs, flagging conflicts only after all pairs are evaluated, generating alternative recommendations only for flagged conflicts, and logging every check for medication safety audits. ### What You Write: Workers

Four workers form the drug safety pipeline: ListMedicationsWorker retrieves active medications, CheckPairsWorker evaluates pairwise interactions, FlagConflictsWorker categorizes severity, and RecommendAlternativesWorker suggests safer therapeutic options.

| Worker | Task | What It Does |
|---|---|---|
| **ListMedicationsWorker** | `drg_list_medications` | Retrieves the patient's complete active medication list (drug name, dose, frequency, route) |
| **CheckPairsWorker** | `drg_check_pairs` | Evaluates every pairwise combination of the new medication against existing medications for interactions |
| **FlagConflictsWorker** | `drg_flag_conflicts` | Categorizes detected interactions by clinical severity (minor, moderate, major, contraindicated) |
| **RecommendAlternativesWorker** | `drg_recommend_alternatives` | Suggests alternative medications in the same therapeutic class that avoid the flagged interactions |

the workflow and compliance logic stay the same.

### The Workflow

```
drg_list_medications
 │
 ▼
drg_check_pairs
 │
 ▼
drg_flag_conflicts
 │
 ▼
drg_recommend_alternatives

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
