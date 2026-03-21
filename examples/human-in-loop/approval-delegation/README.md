# Approval Delegation in Java Using Conductor : Request Preparation, WAIT for Approver, SWITCH to Delegate, and Finalization

Approval delegation. prepares a request, pauses at a WAIT task for the initial approver, then uses a SWITCH to handle delegation: if the approver responds with "delegate" and a delegateTo target, a second WAIT task pauses for the delegate's decision. Whether approved directly or via delegation, the workflow finalizes the result. ## The Problem

You need approvals where the assigned approver can delegate to someone else. A manager receives an approval request but is on vacation, overloaded, or not the right person. they need to reassign it to a colleague or their backup. The original WAIT task must accept three possible actions: approve (finalize immediately), reject (finalize immediately), or delegate (specify who should handle it, then wait for that person). The delegation must be tracked, who delegated to whom, when, and what the final decision was. Without a SWITCH after the WAIT, you cannot route delegation to a second WAIT task for the delegate's input.

Without orchestration, you'd build custom delegation logic. the approver clicks "delegate" in your UI, your backend updates a database record with the new assignee, and a polling loop watches for the delegate's response. If the system crashes after the delegation but before the delegate is notified, the request is stuck in limbo. There is no audit trail showing the delegation chain, and the original requester has no visibility into whether their request has been delegated or is still pending with the original approver.

## The Solution

**You just write the request-preparation and finalization workers. Conductor handles the approval hold, delegation routing, and the second wait for the delegate.**

The WAIT + SWITCH + WAIT pattern is the key here. After preparing the request, the workflow pauses at the first WAIT for the initial approver. The approver's response flows into a SWITCH. if the action is "delegate", the workflow enters a second WAIT task addressed to the delegate specified in the delegateTo field. Whether the approval comes from the original approver or the delegate, the finalize worker processes the outcome. Conductor takes care of holding state at each WAIT, routing delegation via SWITCH, tracking the complete delegation chain with timestamps, and finalizing regardless of which path was taken. ### What You Write: Workers

PrepareWorker sets up the approval request, and FinalizeWorker records who decided. Whether the original approver or their delegate, neither worker handles the delegation routing.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `ad_prepare` | Prepares the approval request data. validates the request, identifies the initial approver, and signals readiness for the first WAIT task |
| *WAIT task* | `initial_approval_wait` | Pauses for the initial approver's response. they can approve (action: "approve"), reject, or delegate (action: "delegate", delegateTo: "person") | Built-in Conductor WAIT, no worker needed |
| *SWITCH task* | `delegation_switch` | Routes based on the approver's action. "delegate" sends to a second WAIT task, other actions proceed to finalization | Built-in Conductor SWITCH, no worker needed |
| *WAIT task* | `delegated_approval_wait` | Pauses for the delegate's decision, with the delegateTo identity passed from the initial approver's response | Built-in Conductor WAIT. no worker needed |
| **FinalizeWorker** | `ad_finalize` | Finalizes the approval. records the decision, who made it (original approver or delegate), and completes the request |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
ad_prepare
 │
 ▼
initial_approval_wait [WAIT]
 │
 ▼
SWITCH (delegation_switch_ref)
 ├── delegate: delegated_approval_wait
 │
 ▼
ad_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
