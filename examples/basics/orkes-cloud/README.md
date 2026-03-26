# Orkes Cloud

Orkes Cloud connection test workflow — single greet task

**Input:** `name` | **Timeout:** 60s

**Output:** `greeting`, `mode`

## Pipeline

```
cloud_greet
```

## Workers

**CloudGreetWorker** (`CloudGreetWorker`): Worker that greets a user with a cloud/local indicator.

```java
this.taskDefName = cloudMode ? "cloud_greet" : "local_greet";
String modeLabel = cloudMode ? "Cloud" : "Local";
```

Reads `name`. Outputs `greeting`, `mode`.

## Workflow Output

- `greeting`: `${cloud_greet_ref.output.greeting}`
- `mode`: `${cloud_greet_ref.output.mode}`

## Data Flow

**cloud_greet**: `name` = `${workflow.input.name}`

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
