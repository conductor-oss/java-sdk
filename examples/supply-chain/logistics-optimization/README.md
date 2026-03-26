# Logistics Optimization

Logistics optimization: analyze demand, optimize routes, schedule vehicles, and dispatch.

**Input:** `region`, `date`, `orders` | **Timeout:** 60s

## Pipeline

```
lo_analyze_demand
    │
lo_optimize_routes
    │
lo_schedule
    │
lo_dispatch
```

## Workers

**AnalyzeDemandWorker** (`lo_analyze_demand`)

```java
Map<String, Integer> demandMap = Map.of("north", 12, "south", 8, "east", 15, "west", 5);
```

Reads `orders`, `region`. Outputs `demandMap`, `orderCount`.

**DispatchWorker** (`lo_dispatch`)

```java
int count = schedule != null ? schedule.size() : 0;
```

Reads `schedule`. Outputs `dispatched`, `vehiclesDispatched`.

**OptimizeRoutesWorker** (`lo_optimize_routes`)

Outputs `routes`, `routeCount`.

**ScheduleWorker** (`lo_schedule`)

```java
.map(rt -> Map.<String, Object>of("route", rt.get("id"), "vehicle", "V-" + rt.get("id"), "departure", "06:00"))
```

Reads `date`, `routes`. Outputs `schedule`, `vehicleCount`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
