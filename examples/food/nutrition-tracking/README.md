# Nutrition Tracking in Java with Conductor

Tracks a user's nutritional intake: logging meals, looking up calories and macros, calculating daily totals against goals, and generating a nutrition report.

## The Problem

You need to track a user's nutritional intake for a meal. The workflow logs what foods were consumed, looks up the nutritional information (calories, macros, micronutrients) for each food item, calculates daily totals by combining the meal with previous meals that day, and generates a nutrition report. Tracking without accurate nutritional data gives users a false sense of their intake; not calculating daily totals means missing calorie or macro targets.

Without orchestration, you'd build a single tracking service that records food entries, queries a nutrition database, accumulates daily totals, and renders reports. manually handling foods not found in the database, retrying failed nutrition API lookups, and managing timezone-aware daily boundaries.

## The Solution

**You just write the meal logging, nutrient lookup, daily total calculation, and nutrition report generation logic. Conductor handles ingredient lookup retries, macro aggregation, and nutritional audit trails.**

Each nutrition concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (log meal, lookup nutrition, calculate daily totals, generate report), retrying if the nutrition database API is unavailable, tracking every meal entry, and resuming from the last step if the process crashes.

### What You Write: Workers

Ingredient logging, macro calculation, meal scoring, and report generation workers each contribute one layer of nutritional analysis.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateDailyWorker** | `nut_calculate_daily` | Calculates daily totals for calories, protein, carbs, and fat, and compares against the calorie goal |
| **LogMealWorker** | `nut_log_meal` | Logs the meal type and foods consumed for the user and assigns a meal ID |
| **LookupNutritionWorker** | `nut_lookup_nutrition` | Looks up nutritional data (calories, protein, carbs, fat, fiber) for the logged foods |
| **ReportWorker** | `nut_report` | Generates a daily nutrition report with calories consumed, goal, remaining, and on-track status |

### The Workflow

```
nut_log_meal
 │
 ▼
nut_lookup_nutrition
 │
 ▼
nut_calculate_daily
 │
 ▼
nut_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
