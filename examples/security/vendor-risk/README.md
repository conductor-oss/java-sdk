# Implementing Vendor Risk Assessment in Java with Conductor : Questionnaire, SOC2 Review, Risk Scoring, and Decision

## The Problem

Before giving a vendor access to your data, you need to assess their security posture. This requires collecting their security questionnaire responses, reviewing their compliance certifications (SOC2 Type II, ISO 27001), scoring the overall risk based on data access level and security maturity, and making a go/no-go decision. If the vendor handles PII, the bar is higher.

Without orchestration, vendor risk assessment lives in email threads. Someone sends the questionnaire, waits weeks for a response, forwards the SOC2 report to security, and the approval decision happens in a meeting with no audit trail. Three months later, nobody can tell why a vendor was approved.

## The Solution

**You just write the questionnaire collection and risk scoring logic. Conductor handles the assessment sequence, retries when vendor portals are slow to respond, and a complete audit trail of every risk score, SOC2 review, and onboarding decision.**

Each assessment step is an independent worker. questionnaire collection, SOC2 review, risk scoring, and decision. Conductor runs them in sequence: collect the questionnaire, review certifications, score the risk, then make the decision. Every assessment is tracked with responses, review findings, risk score, and decision rationale for audit.

### What You Write: Workers

The assessment pipeline chains CollectQuestionnaireWorker to gather vendor responses, AssessRiskWorker to calculate a risk score, ReviewSoc2Worker to verify compliance certifications, and MakeDecisionWorker to produce an approve/conditional/deny ruling.

| Worker | Task | What It Does |
|---|---|---|
| **AssessRiskWorker** | `vr_assess_risk` | Calculates a risk score (0-100) and identifies specific security gaps (e.g., data encryption) |
| **CollectQuestionnaireWorker** | `vr_collect_questionnaire` | Collects the completed security questionnaire from the vendor |
| **MakeDecisionWorker** | `vr_make_decision` | Makes an approve/deny/conditional decision based on risk score and SOC 2 review |
| **ReviewSoc2Worker** | `vr_review_soc2` | Reviews the vendor's SOC 2 Type II report for validity and qualifications |

the workflow logic stays the same.

### The Workflow

```
vr_collect_questionnaire
 │
 ▼
vr_assess_risk
 │
 ▼
vr_review_soc2
 │
 ▼
vr_make_decision

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
