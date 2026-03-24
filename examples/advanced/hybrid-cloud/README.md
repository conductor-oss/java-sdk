# Hybrid Cloud Data Routing in Java Using Conductor : Classify Sensitivity, Route to On-Prem or Cloud

## Sensitive Data Can't Leave Your Data Center

Regulations like GDPR, HIPAA, and PCI-DSS require that certain data. patient records, financial transactions, PII, stays within controlled environments. But running everything on-premises wastes cloud elasticity for workloads that have no compliance constraints. The challenge is automatically determining which data must stay on-prem and which can be processed in the cloud, then routing each record to the right infrastructure.

Without automated classification and routing, engineers make manual decisions about where data should go, or worse, everything runs in one place. either overpaying for on-prem infrastructure for non-sensitive workloads, or risking compliance violations by sending sensitive data to the cloud.

## The Solution

**You write the classification and processing logic. Conductor handles the sensitivity-based routing, retries, and compliance audit trail.**

`HybClassifyDataWorker` examines the data type and content to determine its sensitivity classification and target environment. `onprem` for sensitive data that must stay in the data center, `cloud` for everything else. A `SWITCH` task routes based on this classification: `HybProcessOnpremWorker` handles sensitive records within the on-premises environment, while `HybProcessCloudWorker` sends non-sensitive records to AWS for elastic processing. Conductor's conditional routing makes this classification-based split declarative, and every execution records the data ID, classification, and which path was taken, giving you an audit trail for compliance.

### What You Write: Workers

Three workers handle the classification-and-routing split. Sensitivity classification, on-premises processing for regulated data, and cloud processing for non-sensitive workloads.

| Worker | Task | What It Does |
|---|---|---|
| **HybClassifyDataWorker** | `hyb_classify_data` | Classifies data by sensitivity (e.g., PII vs. general) and determines whether to route to on-prem or cloud |
| **HybProcessCloudWorker** | `hyb_process_cloud` | Processes non-sensitive data in the cloud (e.g.

### The Workflow

```
hyb_classify_data
 │
 ▼
SWITCH (hyb_switch_ref)
 ├── onprem: hyb_process_onprem
 ├── cloud: hyb_process_cloud

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
