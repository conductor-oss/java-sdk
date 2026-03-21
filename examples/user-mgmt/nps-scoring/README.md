# NPS Scoring in Java Using Conductor

## The Problem

Your product team wants to measure user satisfaction after a quarterly release. The team needs to collect NPS survey responses (scores 0-10) from the user base, calculate the NPS score by categorizing respondents into promoters, passives, and detractors, segment users into actionable groups with tailored follow-up strategies, and trigger the appropriate actions for each segment (referral programs for promoters, engagement campaigns for passives, outreach calls for detractors). Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the survey-collection, NPS-calculation, segmentation, and follow-up workers. Conductor handles the scoring pipeline and segment-based action routing.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CollectResponsesWorker gathers survey scores, CalculateNpsWorker computes the promoter-passive-detractor breakdown, SegmentWorker groups respondents, and ActWorker triggers segment-specific follow-ups like referral programs or outreach calls.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `nps_act` | Triggers segment-specific actions: referral programs for promoters, engagement boosts for passives, outreach calls for detractors |
| **CalculateNpsWorker** | `nps_calculate` | Computes the NPS score from survey responses by counting promoters (9-10), passives (7-8), and detractors (0-6) |
| **CollectResponsesWorker** | `nps_collect_responses` | Collects NPS survey responses for a campaign and period, returning individual user scores |
| **SegmentWorker** | `nps_segment` | Segments respondents into promoter, passive, and detractor groups with assigned follow-up actions |

Replace with real identity provider and database calls and ### The Workflow

```
nps_collect_responses
 │
 ▼
nps_calculate
 │
 ▼
nps_segment
 │
 ▼
nps_act

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
