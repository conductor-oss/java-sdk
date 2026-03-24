# Care Coordination in Java Using Conductor : Needs Assessment, Care Planning, Team Assembly, and Patient Monitoring

## The Problem

You need to coordinate care for patients with chronic conditions or complex medical needs. When a patient is flagged for care coordination, their clinical condition and acuity must be assessed to determine what services they need. Based on that assessment, a care plan is created with specific goals, interventions, and timelines. The right care team must then be assembled. a primary care physician, relevant specialists, a care manager, and potentially a social worker or behavioral health provider. Finally, the patient must be enrolled in ongoing monitoring so the care team can track progress against the plan.

Without orchestration, you'd build a monolithic care management application that queries the patient's clinical records, runs the needs assessment, writes the care plan to the EHR, sends team assignment notifications, and activates monitoring. all in one service. If the EHR is temporarily unavailable, you'd need retry logic. If the system crashes after creating the care plan but before assigning the team, the patient has a plan but no one to execute it. CMS and NCQA require documentation of every coordination activity for quality measures and accreditation.

## The Solution

**You just write the care coordination workers. Needs assessment, care plan creation, team assembly, and patient monitoring activation. Conductor handles sequencing, automatic retries when an EHR endpoint is slow, and timestamped records for CMS quality reporting.**

Each stage of care coordination is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of assessing needs before building the plan, assembling the team only after the plan defines what specialties are needed, activating monitoring with the assigned team members, and maintaining a complete audit trail for quality reporting.

### What You Write: Workers

Four workers handle the coordination lifecycle: AssessNeedsWorker evaluates clinical needs, CreatePlanWorker builds the care plan, AssignTeamWorker assembles the multidisciplinary team, and MonitorWorker activates ongoing tracking.

| Worker | Task | What It Does |
|---|---|---|
| **AssessNeedsWorker** | `ccr_assess_needs` | Evaluates the patient's clinical condition and acuity level to determine required services and interventions |
| **CreatePlanWorker** | `ccr_create_plan` | Builds a personalized care plan with goals, interventions, and timelines based on the needs assessment |
| **AssignTeamWorker** | `ccr_assign_team` | Assembles the multidisciplinary care team (PCP, specialists, care manager) based on the care plan and acuity |
| **MonitorWorker** | `ccr_monitor` | Enrolls the patient in ongoing monitoring and links the care team for progress tracking |

the workflow and compliance logic stay the same.

### The Workflow

```
ccr_assess_needs
 │
 ▼
ccr_create_plan
 │
 ▼
ccr_assign_team
 │
 ▼
ccr_monitor

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
