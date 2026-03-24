# Risk Assessment

Risk assessment workflow with parallel market, credit, and operational risk analysis via FORK_JOIN.

**Input:** `portfolioId`, `assessmentDate` | **Timeout:** 60s

## Pipeline

```
rsk_collect_factors
    │
    ┌─────────────────┬─────────────────┬──────────────────────┐
    │ rsk_market_risk │ rsk_credit_risk │ rsk_operational_risk │
    └─────────────────┴─────────────────┴──────────────────────┘
rsk_aggregate
```

## Workers

**AggregateWorker** (`rsk_aggregate`): Aggregates market, credit, and operational risk scores into an overall assessment.

```java
int overall = (int) Math.round(market * 0.4 + credit * 0.35 + ops * 0.25);
String category = overall >= 60 ? "High" : overall >= 30 ? "Moderate" : "Low";
```

Reads `creditRisk`, `marketRisk`, `operationalRisk`. Outputs `overallRisk`, `riskCategory`, `var95`, `capitalAdequate`.

**CollectFactorsWorker** (`rsk_collect_factors`): Collects risk factors for the portfolio.

Reads `portfolioId`. Outputs `marketData`, `creditData`, `operationalData`.

**CreditRiskWorker** (`rsk_credit_risk`): Analyzes credit risk using default rates and concentration data.

```java
int riskScore = (int) Math.round(defaultRate * 20 + concentration / 2);
```

Reads `creditData`. Outputs `riskScore`, `expectedLoss`, `unexpectedLoss`, `methodology`.

**MarketRiskWorker** (`rsk_market_risk`): Analyzes market risk using volatility, beta, and correlation data.

```java
int riskScore = (int) Math.round((volatility / 30 + beta / 2) * 50);
```

Reads `marketData`. Outputs `riskScore`, `var95`, `stressTestLoss`, `methodology`.

**OperationalRiskWorker** (`rsk_operational_risk`): Analyzes operational risk from incident, control gap, and maturity data.

```java
int riskScore = (int) Math.round(incidents * 10 + controlGaps * 5 + (100 - processMaturity) / 5.0);
```

Reads `operationalData`. Outputs `riskScore`, `capitalCharge`, `methodology`.

## Tests

**7 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
