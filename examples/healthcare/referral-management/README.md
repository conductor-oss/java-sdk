# Referral Management in Java Using Conductor : Creation, Specialist Matching, Scheduling, Tracking, and Closure

## The Problem

You need to manage patient referrals from creation through specialist consultation. A primary care physician creates a referral specifying the patient, specialty needed, and clinical reason. The system must match the patient to an appropriate in-network specialist based on specialty, location, availability, and insurance acceptance. An appointment must be scheduled with the matched specialist. The referral must be tracked to ensure the patient actually sees the specialist and the consultation report is sent back to the referring provider. Finally, the referral is closed when the consultation is complete. Lost referrals. where patients never see the specialist or reports never reach the PCP, lead to gaps in care and potential liability.

Without orchestration, you'd build a monolithic referral system that creates the order, queries the provider directory, calls the scheduling API, monitors for completion, and archives the referral. If the provider directory is temporarily unavailable, the patient cannot be matched. If the system crashes after scheduling but before tracking begins, the referral falls through the cracks. Value-based care contracts and quality measures require referral loop closure rates for performance reporting.

## The Solution

**You just write the referral workers. Order creation, specialist matching, appointment scheduling, completion tracking, and loop closure. Conductor handles lifecycle sequencing, automatic retries when the provider directory is temporarily unavailable, and full referral loop visibility for quality reporting.**

Each stage of the referral lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of matching specialists only after the referral is created, scheduling only after a specialist is matched, tracking through completion, closing with the consultation report, and providing full visibility into every referral's status. ### What You Write: Workers

Five workers manage the referral lifecycle: CreateReferralWorker generates the order, MatchSpecialistWorker finds the best in-network provider, ScheduleReferralWorker books the appointment, TrackReferralWorker monitors completion, and CloseReferralWorker closes the loop with the consultation report.

| Worker | Task | What It Does |
|---|---|---|
| **CreateReferralWorker** | `ref_create` | Creates the referral order with patient, specialty, clinical reason, and referring provider |
| **MatchSpecialistWorker** | `ref_match_specialist` | Finds the best-match in-network specialist based on specialty, location, availability, and insurance |
| **ScheduleReferralWorker** | `ref_schedule` | Books the appointment with the matched specialist and notifies the patient |
| **TrackReferralWorker** | `ref_track` | Monitors the referral for appointment completion and consultation report receipt |
| **CloseReferralWorker** | `ref_close` | Closes the referral loop when the consultation report is received by the referring provider |

the workflow and compliance logic stay the same.

### The Workflow

```
ref_create
 │
 ▼
ref_match_specialist
 │
 ▼
ref_schedule
 │
 ▼
ref_track
 │
 ▼
ref_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
