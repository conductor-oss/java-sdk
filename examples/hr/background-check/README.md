# Background Check in Java with Conductor : Consent, Parallel Criminal/Employment/Education Verification, and Report

## The Problem

You need to verify a candidate's background before their start date. The Fair Credit Reporting Act (FCRA) requires written consent before any checks can begin. Once consent is obtained, three independent verifications must run: a criminal record search across multiple jurisdictions (county, state, federal), an employment history verification confirming titles, dates, and employers from the candidate's resume, and an education verification confirming degrees and institutions. These three checks are independent. each queries different databases and services, so they should run in parallel to minimize the total turnaround time. Once all three complete, a consolidated report determines the overall result (clear, flagged, or adverse) for the hiring decision. Running checks sequentially when they could run in parallel wastes days of time-to-hire.

Without orchestration, you'd build a monolithic verification system that collects consent, then sequentially calls the criminal database, employment verification service, and education registrar, or manages parallel threads manually with CompletableFuture and custom join logic. If the criminal records database is temporarily unavailable, you'd need retry logic per check. If the system crashes after employment verification completes but before education finishes, you lose the completed results and must restart everything. FCRA requires a complete audit trail documenting when consent was obtained and what was checked.

## The Solution

**You just write the consent collection, criminal check, employment verification, education verification, and eligibility reporting logic. Conductor handles verification retries, parallel check coordination, and background audit trails.**

Each verification step is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of obtaining consent before any checks begin, running criminal, employment, and education checks simultaneously via FORK_JOIN, waiting for all three to complete before generating the report, retrying any individual check if its data source is temporarily unavailable (without re-running the others), and maintaining an FCRA-compliant audit trail of every verification step.

### What You Write: Workers

Identity verification, criminal records search, employment history check, and report compilation workers each investigate one dimension of a candidate's background.

| Worker | Task | What It Does |
|---|---|---|
| **ConsentWorker** | `bgc_consent` | Collects the candidate's FCRA-compliant written consent and disclosure acknowledgment before any checks can proceed |
| **CriminalWorker** | `bgc_criminal` | Searches criminal records across county, state, and federal jurisdictions, returning clear/flagged status and the number of jurisdictions checked |
| **EmploymentWorker** | `bgc_employment` | Verifies the candidate's employment history. job titles, dates of employment, and employers, against what they reported |
| **EducationWorker** | `bgc_education` | Verifies degrees, institutions, and graduation dates against the National Student Clearinghouse or institution registrars |
| **ReportWorker** | `bgc_report` | Compiles all three verification results into a consolidated background check report with an overall clear/flagged/adverse determination |

### The Workflow

```
bgc_consent
 │
 ▼
FORK_JOIN
 ├── bgc_criminal
 ├── bgc_employment
 └── bgc_education
 │
 ▼
JOIN (wait for all branches)
bgc_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
