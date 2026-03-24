# Recruitment Pipeline in Java with Conductor : Job Posting, Resume Screening, Interview, Evaluation, and Offer

## The Problem

You need to manage the hiring pipeline from job posting through offer letter. A hiring manager opens a requisition for a role in their department. The job must be posted to job boards and the company careers page. As applications arrive, each candidate's resume must be screened against the role's requirements. years of experience, required skills, education, producing a screening score (e.g., 85/100). Candidates who pass screening move to interviews, where structured questions produce an interview score. Both scores feed into a composite evaluation that generates a hire/no-hire recommendation. Top candidates receive an offer with compensation details. Each step depends on the previous, you cannot interview without screening, and you cannot make an offer without a composite evaluation.

Without orchestration, you'd build a monolithic ATS (applicant tracking system) that posts jobs, parses resumes, tracks interview feedback in spreadsheets, and generates offer letters manually. If the job board API is temporarily down when posting, you'd need retry logic. If the system crashes after the interview but before the evaluation is recorded, the candidate is left in limbo with no decision. Recruiters have no single dashboard showing where each candidate stands in the pipeline, and EEOC compliance requires documenting the objective criteria used at each stage.

## The Solution

**You just write the job posting, resume screening, interview coordination, scoring, and offer extension logic. Conductor handles screening retries, candidate routing, and hiring funnel audit trails.**

Each stage of the recruitment pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of screening only after the job is posted, interviewing only after screening passes, evaluating using both the screening score and interview score, extending the offer only after a positive recommendation, retrying if job boards or HRIS systems are temporarily unavailable, and maintaining a complete audit trail for EEOC and OFCCP compliance.

### What You Write: Workers

Job posting, application screening, shortlisting, and offer extension workers handle talent acquisition as a multi-stage funnel.

| Worker | Task | What It Does |
|---|---|---|
| **PostWorker** | `rcp_post` | Posts the job requisition to job boards and the careers page with title, department, and requirements, returning a job ID |
| **ScreenWorker** | `rcp_screen` | Screens the candidate's resume against the job requirements, scoring qualifications on a 0-100 scale and returning a pass/fail decision |
| **InterviewWorker** | `rcp_interview` | Records structured interview results with competency ratings and an overall interview score |
| **EvaluateWorker** | `rcp_evaluate` | Combines the screening score and interview score into a composite evaluation with a hire/no-hire recommendation |
| **OfferWorker** | `rcp_offer` | Generates and extends the offer to the recommended candidate with compensation, start date, and terms |

### The Workflow

```
rcp_post
 │
 ▼
rcp_screen
 │
 ▼
rcp_interview
 │
 ▼
rcp_evaluate
 │
 ▼
rcp_offer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
