# Mental Health Workflow in Java Using Conductor : Intake, Clinical Assessment, Treatment Planning, and Progress Tracking

## The Problem

You need to manage the clinical pathway for behavioral health patients. A referral comes in with a patient ID, referral reason, and assigned provider. The patient must go through intake. collecting demographics, insurance, presenting concerns, and safety screening. A clinical assessment must be performed using validated instruments (PHQ-9 for depression, GAD-7 for anxiety, AUDIT for substance use). Based on the assessment scores, an individualized treatment plan is created with therapy modality (CBT, DBT, EMDR), medication management if indicated, session frequency, and measurable goals. Progress must then be tracked against the treatment plan using outcome measures at each session. A missed safety screening or delayed treatment plan can result in harm to the patient and regulatory consequences.

Without orchestration, you'd build a monolithic behavioral health EHR module that collects intake data, administers assessments, generates the treatment plan, and logs progress notes. If the assessment scoring service is unavailable, intake stalls. If the system crashes after assessment but before creating the treatment plan, the clinician has scores but no plan to act on. 42 CFR Part 2 and state mental health parity laws require strict documentation of every clinical interaction.

## The Solution

**You just write the behavioral health workers. Patient intake, clinical assessment scoring, treatment planning, and progress tracking. Conductor handles clinical step ordering, automatic retries when the assessment scoring service is unavailable, and a 42 CFR Part 2-compliant audit trail.**

Each stage of the mental health workflow is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of completing intake before assessment, building the treatment plan only after assessment scores are available, activating progress tracking after the plan is in place, and maintaining a 42 CFR Part 2-compliant audit trail of every clinical interaction.

### What You Write: Workers

Four workers manage the behavioral health pathway: IntakeWorker collects demographics and safety screening, AssessWorker scores standardized instruments (PHQ-9, GAD-7), TreatmentPlanWorker builds an individualized plan, and TrackProgressWorker monitors therapeutic outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `mh_intake` | Collects patient demographics, insurance, presenting concerns, safety screening, and referral details |
| **AssessWorker** | `mh_assess` | Administers and scores standardized clinical assessments (PHQ-9, GAD-7, Columbia Suicide Severity) |
| **TreatmentPlanWorker** | `mh_treatment_plan` | Creates an individualized treatment plan with therapy modality, medication, session frequency, and goals |
| **TrackProgressWorker** | `mh_track_progress` | Tracks therapeutic progress using outcome measures against the treatment plan goals |

the workflow and compliance logic stay the same.

### The Workflow

```
mh_intake
 │
 ▼
mh_assess
 │
 ▼
mh_treatment_plan
 │
 ▼
mh_track_progress

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
