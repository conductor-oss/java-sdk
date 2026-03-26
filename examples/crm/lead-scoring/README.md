# Lead Scoring

Orchestrates lead scoring through a multi-stage Conductor workflow.

**Input:** `leadId`, `email`, `company` | **Timeout:** 60s

## Pipeline

```
ls_collect_signals
    │
ls_score
    │
ls_classify
    │
ls_route
```

## Workers

**ClassifyWorker** (`ls_classify`): Classifies lead based on score. Real classification with tier assignment.

- `score >= 80` &rarr; `"hot"`
- `score >= 60` &rarr; `"warm"`
- `score >= 40` &rarr; `"cool"`

```java
if (score >= 80) { classification = "hot"; priority = "P1"; }
else if (score >= 60) { classification = "warm"; priority = "P2"; }
```

Reads `totalScore`. Outputs `classification`, `priority`.

**CollectSignalsWorker** (`ls_collect_signals`): Collects lead signals for scoring. Real signal extraction and normalization.

```java
boolean demoRequested = Boolean.TRUE.equals(demoRequestedObj);
```

Reads `companySize`, `demoRequested`, `emailOpens`, `industry`, `pageViews`. Outputs `signals`.

**RouteWorker** (`ls_route`): Routes lead to appropriate sales team. Real routing based on classification and industry.

```java
switch (classification) {
```

Reads `classification`, `priority`. Outputs `assignedTo`, `action`, `routed`.

**ScoreWorker** (`ls_score`): Computes lead score using weighted scoring algorithm.

```java
int engagementScore = Math.min(25, pageViews * 1 + emailOpens * 3);
```

Reads `signals`. Outputs `totalScore`, `companySizeScore`, `industryScore`, `engagementScore`, `behaviorScore`.

## Tests

**6 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
