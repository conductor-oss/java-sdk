# Offer Management in Java with Conductor : Generation, Approval, Delivery, and Accept/Decline Routing

## The Problem

You need to manage job offers from generation through candidate response. After the hiring team selects a candidate, an offer letter must be generated with the position title, base salary, equity package, benefits summary, and start date. The offer must be approved by the compensation committee or hiring VP to ensure it falls within the approved salary band and headcount budget. Once approved, the offer is sent to the candidate with an expiration deadline. The candidate either accepts. triggering the onboarding pipeline, background check, and equipment provisioning, or declines, reopening the requisition and notifying the recruiter to extend the offer to the next candidate in the pipeline. If the system sends an offer before approval, the company may commit to an unauthorized salary. If a decline is not handled, the requisition sits idle and the role goes unfilled.

Without orchestration, you'd manage offers through email threads and spreadsheets. the recruiter drafts the letter, emails the VP for approval, manually sends the offer, and watches for a reply. If the candidate doesn't respond, the recruiter must remember to follow up before the deadline. If the system crashes after approval but before the offer is sent, the approved offer never reaches the candidate. There is no audit trail showing who approved what amount or when the offer was extended.

## The Solution

**You just write the offer generation, compensation approval, candidate delivery, and accept/decline routing logic. Conductor handles approval routing, offer generation retries, and compensation audit trails.**

Each stage of offer management is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of generating the offer before seeking approval, sending only after approval is granted, routing the candidate's response via SWITCH to the correct accept or decline path, retrying if the HRIS or e-signature platform is temporarily unavailable, and maintaining a complete audit trail of every offer's lifecycle.

### What You Write: Workers

Compensation calculation, offer letter generation, approval routing, and candidate notification workers each handle one step of extending a job offer.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `ofm_generate` | Creates the offer letter with candidate name, position title, salary, equity, benefits, and start date, returning an offer ID |
| **ApproveWorker** | `ofm_approve` | Routes the offer for compensation committee or VP approval, verifying the salary falls within the approved band for the role and level |
| **SendWorker** | `ofm_send` | Delivers the approved offer letter to the candidate via email or e-signature platform with an acceptance deadline |
| **AcceptWorker** | `ofm_accept` | Processes the candidate's acceptance. marks the requisition filled, triggers onboarding, and notifies the hiring team |
| **DeclineWorker** | `ofm_decline` | Processes the candidate's decline. reopens the requisition, notifies the recruiter, and flags the next candidate in the pipeline |

### The Workflow

```
ofm_generate
 │
 ▼
ofm_approve
 │
 ▼
ofm_send
 │
 ▼
SWITCH (ofm_switch_ref)
 ├── accept: ofm_accept
 ├── decline: ofm_decline

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
