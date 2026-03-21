# Conditional Approval Routing in Java Using Conductor : Amount-Based Tier Classification and Multi-Level Approval Chains via SWITCH

Conditional approval routing. classifies a request amount into a tier (low/medium/high), then uses SWITCH to route to different approval chains: low-amount requests need only manager approval, medium requests need manager + director, and high-amount requests need manager + director + VP. Each approval level is a WAIT task pausing for that approver's human decision. ## The Problem

You need approval chains that vary based on the request amount. A $500 office supply purchase needs only a manager's sign-off. A $5,000 conference budget needs the manager and director. A $50,000 vendor contract needs manager, director, and VP. The system must classify the amount into a tier (low: under $1,000, medium: $1,000-$9,999, high: $10,000+), then route to the correct approval chain. each level pausing for a human decision before advancing to the next. If any approver rejects, the chain stops. Hardcoding these rules in if/else blocks makes them impossible to change without a code deploy, and there is no visibility into which tier a request was classified as or where it is in the approval chain.

Without orchestration, you'd build a monolithic approval system with nested conditionals. check the amount, send the first email, poll for a response, if approved check if more levels are needed, send the next email, and so on. If the system crashes between the manager's approval and the director's notification, the request is stuck with no one knowing it needs attention. Finance auditors need to see the exact approval chain each request went through, who approved at each level, and how long each level took.

## The Solution

**You just write the tier-classification and request-processing workers. Conductor handles the amount-based routing to the correct approval chain.**

The SWITCH task is the key pattern here. After the classify worker determines the tier, Conductor's SWITCH routes to the correct approval chain. low goes to a single WAIT, medium goes to two sequential WAITs, high goes to three sequential WAITs. Each WAIT pauses for a human approver at that level (manager, director, VP). After all approvals in the selected chain are complete, the process worker finalizes the request. Conductor takes care of routing to the correct chain based on tier, waiting at each approval level, tracking who approved and when at every level, and providing a complete audit trail showing the tier classification and every approval in the chain. ### What You Write: Workers

ClassifyWorker determines the spending tier, and ProcessWorker finalizes after all approvals complete. Neither manages the SWITCH routing or the chain of WAIT tasks for manager, director, and VP.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `car_classify` | Classifies the request amount into a tier. low (under $1,000, manager only), medium ($1,000-$9,999, manager + director), or high ($10,000+, manager + director + VP) |
| *SWITCH task* | `route_approval` | Routes to the correct approval chain based on the tier. low, medium, or high each maps to a different sequence of WAIT tasks | Built-in Conductor SWITCH, no worker needed |
| *WAIT tasks* | `mgr_*/dir_*/vp_*` | Each approval level pauses for the designated approver's decision. manager, director, or VP depending on the tier and level | Built-in Conductor WAIT, no worker needed |
| **ProcessWorker** | `car_process` | Finalizes the request after all required approvals are complete, recording the tier classification and approval chain results |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
car_classify
 │
 ▼
SWITCH (route_ref)
 ├── low: mgr_only_approval
 ├── medium: mgr_med_approval -> dir_med_approval
 ├── high: mgr_hi_approval -> dir_hi_approval -> vp_hi_approval
 │
 ▼
car_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
