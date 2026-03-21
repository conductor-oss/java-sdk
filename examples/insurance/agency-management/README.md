# Agency Management in Java with Conductor : Onboard, License, Assign Territory, Track Performance, Review

## Insurance Agent Management Has Regulatory Requirements

Bringing on a new insurance agent requires onboarding (background check, E&O insurance verification, appointment paperwork), licensing verification (state license active, lines of authority match, continuing education current), territory assignment (geographic boundaries, product lines, no overlap with existing agents), performance tracking (premium volume, policy count, loss ratio, retention rate), and periodic review (compensation adjustments, territory changes, license renewals).

Licensing is the critical compliance step. an agent selling without a valid license exposes the insurer to regulatory penalties and coverage disputes. License status must be verified at onboarding and monitored continuously. Territory assignment must avoid conflicts with existing agents while ensuring market coverage.

## The Solution

**You just write the agent onboarding, license verification, territory assignment, performance tracking, and review logic. Conductor handles licensing retries, territory assignment, and agency lifecycle audit trails.**

`OnboardWorker` processes the new agent application. background checks, E&O insurance verification, and carrier appointment paperwork. `LicenseWorker` verifies state insurance licenses, active status, correct lines of authority, continuing education compliance. `AssignTerritoryWorker` assigns the agent's sales territory, geographic boundaries, eligible product lines, and commission schedules. `TrackWorker` monitors ongoing performance, premium volume, policy count, loss ratio, customer retention, and complaint rate. `ReviewWorker` conducts periodic performance reviews with compensation adjustments and territory modifications. Conductor tracks the complete agent lifecycle for regulatory compliance.

### What You Write: Workers

Agent onboarding, licensing verification, territory assignment, and performance tracking workers each manage one aspect of the agency relationship.

| Worker | Task | What It Does |
|---|---|---|
| **OnboardWorker** | `agm_onboard` | Onboards the new agent. processes the application, verifies E&O insurance, runs background checks, and returns the onboarded status and start date |
| **LicenseWorker** | `agm_license` | Verifies the agent's insurance license. checks the state license status, lines of authority, and continuing education compliance, returning the license number and expiration date |
| **AssignTerritoryWorker** | `agm_assign_territory` | Assigns the agent's sales territory. determines geographic boundaries, eligible product lines, and commission schedules based on the agent's state and qualifications |
| **TrackWorker** | `agm_track` | Monitors agent performance. tracks premium volume, policy count (12 policies written), loss ratio, and retention rate within the assigned territory |
| **ReviewWorker** | `agm_review` | Conducts the performance review. evaluates the agent's tracked performance metrics against territory benchmarks and assigns a rating (exceeds expectations) |

### The Workflow

```
agm_onboard
 │
 ▼
agm_license
 │
 ▼
agm_assign_territory
 │
 ▼
agm_track
 │
 ▼
agm_review

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
