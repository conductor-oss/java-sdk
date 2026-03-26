# Itinerary Planning

Itinerary planning: preferences, search, optimize, book, finalize.

**Input:** `travelerId`, `destination`, `days` | **Timeout:** 60s

## Pipeline

```
itp_preferences
    │
itp_search
    │
itp_optimize
    │
itp_book
    │
itp_finalize
```

## Workers

**BookWorker** (`itp_book`)

Outputs `bookingIds`, `totalCost`.

**FinalizeWorker** (`itp_finalize`)

Reads `travelerId`. Outputs `itineraryId`, `finalized`.

**OptimizeWorker** (`itp_optimize`)

Outputs `optimizedItinerary`.

**PreferencesWorker** (`itp_preferences`)

Reads `travelerId`. Outputs `preferences`.

**SearchWorker** (`itp_search`)

Reads `days`, `destination`. Outputs `options`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
