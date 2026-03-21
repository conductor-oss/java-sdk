# Agent Handoff in Java Using Conductor: Triage Customer Messages and Route to Specialist Agents

A customer writes "I was charged twice" and your first-line agent can't figure out whether that's billing or technical, so the customer gets bounced to tech support, who says "not my department," then back to billing, who asks the customer to repeat everything from scratch. Three handoffs, zero resolution, one furious customer. This example uses [Conductor](https://github.com/conductor-oss/conductor) to classify the message once with a triage worker, then route it through a `SWITCH` task to the right specialist (billing, technical, or general).; no if/else spaghetti, no misroutes, and a full audit trail of which agent handled every message.

## The Problem

A customer writes "I was charged twice for my subscription." That needs a billing specialist who can look up charges, initiate refunds, and send confirmation emails. If it goes to a general agent, the customer gets a generic response and has to repeat themselves. If it goes to tech support, the agent wastes time looking for a technical problem that doesn't exist.

Triage requires understanding the message content (billing keyword detection, error code recognition, general inquiry classification), assigning a confidence score and urgency level, then routing to the right specialist with that context attached. The billing agent needs the customer ID, the urgency level, and the triage notes. The tech agent needs the same metadata plus root-cause diagnostics. Without orchestration, you'd build nested if/else routing with each specialist hardcoded into the triage logic, making it impossible to add a new specialist without modifying the triage code.

## The Solution

**You write the triage logic and specialist handlers. Conductor handles routing, retries, and message tracking.**

`TriageWorker` classifies the customer message into a category (billing, technical, or general) with a deterministic confidence score based on keyword match count, urgency level (normal or high), and matched keyword list. Conductor's `SWITCH` task routes to the matching specialist: `BillingWorker` handles refund processing and dispute resolution, `TechWorker` diagnoses infrastructure and API issues with root-cause analysis, and `GeneralWorker` handles plan inquiries, cancellations, and everything else. Each specialist receives the customer ID, original message, triage notes, and urgency level, and produces a deterministic ticket ID, resolution, and action list based on the input. Conductor records which specialist handled each message and what resolution was provided.

### What You Write: Workers

The triage worker classifies incoming messages, and Conductor routes them to the appropriate specialist. Billing, technical, or general support.

| Worker | Task | What It Does |
|---|---|---|
| **TriageWorker** | `ah_triage` | Classifies message by keyword matching against technical (api, error, timeout, crash, bug, 500, latency, outage) and billing (bill, charge, invoice, refund, payment, subscription, overcharged, credit) keyword sets. Returns category, confidence (0.6-0.95 based on match count), urgency, and matched keywords. |
| **BillingWorker** | `ah_billing` | Routes billing issues by message content: refund requests get charge review and refund initiation; invoice requests get document retrieval; other billing inquiries get account review. Returns a deterministic ticket ID, resolution, and action list. High-urgency tickets are flagged for review. |
| **TechWorker** | `ah_tech` | Performs root-cause analysis by message content: rate limiting gets quota increase, timeouts get connection pool restart, 500 errors get deployment rollback, unclassified issues get escalation to engineering. Returns diagnostics with latency, error rate, and root cause. High-urgency tickets get P1 priority. |
| **GeneralWorker** | `ah_general` | Routes general inquiries by message content: plan/pricing questions get comparison details and upgrade recommendations, cancellation requests get retention callbacks, everything else gets a follow-up. |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
ah_triage
 |
 v
SWITCH (route_to_specialist_ref)
 |-- billing: ah_billing
 |-- technical: ah_tech
 +-- default: ah_general

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
