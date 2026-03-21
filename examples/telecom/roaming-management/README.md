# Roaming Management in Java Using Conductor

## Why Roaming Management Needs Orchestration

Managing roaming events requires a pipeline that spans two independent carrier networks. You detect roaming when a subscriber registers on a visited network. their device attaches to a foreign PLMN and the visited network sends a location update to the home network. You validate that a roaming agreement exists between the home and visited networks, checking that the agreement is active, covers the subscriber's service types, and has not exceeded volume caps. You rate the roaming usage by applying the tariffs defined in the inter-carrier agreement, which differ from the subscriber's domestic plan. You bill the subscriber by adding roaming charges to their account. Finally, you settle the inter-carrier amount between the home and visited operators.

If billing succeeds but settlement fails, the home operator has charged the subscriber but hasn't paid the visited operator. creating a financial discrepancy that compounds across millions of roaming events. If agreement validation discovers no active agreement, the subscriber should be barred from the visited network before usage accumulates unbillable charges. Without orchestration, you'd build a batch process that collects TAP files weekly, manually reconciles rates, and generates settlement invoices in spreadsheets, making it impossible to handle near-real-time roaming events, detect agreement violations before they accumulate, or audit which tariff was applied to which roaming session.

## The Solution

**You just write the roaming detection, agreement validation, usage rating, subscriber billing, and inter-carrier settlement logic. Conductor handles session validation retries, charge calculations, and roaming settlement audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Partner agreement lookup, session validation, charge calculation, and settlement workers each handle one aspect of cross-network roaming.

| Worker | Task | What It Does |
|---|---|---|
| **BillWorker** | `rmg_bill` | Bills the subscriber by adding roaming charges to their account based on rated usage. |
| **DetectRoamingWorker** | `rmg_detect_roaming` | Detects a roaming event when a subscriber connects to a visited network and captures the usage data. |
| **RateWorker** | `rmg_rate` | Rates roaming usage by applying the inter-carrier agreement's tariff schedule to the captured usage. |
| **SettleWorker** | `rmg_settle` | Settles the inter-carrier payment between the home and visited operators for the roaming usage. |
| **ValidateAgreementWorker** | `rmg_validate_agreement` | Validates that an active roaming agreement exists between the home and visited networks. |

### The Workflow

```
rmg_detect_roaming
 │
 ▼
rmg_validate_agreement
 │
 ▼
rmg_rate
 │
 ▼
rmg_bill
 │
 ▼
rmg_settle

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
