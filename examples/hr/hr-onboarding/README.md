# Employee Onboarding in Java with Conductor : Profile Creation, System Provisioning, Mentor Assignment, and Training Plan

## The Problem

You need to onboard a new employee across multiple departments and systems. When a hire is confirmed, the HR team must create the employee's profile with their name, department, and start date. IT must provision a laptop, create email and Slack accounts, and grant access to Jira and other department-specific tools. A mentor from the same department must be assigned to guide the new hire through their first weeks. Finally, a training plan must be generated with required compliance courses, department-specific skills training, and scheduled check-ins. Each step depends on the previous one. IT cannot provision accounts without the employee profile, and the training plan needs to know who the mentor is.

Without orchestration, you'd coordinate all of this through emails, tickets, and spreadsheets. HR creates the profile, emails IT for provisioning, messages a team lead to assign a mentor, and creates a training document. If IT provisioning is delayed, the new hire shows up on day one with no laptop or email. If a step is forgotten, the employee misses mandatory compliance training or never gets a mentor. HR has no single view of where each onboarding stands.

## The Solution

**You just write the profile creation, system provisioning, mentor assignment, and training plan generation logic. Conductor handles provisioning retries, onboarding step sequencing, and new-hire audit trails.**

Each stage of onboarding is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of creating the profile before provisioning, provisioning before mentor assignment, building the training plan with all prior context, retrying if any system (Active Directory, Slack API) is temporarily unavailable, and giving HR complete visibility into every onboarding's status.

### What You Write: Workers

Profile creation, system provisioning, mentor assignment, and training plan workers each automate one phase of bringing a new employee into the organization.

| Worker | Task | What It Does |
|---|---|---|
| **CreateProfileWorker** | `hro_create_profile` | Creates the employee profile in the HRIS with name, department, role, and start date |
| **ProvisionWorker** | `hro_provision` | Provisions IT systems. laptop order, email account, Slack workspace, Jira access, and VPN credentials |
| **AssignMentorWorker** | `hro_assign_mentor` | Selects and assigns a mentor from the same department based on availability and experience |
| **TrainingWorker** | `hro_training` | Generates a personalized training plan with required compliance courses and department-specific skills |

### The Workflow

```
hro_create_profile
 │
 ▼
hro_provision
 │
 ▼
hro_assign_mentor
 │
 ▼
hro_training

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
