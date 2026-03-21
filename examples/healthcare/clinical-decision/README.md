# Clinical Decision Support in Java Using Conductor : Data Gathering, Guideline Application, Risk Scoring, and Treatment Recommendations

## The Problem

You need to provide real-time clinical decision support at the point of care. When a clinician is treating a patient, the system must pull the patient's clinical history (labs, vitals, medications, diagnoses), apply evidence-based clinical guidelines for the presenting condition, compute a risk score (e.g., Framingham for cardiovascular risk, CURB-65 for pneumonia severity), and generate specific treatment recommendations. Each step depends on the previous one. you cannot apply guidelines without the patient's data, and you cannot recommend treatments without a risk score.

Without orchestration, you'd build a monolithic CDS engine that queries the EHR, runs the guideline rules engine, calculates the score, and formats the recommendation. all in a single request handler. If the EHR data service is slow, the entire recommendation is delayed. If new guidelines are published, you'd need to update and redeploy the entire system. Regulatory requirements demand logging of every recommendation generated for patient safety audits.

## The Solution

**You just write the clinical decision support workers. Patient data gathering, guideline application, risk scoring, and treatment recommendation. Conductor handles data flow between stages, retries when the EHR data service is slow, and regulatory logging of every recommendation.**

Each stage of the clinical decision support pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of gathering data before applying guidelines, feeding guideline outputs into the risk scorer, generating recommendations only after scoring is complete, and logging every step for regulatory compliance. ### What You Write: Workers

Four workers form the CDS pipeline: GatherDataWorker pulls clinical history, ApplyGuidelinesWorker evaluates evidence-based criteria, ScoreRiskWorker computes risk scores, and RecommendWorker generates treatment recommendations.

| Worker | Task | What It Does |
|---|---|---|
| **GatherDataWorker** | `cds_gather_data` | Pulls the patient's clinical data. labs, vitals, medications, diagnoses, and history, for the given condition |
| **ApplyGuidelinesWorker** | `cds_apply_guidelines` | Evaluates the patient data against evidence-based clinical guidelines (e.g., AHA, USPSTF, NICE) |
| **ScoreRiskWorker** | `cds_score_risk` | Computes a clinical risk score (Framingham, ASCVD, Wells, CURB-65) based on the guideline-filtered data |
| **RecommendWorker** | `cds_recommend` | Generates specific treatment recommendations (medications, procedures, follow-up) based on the risk score |

the workflow and compliance logic stay the same.

### The Workflow

```
cds_gather_data
 │
 ▼
cds_apply_guidelines
 │
 ▼
cds_score_risk
 │
 ▼
cds_recommend

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
