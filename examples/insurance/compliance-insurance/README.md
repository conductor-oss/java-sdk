# Insurance Compliance in Java with Conductor : Audit, Assess, File Reports, Track, Certify

## Insurance Compliance Spans Multiple Regulatory Bodies

An insurance company must comply with state DOI regulations (rate filings, market conduct), NAIC requirements (financial reporting, RBC ratios), federal regulations (OFAC sanctions screening, anti-money laundering), and industry standards (data security, claims handling best practices). Each regulatory body has different reporting requirements, filing deadlines, and audit expectations.

Compliance gaps create regulatory risk. fines, license revocation, or consent orders. The compliance workflow audits current practices, identifies gaps against requirements, files required reports before deadlines, tracks remediation of any identified gaps, and certifies compliance for each regulatory body. Missing a filing deadline or failing to remediate a gap can trigger regulatory action.

## The Solution

**You just write the audit execution, gap assessment, regulatory filing, tracking, and certification logic. Conductor handles assessment retries, remediation routing, and regulatory audit trails.**

`AuditWorker` examines current practices against regulatory requirements. reviewing rate filings, claims handling procedures, financial ratios, and data security controls. `AssessWorker` identifies compliance gaps and risk areas with severity ratings and remediation priorities. `FileReportsWorker` prepares and submits required regulatory filings, financial statements, market conduct reports, and statutory filings. `TrackWorker` monitors remediation progress for identified gaps with deadlines and accountable owners. `CertifyWorker` generates compliance certifications confirming adherence to regulatory requirements. Conductor tracks the full compliance lifecycle for regulatory examination readiness.

### What You Write: Workers

Regulation mapping, control assessment, gap identification, and remediation tracking workers each address one phase of insurance regulatory compliance.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `cpi_audit` | Audits current practices against regulatory requirements. reviews rate filings, claims handling procedures, financial ratios, and data security controls for the compliance period, outputting detailed findings |
| **AssessWorker** | `cpi_assess` | Assesses compliance gaps. analyzes audit findings to identify issues (2 minor issues found), assigns severity ratings, and creates a remediation plan with priorities and remediation items |
| **FileReportsWorker** | `cpi_file_reports` | Files required regulatory reports. prepares and submits filings to the specified regulatory body using the assessment data, returns the filingId for tracking |
| **TrackWorker** | `cpi_track` | Tracks remediation progress. monitors resolution of identified compliance items with deadlines and accountable owners, confirms all items resolved |
| **CertifyWorker** | `cpi_certify` | Generates the compliance certification. confirms all remediation items are resolved and produces the certification with complianceStatus and certificationId for the regulatory body |

### The Workflow

```
cpi_audit
 │
 ▼
cpi_assess
 │
 ▼
cpi_file_reports
 │
 ▼
cpi_track
 │
 ▼
cpi_certify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
