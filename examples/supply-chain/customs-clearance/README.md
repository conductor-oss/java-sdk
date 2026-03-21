# Customs Clearance in Java with Conductor : Declaration, Document Validation, Duty Calculation, Clearance, and Cargo Release

## The Problem

You need to clear imported shipments through customs. A container of electronic components (HS code 8542, $45K value) and packaging materials (HS code 4819, $5K value) arriving from Shanghai must be declared to US Customs, documents validated (commercial invoice, packing list, bill of lading), duties calculated based on tariff schedules and trade agreements, clearance obtained from CBP, and cargo released from the port. Each step depends on the previous one. you cannot calculate duty without validated HS codes, and cargo cannot be released without clearance.

Without orchestration, customs brokers manage this process through email chains with freight forwarders and government portals. If the duty calculation fails because of an invalid HS code, the shipment sits at the port accruing demurrage charges ($500+/day) while someone manually fixes the declaration and resubmits. There is no unified timeline of when each step completed, making it impossible to identify bottlenecks or prove compliance to auditors.

## The Solution

**You just write the customs workers. Declaration filing, document validation, duty calculation, clearance submission, and cargo release. Conductor handles strict step ordering, automatic retries on API timeouts, and timestamped records for trade compliance audits.**

Each step of the customs clearance process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so declarations are filed before validation, validated documents feed duty calculation, duties must be confirmed before clearance is requested, and cargo is only released after clearance is granted. If the customs authority's API times out during the clearance request, Conductor retries automatically without re-filing the declaration. Every filing, validation result, duty amount, and clearance decision is recorded with timestamps for trade compliance audits.

### What You Write: Workers

Five workers move shipments through customs: DeclareWorker files the declaration, ValidateWorker checks HS codes and documents, CalculateDutyWorker computes tariffs, ClearWorker submits for clearance, and ReleaseWorker frees cargo for delivery.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateDutyWorker** | `cst_calculate_duty` | Calculates import duties and tariffs based on HS codes, goods value, and trade agreements. |
| **ClearWorker** | `cst_clear` | Submits the clearance request to customs authorities with declaration and duty payment. |
| **DeclareWorker** | `cst_declare` | Files the customs declaration with shipment details, origin, and goods classification. |
| **ReleaseWorker** | `cst_release` | Releases cargo from the port for domestic delivery after clearance is granted. |
| **ValidateWorker** | `cst_validate` | Validates HS codes and commercial documents (invoice, packing list, bill of lading). |

### The Workflow

```
cst_declare
 │
 ▼
cst_validate
 │
 ▼
cst_calculate_duty
 │
 ▼
cst_clear
 │
 ▼
cst_release

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
