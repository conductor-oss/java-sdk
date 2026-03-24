# Telemedicine Visit in Java Using Conductor : Scheduling, Video Connection, Clinical Consultation, e-Prescribing, and Follow-Up

## The Problem

You need to manage the full lifecycle of a telemedicine visit. A patient requests a virtual visit with a provider for a specific clinical reason. The appointment must be scheduled on both the patient's and provider's calendars. At the appointment time, a secure healthcare-pattern video connection must be established. The provider conducts the consultation. reviewing the chief complaint, taking history, and making an assessment. If medication is indicated, an e-prescription must be transmitted to the patient's preferred pharmacy. Finally, follow-up care must be arranged, a future appointment, lab orders, or referral to a specialist. Each step must complete before the next, you cannot consult without a connection, and you cannot prescribe without a consultation.

Without orchestration, you'd build a monolithic telehealth platform that manages scheduling, video sessions, clinical documentation, EPCS (electronic prescribing for controlled substances), and follow-up. all in one tightly coupled system. If the video platform has a brief outage, the entire visit flow fails. If the system crashes after the consultation but before the prescription is sent, the patient leaves the visit without their medication. Payers and state telehealth parity laws require documentation of the visit modality, duration, and clinical decision-making.

## The Solution

**You just write the telehealth workers. Visit scheduling, video connection, clinical consultation, e-prescribing, and follow-up arrangement. Conductor handles visit stage sequencing, automatic retries when the video platform has a brief outage, and a complete visit record for billing and telehealth parity compliance.**

Each stage of the telemedicine visit is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of scheduling before connecting, consulting only after the video session is established, prescribing based on the consultation findings, arranging follow-up as the final step, and maintaining a complete visit record for billing and compliance.

### What You Write: Workers

Five workers cover the virtual visit lifecycle: ScheduleWorker books the appointment, ConnectWorker establishes the secure video session, ConsultWorker records the clinical encounter, PrescribeWorker transmits e-prescriptions, and FollowUpWorker arranges post-visit care.

| Worker | Task | What It Does |
|---|---|---|
| **ScheduleWorker** | `tlm_schedule` | Books the virtual visit on both patient and provider calendars with visit link and instructions |
| **ConnectWorker** | `tlm_connect` | Establishes the secure, healthcare-pattern video connection between patient and provider |
| **ConsultWorker** | `tlm_consult` | Records the clinical consultation. chief complaint, history, assessment, and plan |
| **PrescribeWorker** | `tlm_prescribe` | Writes and transmits the e-prescription to the patient's pharmacy via Surescripts |
| **FollowUpWorker** | `tlm_followup` | Arranges follow-up care. future appointments, lab orders, referrals, or patient education |

the workflow and compliance logic stay the same.

### The Workflow

```
tlm_schedule
 │
 ▼
tlm_connect
 │
 ▼
tlm_consult
 │
 ▼
tlm_prescribe
 │
 ▼
tlm_followup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
