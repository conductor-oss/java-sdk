# Nutrition Tracking

Orchestrates nutrition tracking through a multi-stage Conductor workflow.

**Input:** `userId`, `mealType`, `foods` | **Timeout:** 60s

## Pipeline

```
nut_log_meal
    │
nut_lookup_nutrition
    │
nut_calculate_daily
    │
nut_report
```

## Workers

**CalculateDailyWorker** (`nut_calculate_daily`)

Outputs `daily`.

**LogMealWorker** (`nut_log_meal`)

Reads `mealType`, `userId`. Outputs `loggedFoods`, `mealId`.

**LookupNutritionWorker** (`nut_lookup_nutrition`)

Outputs `nutrition`.

**ReportWorker** (`nut_report`)

Reads `userId`. Outputs `report`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
