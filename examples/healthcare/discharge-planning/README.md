# Hospital Discharge Planning in Java Using Conductor : Readiness Assessment, Care Plan, Education, and Follow-Up Scheduling

## The Problem

You need to safely discharge patients from the hospital while preventing readmissions. Each discharge requires assessing whether the patient meets clinical readiness criteria (stable vitals, ambulatory status, pain management). A discharge plan must be created covering medications, activity restrictions, wound care, and post-acute services. Services like home health nursing, durable medical equipment, and prescription delivery must be coordinated. The patient and family need education on medication schedules, warning signs, and when to seek emergency care. Finally, follow-up appointments must be scheduled with the PCP and any specialists within the appropriate timeframe. A missed step. like failing to schedule follow-up or educate the patient on medication changes, directly increases 30-day readmission risk.

Without orchestration, you'd build a monolithic discharge application that checks readiness criteria, writes the plan to the EHR, calls home health agencies, generates education materials, and books follow-up appointments. If the home health referral API is down, you'd need retry logic. If the system crashes after creating the plan but before educating the patient, the patient leaves without understanding their care instructions. CMS penalizes hospitals for excessive readmissions under HRRP, making every discharge a compliance-critical process.

## The Solution

**You just write the discharge workers. Readiness assessment, care plan creation, service coordination, patient education, and follow-up scheduling. Conductor handles step dependencies, automatic retries when a service referral API is down, and complete discharge documentation for HRRP compliance.**

Each stage of the discharge process is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of assessing readiness before creating the plan, coordinating services based on the plan's requirements, delivering education before the patient leaves, scheduling follow-up as the final step, and maintaining a complete record of every discharge for quality reporting. ### What You Write: Workers

Five workers manage the discharge process: AssessReadinessWorker checks clinical criteria, CreateDischargePlanWorker builds the care plan, CoordinateWorker arranges post-acute services, EducateWorker delivers patient instructions, and ScheduleFollowupWorker books follow-up visits.

| Worker | Task | What It Does |
|---|---|---|
| **AssessReadinessWorker** | `dsc_assess_readiness` | Evaluates clinical discharge criteria. stable vitals, adequate mobility, pain control, safe home environment |
| **CreateDischargePlanWorker** | `dsc_create_plan` | Builds the discharge plan with medication reconciliation, activity restrictions, and post-acute service needs |
| **CoordinateWorker** | `dsc_coordinate` | Arranges post-discharge services. home health, DME delivery, pharmacy, transportation |
| **EducateWorker** | `dsc_educate` | Delivers patient/family education on medications, warning signs, dietary restrictions, and self-care instructions |
| **ScheduleFollowupWorker** | `dsc_schedule_followup` | Books follow-up appointments with PCP and specialists within the clinically appropriate timeframe |

the workflow and compliance logic stay the same.

### The Workflow

```
dsc_assess_readiness
 │
 ▼
dsc_create_plan
 │
 ▼
dsc_coordinate
 │
 ▼
dsc_educate
 │
 ▼
dsc_schedule_followup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
