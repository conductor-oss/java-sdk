# Fraud Detection in Java Using Conductor: Parallel Risk Signals, Deterministic Decision

A legitimate customer buys a $5,000 camera for a trip leaving in 2 hours. Your fraud system flags it: high amount, new merchant category, first purchase over $1,000. Card frozen. Customer calls support, furious, while their flight boards without them. Meanwhile, an actual fraudster running $80 test charges across stolen cards sails through because no single transaction crosses your threshold. The problem isn't the rules or the ML model, it's that rule checks, ML scoring, and velocity analysis run sequentially, so you either add latency to every transaction or skip checks to stay fast. This example uses [Conductor](https://github.com/conductor-oss/conductor) to run all risk signals in parallel and feed them into a deterministic decision function, you write the fraud logic, Conductor handles parallelism, retries, durability, and audit-ready observability.

## Fraud Detection Must Be Fast, Thorough, and Auditable

A $2,000 transaction arrives. Is it legitimate or fraudulent? The answer depends on multiple independent signals: Is the transaction velocity unusual (5 transactions in 10 minutes from the same card)? Does the amount exceed known risk thresholds? Does the ML model flag the feature pattern as suspicious? Each check is independent and can run simultaneously.

Running risk checks sequentially adds latency. unacceptable for real-time payment processing where decisions must complete in under 500ms. Running them in parallel gives you the speed of the slowest check, not the sum of all three. The final decision combines all risk signals into a score that determines whether to approve, flag for manual review, or decline. Every decision must be auditable, regulators and dispute teams need to see exactly which risk signals triggered a decline.

## The Solution

**You just write the transaction analysis, rule checks, ML scoring, velocity detection, and decisioning logic. Conductor handles parallel risk scoring, decision routing, and complete fraud analysis audit trails.**

`AnalyzeTransactionWorker` examines the transaction metadata. Amount, merchant, and customer, and extracts a feature vector (amount deviation from historical average, merchant category, distance from home). `FORK_JOIN` dispatches three parallel risk checks: rule evaluation (amount thresholds, new-merchant risk), ML scoring (weighted feature model), and velocity analysis (transaction frequency patterns). After `JOIN` collects all risk signals, `DecideWorker` combines them into a deterministic APPROVE / REVIEW / BLOCK decision. Conductor runs all three checks in parallel for sub-second decisions and records every risk signal for audit compliance.

### What You Write: Workers

Risk signal workers run in parallel. Rule checks, ML scoring, and velocity detection execute concurrently, feeding results into a single decisioning worker.

| Worker | Task | What It Does |
|---|---|---|
| `AnalyzeTransactionWorker` | `frd_analyze_transaction` | Builds a customer profile and extracts a feature vector (amount deviation, merchant category, distance, new-merchant flag) from the transaction inputs |
| `RuleCheckWorker` | `frd_rule_check` | Evaluates fraud rules against the transaction amount: amount > $1000 threshold, new-merchant high-value (> $500), and round-amount detection. Returns overall `ruleResult` (`low_risk` / `medium_risk` / `high_risk`) and which rules fired |
| `MlScoreWorker` | `frd_ml_score` | Scores the transaction using a weighted feature model: amount deviation (0.35), distance from home (0.25), new merchant (0.20), time of day (0.10), merchant category (0.10). Returns `fraudScore` in [0, 1], model version, and confidence |
| `VelocityCheckWorker` | `frd_velocity_check` | Checks transaction velocity patterns: rapid succession, unusual volume, and geographic anomaly flags. Returns overall `velocityResult` (`normal` / `elevated` / `suspicious`) and per-flag details |
| `DecideWorker` | `frd_decide` | Combines rule result, ML fraud score, and velocity result into a final APPROVE / REVIEW / BLOCK decision with risk score and reason |

### Decision Semantics

`DecideWorker` uses deterministic thresholds to make the final decision:

| Decision | Condition | Risk Score |
|---|---|---|
| **BLOCK** | `fraudScore > 0.8` OR `ruleResult == "high_risk"` OR `velocityResult == "suspicious"` | `max(fraudScore, 0.85)` |
| **REVIEW** | `fraudScore > 0.5` OR `ruleResult == "medium_risk"` | `max(fraudScore, 0.55)` |
| **APPROVE** | All other cases (score <= 0.5, rules pass, velocity normal) | `fraudScore` as-is |

Thresholds are evaluated top-down: BLOCK conditions are checked first, then REVIEW, then APPROVE (the default). The constants `BLOCK_SCORE_THRESHOLD` (0.8) and `REVIEW_SCORE_THRESHOLD` (0.5) are defined in `DecideWorker.java`.

### The Workflow

```
frd_analyze_transaction
 |
 v
FORK_JOIN
 |-- frd_rule_check
 |-- frd_ml_score
 +-- frd_velocity_check
 |
 v
JOIN (wait for all branches)
frd_decide

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
